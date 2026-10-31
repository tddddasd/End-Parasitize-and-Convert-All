package org.tdddd.epca.impl.overworld.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The infested raid.
 *
 * <p>Vanilla's {@code Raid}/{@code Raider} stack cannot be reused here: the infested pillager and
 * vindicator are plain {@code PathfinderMob}s, not vanilla {@code Raider}s, so this class reimplements the
 * visible parts of a raid (boss bar, waves, wave horn, difficulty scaling, victory) on its own terms.</p>
 *
 * <p>Triggered by the {@link ModEffects#INFESTED_OMEN} effect while the carrier stands inside a village
 * ({@code ServerLevel#isVillage}).</p>
 */
@EventBusSubscriber(modid = epca.MODID)
public final class InfestedRaidManager {
    /** Raid area radius, and the radius of the victory barrier. */
    public static final double AREA_RADIUS = InfestedRaidBarrierData.RADIUS;
    /** Range in which parasites get the 2.5x target range boost. */
    public static final double PARASITE_BOOST_RANGE = AREA_RADIUS;
    /** 16 blocks is the usual parasite acquisition range; the raid multiplies it by 2.5. */
    public static final double PARASITE_TARGET_RANGE = 16.0D * 2.5D;

    private static final int WAVE_DELAY_TICKS = 100;
    private static final double SPAWN_RING_MIN = 22.0D;
    private static final double SPAWN_RING_MAX = 40.0D;
    /** Vanilla raid mob count is scaled by 1.5 as the spec requires. */
    private static final double SIZE_MULTIPLIER = 1.5D;

    /**
     * Vanilla {@code Raid.RaiderType} tables, copied verbatim: index is the wave number, entry 0 is unused.
     * <pre>VINDICATOR(EntityType.VINDICATOR, new int[]{0, 0, 2, 0, 1, 4, 2, 5})
     * PILLAGER  (EntityType.PILLAGER,  new int[]{0, 4, 3, 3, 4, 4, 4, 2})</pre>
     */
    private static final int[] PILLAGER_PER_WAVE = {0, 4, 3, 3, 4, 4, 4, 2};
    private static final int[] VINDICATOR_PER_WAVE = {0, 0, 2, 0, 1, 4, 2, 5};

    /** Vanilla count -> infested-raid count (x1.5, rounded up so one pillager still means two). */
    private static int scaleToRaid(int vanillaCount) {
        return vanillaCount <= 0 ? 0 : (int) Math.ceil(vanillaCount * SIZE_MULTIPLIER);
    }

    /** Vanilla {@code Raid#getNumGroups}: 3 / 5 / 7 waves by difficulty, none on Peaceful. */
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

    // ────────── Tick ──────────

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
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

    /** Players carrying the Infested Omen who walk into a village start a raid on the spot. */
    private static void tickOmenTriggers(ServerLevel level) {
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (!player.hasEffect(ModEffects.INFESTED_OMEN)) continue;
            if (RAIDS.containsKey(level.dimension())) continue;
            BlockPos pos = player.blockPosition();
            if (!level.isVillage(pos)) continue;
            int omen = player.getEffect(ModEffects.INFESTED_OMEN).getAmplifier();
            player.removeEffect(ModEffects.INFESTED_OMEN);
            startRaid(level, pos, omen, player);
        }
    }

    /**
     * Parasites that are not part of the raid still get a 2.5x acquisition range while a raid is running
     * near them. They never join the waves or the raider count.
     */
    private static void tickParasiteBoost(ServerLevel level) {
        Raid raid = RAIDS.get(level.dimension());
        if (raid == null) return;
        AABB area = raid.area();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity instanceof ServerPlayer) continue;
            if (!(entity instanceof Mob mob)) continue;
            if (!IParasite.isParasiteByTagOrInterface(entity)) continue;
            if (raid.isRaider(entity.getUUID())) continue;
            if (mob.getTarget() != null && mob.getTarget().isAlive()) continue;
            LivingEntity target = findTarget(mob, PARASITE_TARGET_RANGE);
            if (target != null) {
                mob.setTarget(target);
                if (mob.getNavigation() != null) mob.getNavigation().moveTo(target, 1.0D);
            }
        }
    }

    // ────────── Raid lifecycle ──────────

    public static void startRaid(ServerLevel level, BlockPos center, int omenLevel, ServerPlayer trigger) {
        // Vanilla wave count: one group per difficulty tier (3 / 5 / 7), plus one bonus wave at omen level
        // II or higher. Peaceful never runs a raid at all.
        int groups = groupsForDifficulty(level.getDifficulty());
        if (groups <= 0) return;
        int totalWaves = groups + (omenLevel > 1 ? 1 : 0);
        Raid raid = new Raid(level, center, totalWaves, groups, Math.max(0, omenLevel));
        RAIDS.put(level.dimension(), raid);
        raid.bar.setVisible(true);
        raid.bar.addPlayer(trigger);
        trigger.sendSystemMessage(Component.translatable("epca.raid.started"));
        // The wave horn is played when each wave actually spawns (see spawnNextWave), like vanilla.
    }

    private static void playWaveHorn(ServerLevel level, BlockPos center) {
        level.playSound(null, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                SoundEvents.RAID_HORN, SoundSource.NEUTRAL, 1.0F, 0.62F);
    }

    /** @return the raid running in this level, or {@code null}. */
    public static Raid activeRaid(ServerLevel level) {
        return RAIDS.get(level.dimension());
    }

    /** Late joiners must see barriers that were created before they connected. */
    @SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        InfestedRaidBarrierData data = InfestedRaidBarrierData.get(level);
        data.prune();
        org.tdddd.epca.impl.network.ModNetwork.sendToPlayer(player,
                new org.tdddd.epca.impl.network.packet.s2c.RaidBarrierS2CPacket(data.all()));
    }

    // ────────── Targeting helpers ──────────

    /** Non-nest-leader players and non-parasite creatures. */
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
            // Nest leaders are parasites themselves: they are never a raid target.
            return !HiveLeaderManager.isHiveLeader(player.getUUID());
        }
        if (IParasite.isParasiteByTagOrInterface(candidate)) return false;
        return candidate.getType() != seeker.getType();
    }

    // ────────── Raid state ──────────

    public static final class Raid {
        private final ServerLevel level;
        private final BlockPos center;
        /** Total waves of this raid (vanilla groups + the optional bonus wave). */
        private final int totalWaves;
        /** Vanilla group count of the difficulty: the table index the bonus wave uses. */
        private final int numGroups;
        private final ServerBossEvent bar;
        private final List<UUID> raiders = new ArrayList<>();

        private int wave = 1;
        private int pendingSpawnTicks = 40;
        private boolean running = true;
        /** Size of the wave currently spawned: used for the bar progress. */
        private int waveTotal = 1;
        /** Omen level of the raid, used to decide whether the last wave is a bonus wave (vanilla rule). */
        private final int omenLevel;

        Raid(ServerLevel level, BlockPos center, int totalWaves, int numGroups, int omenLevel) {
            this.level = level;
            this.center = center;
            this.totalWaves = totalWaves;
            this.numGroups = numGroups;
            this.omenLevel = omenLevel;
            this.bar = new ServerBossEvent(UUID.randomUUID(),
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
                    // Current wave cleared: announce and queue the next one.
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
            for (ServerPlayer player : List.copyOf(this.level.players())) {
                if (player.position().distanceToSqr(Vec3.atCenterOf(this.center)) > AREA_RADIUS * AREA_RADIUS) {
                    this.bar.removePlayer(player);
                    continue;
                }
                this.bar.addPlayer(player);
            }
        }

        private void pruneDeadRaiders() {
            this.raiders.removeIf(id -> {
                var entity = this.level.getEntity(id);
                return entity == null || !entity.isAlive();
            });
        }

        private void spawnNextWave() {
            // Vanilla plays the raid horn as every wave spawns; ours is the same horn pitched down.
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
            if (!(type.create(this.level, EntitySpawnReason.EVENT) instanceof Mob raider)) return;
            BlockPos spot = findSpawnSpot();
            raider.snapTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D,
                    this.level.getRandom().nextFloat() * 360.0F, 0.0F);
            raider.setPersistenceRequired();
            if (this.level.addFreshEntity(raider)) {
                this.raiders.add(raider.getUUID());
            }
        }

        /** Index into the vanilla per-wave tables, honouring the bonus-wave rule. */
        private int waveIndex() {
            // Vanilla: the bonus wave (omen level > I) uses the numGroups row, normal waves use their own.
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

        /**
         * Vanilla {@code Raid#getPotentialBonusSpawns} for PILLAGER/VINDICATOR:
         * EASY and NORMAL roll 0..1, HARD rolls 0..2, PEACEFUL rolls nothing.
         */
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

        /** Raiders chase non-nest-leader players and non-parasite creatures, and walk towards them. */
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

            // The reward is the barrier itself: it purifies, shields and hands out the buffs.
            InfestedRaidBarrierData data = InfestedRaidBarrierData.get(this.level);
            data.add(InfestedRaidBarrierData.create(this.level.dimension().identifier(), this.center));
            org.tdddd.epca.impl.network.packet.s2c.RaidBarrierS2CPacket.sync(this.level, data);

            this.level.playSound(null, this.center.getX() + 0.5D, this.center.getY() + 0.5D, this.center.getZ() + 0.5D,
                    SoundEvents.RAID_HORN, SoundSource.NEUTRAL, 1.0F, 1.15F);
            // Victory is deliberately silent in chat: the barrier appearing is the feedback.
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
