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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = epca.MODID)
public final class ArayaSyncHandler {

    private static long serverTickCounter;

    private static final Map<UUID, List<ActiveFire>> ACTIVE_FIRES = new HashMap<>();

    private ArayaSyncHandler() {
    }

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

        if (ArayaTiansha.isOtherPlayer(killer, victim)) {
            ArayaTiansha.increment(staff);
        }

        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static void broadcastSlash(ServerLevel level, Player attacker, LivingEntity victim) {
        Vec3 from = attacker.position();
        Vec3 to = victim.position();
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        if (dx * dx + dz * dz < 1.0E-6D) {

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

        ACTIVE_FIRES.keySet().removeIf(uuid -> !holderIds.contains(uuid));
        if (holders.isEmpty()) {

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
