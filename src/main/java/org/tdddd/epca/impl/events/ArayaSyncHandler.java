package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SpawnArayaSlashPacket;
import org.tdddd.epca.impl.network.packet.s2c.SyncArayaAuraPacket;
import org.tdddd.epca.impl.network.packet.s2c.SyncArayaFirePacket;
import org.tdddd.epca.impl.overworld.registry.items.item.KillStick;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
 * render-only fire field around it is re-rolled every
 * {@link ArayaConstants#FIRE_REROLL_TICKS} ticks. Both batches are complete sets, so nothing has to be
 * cancelled explicitly: a holder that stops qualifying is simply absent from the next sweep, and the
 * client drops its aura, its BGM and its fire.</p>
 *
 * <p>Nothing here reads or writes a client class: it is a plain server tick subscriber, so it runs
 * unchanged on an integrated and on a dedicated server.</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID)
public final class ArayaSyncHandler {

    /** Server ticks seen by this handler; drives the sweep cadence and the fire re-roll period. */
    private static long serverTickCounter;

    private ArayaSyncHandler() {
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

        // The vanilla trident throw, at the victim, for everyone in range.
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS, 1.0F, 1.0F);

        broadcastSlash(level, killer, victim);
    }

    /**
     * Sends the slash to every player in the level.
     *
     * <p>The blade is centred on the victim and its direction is the horizontal direction the hit came
     * from, so the 50-degree cut always runs away from the attacker instead of in an arbitrary direction.
     * Only the horizontal part is sent, because the blade's own 50-degree rise is what the renderer
     * builds from it.</p>
     */
    private static void broadcastSlash(ServerLevel level, ServerPlayer killer, LivingEntity victim) {
        Vec3 from = killer.position();
        Vec3 to = victim.position();
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        if (dx * dx + dz * dz < 1.0E-6D) {
            // Attacker and victim on the same spot: fall back to the attacker's facing so the blade
            // still has a definite direction.
            Vec3 look = killer.getLookAngle();
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
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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
            if (reroll && !visible.isEmpty()) {
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

    /** One batch with the union of the fire fields of the holders this listener can see. */
    private static SyncArayaFirePacket buildFirePacket(ServerLevel level, List<ServerPlayer> holders) {
        List<Integer> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();
        List<Integer> zs = new ArrayList<>();
        for (ServerPlayer holder : holders) {
            List<BlockPos> field = rollFireField(level, holder);
            for (BlockPos pos : field) {
                if (xs.size() >= SyncArayaFirePacket.MAX_ENTRIES) {
                    break;
                }
                xs.add(Integer.valueOf(pos.getX()));
                ys.add(Integer.valueOf(pos.getY()));
                zs.add(Integer.valueOf(pos.getZ()));
            }
        }
        int size = xs.size();
        int[] xArray = new int[size];
        int[] yArray = new int[size];
        int[] zArray = new int[size];
        int[] lifetimes = new int[size];
        float[] heights = new float[size];
        for (int i = 0; i < size; i++) {
            int x = xs.get(i).intValue();
            int y = ys.get(i).intValue();
            int z = zs.get(i).intValue();
            xArray[i] = x;
            yArray[i] = y;
            zArray[i] = z;
            lifetimes[i] = ArayaConstants.fireLifetimeTicks(x, y, z);
            heights[i] = ArayaConstants.fireHeight(x, y, z);
        }
        return new SyncArayaFirePacket(level.getGameTime(), xArray, yArray, zArray, lifetimes, heights);
    }

    /**
     * Rolls one holder's fire field: up to {@link ArayaConstants#FIRE_COUNT} positions inside
     * {@link ArayaConstants#FIRE_RADIUS} blocks, keeping only those where a complete block has a free
     * position on top of it.
     *
     * <p>The roll is a pure function of the holder's entity id and the current roll period, so the field
     * is stable for the whole wave - it does not jump between two sweeps of the same period, and every
     * client is told exactly the same positions.</p>
     */
    private static List<BlockPos> rollFireField(ServerLevel level, ServerPlayer holder) {
        long period = serverTickCounter / Math.max(1L, ArayaConstants.FIRE_REROLL_TICKS);
        Random random = new Random(0x9E3779B97F4A7C15L * (holder.getId() + 1L)
                + period * 0xBF58476D1CE4E5B9L);
        List<BlockPos> field = new ArrayList<>();
        int attempts = ArayaConstants.FIRE_COUNT * 4;
        for (int i = 0; i < attempts && field.size() < ArayaConstants.FIRE_COUNT; i++) {
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
