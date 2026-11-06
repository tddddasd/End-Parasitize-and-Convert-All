package org.tdddd.epca.impl.overworld.raid;

import net.minecraft.core.BlockPos;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = epca.MODID)
public final class InfestedRaidManager {

    public static final double AREA_RADIUS = InfestedRaidBarrierData.RADIUS;

    public static final double PARASITE_BOOST_RANGE = AREA_RADIUS;

    public static final double PARASITE_TARGET_RANGE = 16.0D * 2.5D;

    private static final int WAVE_DELAY_TICKS = 100;
    private static final double SPAWN_RING_MIN = 22.0D;
    private static final double SPAWN_RING_MAX = 40.0D;

    private static final double SIZE_MULTIPLIER = 1.5D;

    private static final int[] PILLAGER_PER_WAVE = {0, 4, 3, 3, 4, 4, 4, 2};
    private static final int[] VINDICATOR_PER_WAVE = {0, 0, 2, 0, 1, 4, 2, 5};

    private static int scaleToRaid(int vanillaCount) {
        return vanillaCount <= 0 ? 0 : (int) Math.ceil(vanillaCount * SIZE_MULTIPLIER);
    }

    private static int groupsForDifficulty(net.minecraft.world.Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> 0;
            case EASY -> 3;
            case NORMAL -> 5;
            case HARD -> 7;
        };
    }

    private static final Map<ResourceKey<Level>, Raid> RAIDS = new HashMap<>();
    private static int tickCounter;

    private InfestedRaidManager() { }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        tickCounter++;

        boolean barrierTick = tickCounter % InfestedRaidBarrier.PULSE_TICKS == 0;
        boolean raidTick = tickCounter % 20 == 0;
        if (!barrierTick && !raidTick) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (barrierTick) InfestedRaidBarrier.pulse(level);
            if (raidTick) {
                Raid raid = RAIDS.get(level.dimension());
                if (raid != null) raid.tick();
                tickOmenTriggers(level);
                tickParasiteBoost(level);
            }
        }
    }

    private static void tickOmenTriggers(ServerLevel level) {
        for (ServerPlayer player : new ArrayList<>(level.players())) {
            if (!player.hasEffect(ModEffects.INFESTED_OMEN.get())) continue;
            if (RAIDS.containsKey(level.dimension())) continue;
            BlockPos pos = player.blockPosition();
            if (!level.isVillage(pos)) continue;
            int omen = player.getEffect(ModEffects.INFESTED_OMEN.get()).getAmplifier();
            player.removeEffect(ModEffects.INFESTED_OMEN.get());
            startRaid(level, pos, omen, player);
        }
    }

    private static void tickParasiteBoost(ServerLevel level) {
        Raid raid = RAIDS.get(level.dimension());
        if (raid == null) return;
        AABB area = raid.area();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity instanceof ServerPlayer) continue;
            if (!(entity instanceof Mob mob)) continue;
            if (!ParasiteEvents.isParasiteByTagOrInterface(entity)) continue;
            if (raid.isRaider(entity.getUUID())) continue;
            if (mob.getTarget() != null && mob.getTarget().isAlive()) continue;
            LivingEntity target = findTarget(mob, PARASITE_TARGET_RANGE);
            if (target != null) {
                mob.setTarget(target);
                if (mob.getNavigation() != null) mob.getNavigation().moveTo(target, 1.0D);
            }
        }
    }

    public static void startRaid(ServerLevel level, BlockPos center, int omenLevel, ServerPlayer trigger) {

        int groups = groupsForDifficulty(level.getDifficulty());
        if (groups <= 0) return;
        int totalWaves = groups + (omenLevel > 1 ? 1 : 0);
        Raid raid = new Raid(level, center, totalWaves, groups, Math.max(0, omenLevel));
        RAIDS.put(level.dimension(), raid);
        raid.bar.setVisible(true);
        raid.bar.addPlayer(trigger);
        trigger.sendSystemMessage(Component.translatable("epca.raid.started"));

    }

    private static void playWaveHorn(ServerLevel level, BlockPos center) {
        level.playSound(null, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                SoundEvents.RAID_HORN.value(), SoundSource.NEUTRAL, 1.0F, 0.62F);
    }

    public static Raid activeRaid(ServerLevel level) {
        return RAIDS.get(level.dimension());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        InfestedRaidBarrierData data = InfestedRaidBarrierData.get(level);
        data.prune();
        org.tdddd.epca.impl.network.packet.s2c.RaidBarrierS2CPacket.sendTo(player, data);
    }

    private static LivingEntity findTarget(Mob seeker, double range) {
        AABB box = seeker.getBoundingBox().inflate(range);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : seeker.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (!isValidRaidTarget(seeker, candidate)) continue;
            double d = seeker.distanceToSqr(candidate);
            if (d < bestDist) {
                bestDist = d;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean isValidRaidTarget(Mob seeker, LivingEntity candidate) {
        if (candidate == seeker || !candidate.isAlive()) return false;
        if (candidate instanceof ServerPlayer player) {
            if (player.isCreative() || player.isSpectator()) return false;

            return !HiveLeaderManager.isHiveLeader(player.getUUID());
        }
        if (ParasiteEvents.isParasiteByTagOrInterface(candidate)) return false;
        return candidate.getType() != seeker.getType();
    }

    public static final class Raid {
        private final ServerLevel level;
        private final BlockPos center;

        private final int totalWaves;

        private final int numGroups;
        private final ServerBossEvent bar;
        private final List<UUID> raiders = new ArrayList<>();

        private int wave = 1;
        private int pendingSpawnTicks = 40;
        private boolean running = true;

        private int waveTotal = 1;

        private final int omenLevel;

        Raid(ServerLevel level, BlockPos center, int totalWaves, int numGroups, int omenLevel) {
            this.level = level;
            this.center = center;
            this.totalWaves = totalWaves;
            this.numGroups = numGroups;
            this.omenLevel = omenLevel;
            this.bar = new ServerBossEvent(
                    Component.translatable("epca.raid.bar", 1, totalWaves),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            this.bar.setDarkenScreen(false);
        }

        AABB area() {
            return AABB.ofSize(Vec3.atCenterOf(this.center), AREA_RADIUS * 2.0D, AREA_RADIUS * 2.0D, AREA_RADIUS * 2.0D);
        }

        boolean isRaider(UUID id) {
            return this.raiders.contains(id);
        }

        void tick() {
            if (!this.running) return;
            refreshPlayers();
            pruneDeadRaiders();

            if (this.raiders.isEmpty()) {
                if (this.pendingSpawnTicks > 0) {
                    this.pendingSpawnTicks -= 20;
                    if (this.pendingSpawnTicks <= 0) spawnNextWave();
                } else if (this.wave >= this.totalWaves) {
                    victory();
                } else {

                    this.wave++;
                    this.pendingSpawnTicks = WAVE_DELAY_TICKS;
                    updateBar();
                }
            } else {
                assignTargets();
                updateBar();
            }
        }

        private void refreshPlayers() {
            for (ServerPlayer player : new ArrayList<>(this.level.players())) {
                if (player.position().distanceToSqr(Vec3.atCenterOf(this.center)) > AREA_RADIUS * AREA_RADIUS) {
                    this.bar.removePlayer(player);
                    continue;
                }
                this.bar.addPlayer(player);
            }
        }

        private void pruneDeadRaiders() {
            this.raiders.removeIf(id -> {
                net.minecraft.world.entity.Entity entity = this.level.getEntity(id);
                return entity == null || !entity.isAlive();
            });
        }

        private void spawnNextWave() {

            playWaveHorn(this.level, this.center);
            int pillagers = wavePillagers();
            int vindicators = waveVindicators();
            this.waveTotal = Math.max(1, pillagers + vindicators);
            for (int i = 0; i < pillagers; i++) {
                spawnRaider(ModEntities.INFESTED_PILLAGER.get());
            }
            for (int i = 0; i < vindicators; i++) {
                spawnRaider(ModEntities.INFESTED_VINDICATOR.get());
            }
            this.pendingSpawnTicks = 0;
            updateBar();
        }

        private void spawnRaider(EntityType<?> type) {
            if (!(type.create(this.level) instanceof Mob raider)) return;
            BlockPos spot = findSpawnSpot();
            raider.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D,
                    this.level.getRandom().nextFloat() * 360.0F, 0.0F);
            raider.setPersistenceRequired();
            if (this.level.addFreshEntity(raider)) {
                this.raiders.add(raider.getUUID());
            }
        }

        private int waveIndex() {

            boolean bonusWave = this.omenLevel > 1 && this.wave > this.numGroups;
            int index = bonusWave ? this.numGroups : this.wave;
            return Math.max(0, Math.min(index, PILLAGER_PER_WAVE.length - 1));
        }

        private int wavePillagers() {
            return scaleToRaid(PILLAGER_PER_WAVE[waveIndex()] + bonusRoll());
        }

        private int waveVindicators() {
            return scaleToRaid(VINDICATOR_PER_WAVE[waveIndex()] + bonusRoll());
        }

        private int bonusRoll() {
            int bonus = switch (this.level.getDifficulty()) {
                case PEACEFUL -> 0;
                case EASY -> 1;
                case NORMAL -> 1;
                case HARD -> 2;
            };
            return bonus > 0 ? this.level.getRandom().nextInt(bonus + 1) : 0;
        }

        private BlockPos findSpawnSpot() {
            for (int attempt = 0; attempt < 24; attempt++) {
                double angle = this.level.getRandom().nextDouble() * Math.PI * 2.0D;
                double dist = SPAWN_RING_MIN + this.level.getRandom().nextDouble() * (SPAWN_RING_MAX - SPAWN_RING_MIN);
                int x = this.center.getX() + (int) Math.round(Math.cos(angle) * dist);
                int z = this.center.getZ() + (int) Math.round(Math.sin(angle) * dist);
                BlockPos top = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        new BlockPos(x, this.center.getY(), z));
                if (this.level.getBlockState(top).isAir() && this.level.getBlockState(top.below()).blocksMotion()) {
                    return top;
                }
            }
            return this.center.above();
        }

        private void assignTargets() {
            for (UUID id : this.raiders) {
                if (!(this.level.getEntity(id) instanceof Mob mob)) continue;
                LivingEntity current = mob.getTarget();
                if (current != null && current.isAlive() && isValidRaidTarget(mob, current)) {
                    if (mob.getNavigation() != null && mob.getNavigation().isDone()) {
                        mob.getNavigation().moveTo(current, 1.0D);
                    }
                    continue;
                }
                LivingEntity target = findTarget(mob, AREA_RADIUS);
                if (target != null) {
                    mob.setTarget(target);
                    if (mob.getNavigation() != null) mob.getNavigation().moveTo(target, 1.0D);
                }
            }
        }

        private void updateBar() {
            int alive = this.raiders.size();
            this.bar.setProgress(Math.max(0.0F, Math.min(1.0F, alive / (float) Math.max(1, this.waveTotal))));
            this.bar.setName(Component.translatable("epca.raid.bar",
                    Math.min(this.wave, this.totalWaves), this.totalWaves));
        }

        private void victory() {
            this.running = false;
            this.bar.setVisible(false);
            this.bar.removeAllPlayers();
            RAIDS.remove(this.level.dimension());

            InfestedRaidBarrierData data = InfestedRaidBarrierData.get(this.level);
            data.add(InfestedRaidBarrierData.create(this.level.dimension().location(), this.center));
            org.tdddd.epca.impl.network.packet.s2c.RaidBarrierS2CPacket.sendAll(this.level, data);

            this.level.playSound(null, this.center.getX() + 0.5D, this.center.getY() + 0.5D, this.center.getZ() + 0.5D,
                    SoundEvents.RAID_HORN.value(), SoundSource.NEUTRAL, 1.0F, 1.15F);

        }

        public ServerLevel level() {
            return this.level;
        }

        public BlockPos center() {
            return this.center;
        }

        public int wave() {
            return this.wave;
        }

        public int totalWaves() {
            return this.totalWaves;
        }
    }
}
