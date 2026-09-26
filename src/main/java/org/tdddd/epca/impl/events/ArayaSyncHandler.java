package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SpawnArayaSlashPacket;
import org.tdddd.epca.impl.network.packet.s2c.SyncArayaAuraPacket;
import org.tdddd.epca.impl.network.packet.s2c.SyncArayaFirePacket;
import org.tdddd.epca.impl.overworld.registry.items.item.KillStick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Server half of the Alayavijnana staff feature: the kill path, the slash broadcast, the aura sweep and
 * the fire field.
 *
 * <h2>The kill path (the damage itself plus the counter)</h2>
 * <p>The 444-point hit is applied by {@code KillStick#onLeftClickEntity}, through the mod's own
 * minimum-damage mechanism ({@code yawning_neko_api:minimum}, exactly the way
 * {@code EnderBladeAttackHandler} builds a source for it). This handler owns what happens <i>after</i>
 * that hit: when the victim actually dies and the killer is a player holding the named staff, the kill is
 * counted on the staff stack ({@link ArayaTiansha}) if the victim was <b>another player</b>, the vanilla
 * trident throw sound plays at the victim's position, and the slash is broadcast to everyone who can see
 * it.</p>
 *
 * <h2>The aura sweep</h2>
 * <p>{@link ArayaConstants#AURA_SYNC_INTERVAL_TICKS} ticks apart, every level is swept for players whose
 * carried staff has an active counter. Each such holder is reported to the players around it, and the
 * render-only fire field around it is re-rolled every {@link ArayaConstants#FIRE_REROLL_TICKS} ticks.
 * Both batches are complete sets, so nothing has to be cancelled explicitly: a holder that stops
 * qualifying is simply absent from the next sweep, and the client drops its aura, its BGM and its
 * fire.</p>
 *
 * <p>Nothing here reads or writes a client class: it is a plain server tick subscriber, so it runs
 * unchanged on an integrated and on a dedicated server.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>{@code @Mod.EventBusSubscriber} plus {@code TickEvent.ServerTickEvent} with a phase become
 * {@code @EventBusSubscriber} plus {@code ServerTickEvent.Post}; the sweep logic is identical.</p>
 */
@EventBusSubscriber(modid = epca.MODID)
public final class ArayaSyncHandler {

    /** Server ticks seen by this handler; drives the sweep cadence and the fire re-roll period. */
    private static long serverTickCounter;

    /**
     * The fire blocks the server believes are alive, per holder.
     *
     * <p>The server owns the field: it rolls a wave of {@code FIRE_MIN_PER_WAVE..FIRE_MAX_PER_WAVE} blocks
     * around each holder on the roll period, keeps the previous waves until their lifetime runs out, caps
     * the total at {@code FIRE_MAX_ACTIVE} by dropping the oldest, and ships the resulting set - position,
     * age, lifetime and height - so every client draws exactly the same fire at exactly the same time.</p>
     */
    private static final Map<UUID, List<ActiveFire>> ACTIVE_FIRES = new HashMap<>();

    private ArayaSyncHandler() {
    }

    /** One render-only fire block that is currently burning around a holder. */
    private static final class ActiveFire {
        private final BlockPos position;
        private final long bornAtTick;
        private final int lifetimeTicks;
        private final float height;

        private ActiveFire(BlockPos position, long bornAtTick, int lifetimeTicks, float height) {
            this.position = position;
            this.bornAtTick = bornAtTick;
            this.lifetimeTicks = lifetimeTicks;
            this.height = height;
        }
    }

    // ===============================================================================================
    //  Kill path
    // ===============================================================================================

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        ItemStack staff = killer.getMainHandItem();
        if (!KillStick.isAlayavijnana(staff)) {
            return;
        }

        // Count only a kill of somebody else: a self-kill, a mob kill and a kill with anything but the
        // renamed staff all leave the counter alone.
        if (ArayaTiansha.isOtherPlayer(killer, victim)) {
            ArayaTiansha.increment(staff);
        }

        // The vanilla trident throw, at the victim, for everyone in range. The slash is not spawned
        // here any more: it belongs to the hit itself and is sent from the click path, so that a
        // renamed staff cuts on every left click and not only on a kill.
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * Sends the slash to every player in the level.
     *
     * <p>Called for every left click of the renamed staff on a living entity - mobs and players alike -
     * so the cut shows on ordinary hits as well as on kills. The blade is centred on the victim and its
     * direction is the horizontal direction the hit came from, so the 50-degree cut always runs away
     * from the attacker. Only the horizontal part is sent, because the blade's own 50-degree rise is
     * what the renderer builds from it.</p>
     */
    public static void broadcastSlash(ServerLevel level, Player attacker, LivingEntity victim) {
        Vec3 from = attacker.position();
        Vec3 to = victim.position();
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        if (dx * dx + dz * dz < 1.0E-6D) {
            // Attacker and victim on the same spot: fall back to the attacker's facing so the blade
            // still has a definite direction.
            Vec3 look = attacker.getLookAngle();
            dx = look.x;
            dz = look.z;
        }
        double length = Math.sqrt(dx * dx + dz * dz);
        SpawnArayaSlashPacket packet = new SpawnArayaSlashPacket(to.x, victim.getEyeY(), to.z,
                (float) (dx / length), (float) (dz / length), level.getGameTime());
        for (ServerPlayer player : level.players()) {
            ModNetwork.sendToPlayer(player, packet);
        }
    }

    // ===============================================================================================
    //  Aura sweep
    // ===============================================================================================

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        serverTickCounter++;
        if (serverTickCounter % ArayaConstants.AURA_SYNC_INTERVAL_TICKS != 0L) {
            return;
        }
        boolean reroll = serverTickCounter % ArayaConstants.FIRE_REROLL_TICKS == 0L;
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            sweepLevel(level, reroll);
        }
    }

    /** Reports the active holders of one level to the players around them, and re-rolls their fire. */
    private static void sweepLevel(ServerLevel level, boolean reroll) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        List<ServerPlayer> holders = new ArrayList<>();
        for (ServerPlayer player : players) {
            if (!ArayaTiansha.activeStaff(player).isEmpty()) {
                holders.add(player);
            }
        }
        // TEMP DIAGNOSTIC (remove once the effect is confirmed on screen): the aura only exists from
        // TIANSHA_THRESHOLD player kills on, so this line says whether that condition is met at all.
        if (reroll) {
            StringBuilder counters = new StringBuilder();
            for (ServerPlayer holder : holders) {
                counters.append(holder.getName().getString()).append('=')
                        .append(ArayaTiansha.get(ArayaTiansha.activeStaff(holder))).append(' ');
            }
            epca.LOGGER.info("[araya] sweep: {} holder(s) with the named staff; counters: {}",
                    holders.size(), counters.toString().trim());
        }
        // The fire field: age out what burned down, roll a new wave on the roll period, and hold the total
        // at FIRE_MAX_ACTIVE by dropping the oldest blocks of the holder whose waves overlap the most.
        long now = level.getGameTime();
        Set<UUID> holderIds = new HashSet<>();
        for (ServerPlayer holder : holders) {
            holderIds.add(holder.getUUID());
            List<ActiveFire> active = ACTIVE_FIRES.computeIfAbsent(holder.getUUID(),
                    key -> new ArrayList<>());
            active.removeIf(fire -> now - fire.bornAtTick >= fire.lifetimeTicks);
            if (reroll) {
                for (BlockPos pos : rollFireField(level, holder)) {
                    active.add(new ActiveFire(pos, now,
                            ArayaConstants.fireLifetimeTicks(pos.getX(), pos.getY(), pos.getZ()),
                            ArayaConstants.fireHeight(pos.getX(), pos.getY(), pos.getZ())));
                }
                while (active.size() > ArayaConstants.FIRE_MAX_ACTIVE) {
                    active.remove(0);
                }
            }
        }
        // A holder who lost the staff (or left) must not leave fire burning behind them.
        ACTIVE_FIRES.keySet().removeIf(uuid -> !holderIds.contains(uuid));
        if (holders.isEmpty()) {
            // One empty batch per player is what clears the client cache, so a level whose last holder
            // just lost the staff still stops the BGM and removes the fire.
            for (ServerPlayer listener : players) {
                ModNetwork.sendToPlayer(listener, new SyncArayaAuraPacket(new int[0], new double[0]));
            }
            return;
        }

        double reportRadiusSqr = ArayaConstants.AURA_REPORT_RADIUS * ArayaConstants.AURA_REPORT_RADIUS;
        for (ServerPlayer listener : players) {
            List<ServerPlayer> visible = new ArrayList<>();
            for (ServerPlayer holder : holders) {
                if (listener != holder && listener.distanceToSqr(holder) > reportRadiusSqr) {
                    continue;
                }
                visible.add(holder);
                if (visible.size() >= SyncArayaAuraPacket.MAX_ENTRIES) {
                    break;
                }
            }
            ModNetwork.sendToPlayer(listener, buildAuraPacket(visible));
            // The whole fire set is resent on every sweep, not only on a roll: the entries carry their own
            // age, so a listener that just came into range sees the fires already burning instead of an
            // empty field for up to one roll period.
            if (!visible.isEmpty()) {
                ModNetwork.sendToPlayer(listener, buildFirePacket(level, visible));
            }
        }
    }

    private static SyncArayaAuraPacket buildAuraPacket(List<ServerPlayer> holders) {
        int size = holders.size();
        int[] entityIds = new int[size];
        double[] positions = new double[size * 3];
        for (int i = 0; i < size; i++) {
            ServerPlayer holder = holders.get(i);
            entityIds[i] = holder.getId();
            positions[i * 3] = holder.getX();
            positions[i * 3 + 1] = holder.getY();
            positions[i * 3 + 2] = holder.getZ();
        }
        return new SyncArayaAuraPacket(entityIds, positions);
    }

    /** One batch with the union of the live fire blocks of the holders this listener can see. */
    private static SyncArayaFirePacket buildFirePacket(ServerLevel level, List<ServerPlayer> holders) {
        long now = level.getGameTime();
        List<ActiveFire> pooled = new ArrayList<>();
        for (ServerPlayer holder : holders) {
            List<ActiveFire> active = ACTIVE_FIRES.get(holder.getUUID());
            if (active == null) {
                continue;
            }
            for (ActiveFire fire : active) {
                if (pooled.size() >= SyncArayaFirePacket.MAX_ENTRIES) {
                    break;
                }
                pooled.add(fire);
            }
        }
        int size = pooled.size();
        long[] bornAgoTicks = new long[size];
        int[] xArray = new int[size];
        int[] yArray = new int[size];
        int[] zArray = new int[size];
        int[] lifetimes = new int[size];
        float[] heights = new float[size];
        for (int i = 0; i < size; i++) {
            ActiveFire fire = pooled.get(i);
            xArray[i] = fire.position.getX();
            yArray[i] = fire.position.getY();
            zArray[i] = fire.position.getZ();
            bornAgoTicks[i] = Math.max(0L, now - fire.bornAtTick);
            lifetimes[i] = fire.lifetimeTicks;
            heights[i] = fire.height;
        }
        return new SyncArayaFirePacket(now, bornAgoTicks, xArray, yArray, zArray, lifetimes, heights);
    }

    /**
     * Rolls one holder's new fire wave: {@link ArayaConstants#FIRE_MIN_PER_WAVE} to
     * {@link ArayaConstants#FIRE_MAX_PER_WAVE} positions inside {@link ArayaConstants#FIRE_RADIUS} blocks,
     * keeping only those where a complete block has a free position on top of it.
     *
     * <p>The roll is a pure function of the holder's entity id and the current roll period, so the wave is
     * stable for the whole roll and every client is told exactly the same positions.</p>
     */
    private static List<BlockPos> rollFireField(ServerLevel level, ServerPlayer holder) {
        long period = serverTickCounter / Math.max(1L, ArayaConstants.FIRE_REROLL_TICKS);
        Random random = new Random(0x9E3779B97F4A7C15L * (holder.getId() + 1L)
                + period * 0xBF58476D1CE4E5B9L);
        int span = ArayaConstants.FIRE_MAX_PER_WAVE - ArayaConstants.FIRE_MIN_PER_WAVE + 1;
        int wanted = ArayaConstants.FIRE_MIN_PER_WAVE + (span > 0 ? random.nextInt(span) : 0);
        List<BlockPos> field = new ArrayList<>();
        int attempts = wanted * 4;
        for (int i = 0; i < attempts && field.size() < wanted; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(random.nextDouble()) * ArayaConstants.FIRE_RADIUS;
            int x = Mth.floor(holder.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(holder.getZ() + Math.sin(angle) * distance);
            int y = Mth.floor(holder.getY());
            BlockPos surface = findFireSurface(level, x, y, z);
            if (surface != null) {
                field.add(surface);
            }
        }
        return field;
    }

    /**
     * The position a fire may be drawn at in one column, or {@code null}.
     *
     * <p>"完整方块" is checked with the block's own collision shape ({@code isCollisionShapeFullBlock})
     * rather than with a block tag, so it means the same thing for modded blocks; the position above the
     * floor also has to be replaceable and dry, so the fire never sticks out of a wall or floats on
     * water.</p>
     */
    private static BlockPos findFireSurface(ServerLevel level, int x, int aroundY, int z) {
        for (int offset = 1; offset >= -2; offset--) {
            BlockPos floor = new BlockPos(x, aroundY + offset, z);
            BlockPos above = floor.above();
            if (!level.isLoaded(floor) || !level.isLoaded(above)) {
                continue;
            }
            BlockState floorState = level.getBlockState(floor);
            if (floorState.isAir() || !floorState.getFluidState().isEmpty()
                    || !floorState.isCollisionShapeFullBlock(level, floor)) {
                continue;
            }
            BlockState aboveState = level.getBlockState(above);
            if (!aboveState.canBeReplaced()) {
                continue;
            }
            if (aboveState.getFluidState().getType() == Fluids.WATER
                    || aboveState.getFluidState().getType() == Fluids.FLOWING_WATER) {
                continue;
            }
            return above;
        }
        return null;
    }
}
