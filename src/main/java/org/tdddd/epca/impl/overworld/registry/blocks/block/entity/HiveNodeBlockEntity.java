package org.tdddd.epca.impl.overworld.registry.blocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.blocks.block.HiveNode;

public class HiveNodeBlockEntity extends BlockEntity
        implements org.tdddd.epca.impl.overworld.data.BiomassStorage {

    public static final int MAX_BIOMASS_COUNT = 64;

    private static final String BIOMASS_COUNT_KEY = "biomassCount";

    private int biomassCount = 0;

    private boolean natural = false;

    private static final int BIOMASS_INTERVAL_TICKS = 45 * 20;

    private static final int CURBUG_INTERVAL_TICKS = 60 * 20;

    private static final int GROWTH_CHECK_TICKS = 20;

    private static final int GROWTH_MIN_TICKS = 15 * 20;
    private static final int GROWTH_MAX_TICKS = 30 * 20;
    private static final String GROWTH_KEY = "epca:hive_growth";

    private static final String GROWTH_ACCEL_KEY = "epca:hive_growth_accel";
    private static final double CAPTIVE_RADIUS = 8.0D;

    private static final int INFEST_RADIUS = 8;

    private static final int RIPPER_GROWTH_COST = 32;

    private static final int RIPPER_CHECK_TICKS = 20;
    private static final double RIPPER_RADIUS = 8.0D;

    private static final int STRUCTURE_LINK_DISTANCE = 24;
    private static final int STRUCTURE_MIN_NODES = 3;

    private static final int STRUCTURE_ESTABLISH_POINTS = 200;
    private static final int STRUCTURE_EXTRA_NODE_POINTS = 40;
    private static final int STRUCTURE_CHECK_TICKS = 100;

    private static final double FEED_ATTRACT_RADIUS = 32.0D;
    private static final double FEED_RADIUS = 4.0D;

    private static final double LEAVE_DISTANCE = 20.0D;
    private static final int FEED_CHECK_TICKS = 40;
    private static final String FED_KEY = "epca:hive_fed_at";

    private static final int LINK_CHECK_TICKS = 100;
    private static final int LINK_DURATION_TICKS = 200;
    private static final double LINK_RADIUS = 16.0D;

    private static final int INFEST_MIN_TICKS = 20 * 20;
    private static final int INFEST_MAX_TICKS = 30 * 20;

    private int biomassTimer = 0;
    private int spawnTimer = 0;
    private int growthTimer = 0;
    private int infestTimer = 0;
    private int infestTarget = 0;
    private int ripperTimer = 0;
    private int linkTimer = 0;
    private int feedTimer = 0;
    private int structureTimer = 0;

    private int structureScanTimer = 0;
    private static final int STRUCTURE_SCAN_EVERY = 10;
    private boolean inStructure = false;
    private int knownStructureSize = 0;

    private boolean structureRewarded = false;

    public HiveNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HIVE_NODE.get(), pos, state);
    }

    @Override
    public int getMaxBiomassCount() {
        return MAX_BIOMASS_COUNT;
    }

    @Override
    public int getBiomassCount() {
        return biomassCount;
    }

    @Override
    public void setBiomassCount(int value) {
        int clamped = Mth.clamp(value, 0, MAX_BIOMASS_COUNT);
        if (clamped == this.biomassCount) {
            return;
        }
        this.biomassCount = clamped;
        this.setChanged();
        updateStage();
    }

    public void addBiomassCount(int amount) {
        setBiomassCount(this.biomassCount + amount);
    }

    public boolean hasRoom() {
        return this.biomassCount < MAX_BIOMASS_COUNT;
    }

    public boolean isNatural() {
        return this.natural;
    }

    public void setNatural(boolean natural) {
        if (this.natural == natural) {
            return;
        }
        this.natural = natural;
        this.setChanged();
    }

    private void updateStage() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        BlockState state = this.getBlockState();
        int stage = HiveNode.stageFor(this.biomassCount);
        if (state.hasProperty(HiveNode.STAGE) && state.getValue(HiveNode.STAGE) != stage) {
            this.level.setBlock(this.worldPosition, state.setValue(HiveNode.STAGE, stage), 3);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.biomassCount = Mth.clamp(tag.getInt(BIOMASS_COUNT_KEY), 0, MAX_BIOMASS_COUNT);
        this.biomassTimer = tag.getInt("biomassTimer");
        this.infestTimer = tag.getInt("infestTimer");
        this.infestTarget = tag.getInt("infestTarget");
        this.ripperTimer = tag.getInt("ripperTimer");
        this.linkTimer = tag.getInt("linkTimer");
        this.feedTimer = tag.getInt("feedTimer");
        this.inStructure = tag.getBoolean("inStructure");
        this.structureRewarded = tag.getBoolean("structureRewarded");
        this.knownStructureSize = tag.getInt("knownStructureSize");
        this.spawnTimer = tag.getInt("spawnTimer");
        this.natural = tag.getBoolean("natural");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(BIOMASS_COUNT_KEY, this.biomassCount);
        tag.putInt("biomassTimer", this.biomassTimer);
        tag.putInt("infestTimer", this.infestTimer);
        tag.putInt("infestTarget", this.infestTarget);
        tag.putInt("ripperTimer", this.ripperTimer);
        tag.putInt("linkTimer", this.linkTimer);
        tag.putInt("feedTimer", this.feedTimer);
        tag.putBoolean("inStructure", this.inStructure);
        tag.putBoolean("structureRewarded", this.structureRewarded);
        tag.putInt("knownStructureSize", this.knownStructureSize);
        tag.putInt("spawnTimer", this.spawnTimer);
        tag.putBoolean("natural", this.natural);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HiveNodeBlockEntity be) {
        if (level.isClientSide) {
            return;
        }

        if (++be.biomassTimer >= BIOMASS_INTERVAL_TICKS) {
            be.biomassTimer = 0;
            if (be.hasRoom()) {
                be.addBiomassCount(1);
            }
        }

        if (++be.spawnTimer >= CURBUG_INTERVAL_TICKS) {
            be.spawnTimer = 0;
            if (be.biomassCount >= 1 && level instanceof net.minecraft.server.level.ServerLevel server) {
                if (be.spawnCurbug(server)) {
                    be.setBiomassCount(be.biomassCount - 1);
                }
            }
        }

        if (++be.ripperTimer >= RIPPER_CHECK_TICKS) {
            be.ripperTimer = 0;
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                be.absorbRipper(server);
            }
        }

        if (++be.linkTimer >= LINK_CHECK_TICKS) {
            be.linkTimer = 0;
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                be.applyLink(server);
            }
        }

        if (++be.feedTimer >= FEED_CHECK_TICKS) {
            be.feedTimer = 0;
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                be.attractFeeders(server);
            }
        }

        if (++be.structureTimer >= STRUCTURE_CHECK_TICKS) {
            be.structureTimer = 0;
            if (++be.structureScanTimer >= STRUCTURE_SCAN_EVERY) {
                be.structureScanTimer = 0;
                if (level instanceof net.minecraft.server.level.ServerLevel server) {
                    be.updateStructure(server);
                }
            }
        }

        if (++be.infestTimer >= (be.infestTarget <= 0 ? INFEST_MIN_TICKS : be.infestTarget)) {
            be.infestTimer = 0;
            be.infestTarget = INFEST_MIN_TICKS
                    + level.random.nextInt(INFEST_MAX_TICKS - INFEST_MIN_TICKS + 1);
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                be.infestNearby(server);
            }
        }

        if (++be.growthTimer >= GROWTH_CHECK_TICKS) {
            be.growthTimer = 0;
            be.advanceCaptiveGrowth();
        }
    }

    private boolean spawnCurbug(net.minecraft.server.level.ServerLevel level) {

        net.minecraft.core.BlockPos above = this.worldPosition.above();
        if (level.getBlockState(above).isCollisionShapeFullBlock(level, above)) {
            return false;
        }
        net.minecraft.world.entity.Mob mob = org.tdddd.epca.impl.overworld.registry.ModEntities.CURBUG.get().create(level);
        if (mob == null) {
            return false;
        }
        double x = this.worldPosition.getX() + 0.5D;
        double y = this.worldPosition.getY() + 1.0D;
        double z = this.worldPosition.getZ() + 0.5D;
        mob.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);

        mob.getPersistentData().putInt(GROWTH_KEY,
                GROWTH_MIN_TICKS + level.random.nextInt(GROWTH_MAX_TICKS - GROWTH_MIN_TICKS + 1));
        level.addFreshEntity(mob);
        spawnBiomassParticles(level);
        return true;
    }

    public boolean isInStructure() {
        return this.inStructure;
    }

    private void updateStructure(net.minecraft.server.level.ServerLevel level) {
        java.util.List<BlockPos> nodes = new java.util.ArrayList<>();
        int search = STRUCTURE_LINK_DISTANCE;
        for (BlockPos pos : BlockPos.betweenClosed(
                this.worldPosition.offset(-search, -search, -search),
                this.worldPosition.offset(search, search, search))) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(org.tdddd.epca.impl.overworld.registry.ModBlocks.HIVE_NODE.get())) {
                nodes.add(pos.immutable());
            }
        }

        java.util.Set<BlockPos> reached = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        reached.add(this.worldPosition);
        queue.add(this.worldPosition);
        while (!queue.isEmpty() && reached.size() < 128) {
            BlockPos current = queue.poll();
            for (BlockPos other : nodes) {
                if (reached.contains(other)) {
                    continue;
                }
                if (Math.abs(other.getX() - current.getX()) <= search
                        && Math.abs(other.getY() - current.getY()) <= search
                        && Math.abs(other.getZ() - current.getZ()) <= search
                        && current.distSqr(other) <= (double) (search * search)) {
                    reached.add(other);
                    queue.add(other);
                }
            }
        }
        boolean now = reached.size() >= STRUCTURE_MIN_NODES;

        if (now && !this.inStructure
                && org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeRules.hasHiveStructureWithinChunks(
                        level, this.worldPosition,
                        org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeRules.STRUCTURE_EXCLUSION_CHUNKS,
                        reached)) {
            now = false;
        }
        if (now != this.inStructure) {
            this.inStructure = now;
            this.setChanged();

            var manager = org.tdddd.epca.impl.overworld.data.EvolutionManager.forDimension(level);
            if (manager != null) {
                if (now) {
                    if (!this.structureRewarded) {
                        this.structureRewarded = true;
                        manager.addPoints(STRUCTURE_ESTABLISH_POINTS);
                    }
                    this.knownStructureSize = reached.size();
                } else {
                    this.knownStructureSize = 0;
                }
            }
        } else if (now && reached.size() > this.knownStructureSize) {
            int grown = reached.size() - this.knownStructureSize;
            this.knownStructureSize = reached.size();
            var manager = org.tdddd.epca.impl.overworld.data.EvolutionManager.forDimension(level);
            if (manager != null) {
                manager.addPoints(STRUCTURE_EXTRA_NODE_POINTS * grown);
            }
        }
    }

    private void attractFeeders(net.minecraft.server.level.ServerLevel level) {
        net.minecraft.world.phys.AABB area =
                new net.minecraft.world.phys.AABB(this.worldPosition).inflate(FEED_ATTRACT_RADIUS);
        for (net.minecraft.world.entity.Mob mob
                : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, area)) {
            if (!mob.isAlive()) {
                continue;
            }

            int ownBiomass = org.tdddd.epca.impl.overworld.data.EntityBiomassCountManager.getCurrentBiomassCount(mob);
            if (ownBiomass <= 0) {
                continue;
            }
            double distance = Math.sqrt(mob.blockPosition().distSqr(this.worldPosition));
            if (distance <= FEED_RADIUS) {

                if (mob.getPersistentData().getLong(FED_KEY) != 0L) {
                    continue;
                }
                mob.getPersistentData().putLong(FED_KEY, level.getGameTime());
                mob.getPersistentData().remove(GROWTH_KEY);

                if (ownBiomass > 0 && this.hasRoom()) {
                    org.tdddd.epca.impl.overworld.data.EntityBiomassCountManager.setBiomassCount(mob, ownBiomass - 1);
                    this.addBiomassCount(1);
                    spawnBiomassParticles(level);
                }

                double angle = level.random.nextDouble() * Math.PI * 2.0D;
                double tx = this.worldPosition.getX() + 0.5D + Math.cos(angle) * LEAVE_DISTANCE;
                double tz = this.worldPosition.getZ() + 0.5D + Math.sin(angle) * LEAVE_DISTANCE;
                mob.getNavigation().moveTo(tx, this.worldPosition.getY(), tz, 1.2D);
            } else if (distance <= FEED_ATTRACT_RADIUS) {

                mob.getNavigation().moveTo(this.worldPosition.getX() + 0.5D, this.worldPosition.getY(),
                        this.worldPosition.getZ() + 0.5D, 1.0D);
            }
        }
    }

    private void applyLink(net.minecraft.server.level.ServerLevel level) {
        net.minecraft.world.phys.AABB area =
                new net.minecraft.world.phys.AABB(this.worldPosition).inflate(LINK_RADIUS);
        for (net.minecraft.world.entity.LivingEntity living
                : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area)) {
            if (living instanceof net.minecraft.world.entity.player.Player) {
                continue;
            }

            boolean nodeBorn = living.getPersistentData().contains(GROWTH_KEY);
            boolean onesent = living.getType().is(org.tdddd.epca.impl.overworld.registry.ModTags.ONESENT);
            if (!nodeBorn && !onesent) {
                continue;
            }
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    org.tdddd.epca.impl.overworld.registry.ModEffects.LINK.get(),
                    LINK_DURATION_TICKS, 0, true, false));
        }
    }

    private void absorbRipper(net.minecraft.server.level.ServerLevel level) {
        if (this.biomassCount < RIPPER_GROWTH_COST) {
            return;
        }
        net.minecraft.world.phys.AABB area =
                new net.minecraft.world.phys.AABB(this.worldPosition).inflate(RIPPER_RADIUS);
        for (org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Ripper ripper
                : level.getEntitiesOfClass(org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Ripper.class, area)) {
            if (!ripper.isAlive()) {
                continue;
            }
            BlockPos target = ripper.blockPosition();
            if (!org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeRules
                    .canCreateNaturalNode(level, target)) {
                return;
            }
            this.setBiomassCount(this.biomassCount - RIPPER_GROWTH_COST);
            ripper.discard();
            level.setBlock(target, org.tdddd.epca.impl.overworld.registry.ModBlocks.HIVE_NODE.get().defaultBlockState(), 3);
            org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeRules.markNatural(level, target);
            spawnBiomassParticles(level);
            return;
        }
    }

    private void infestNearby(net.minecraft.server.level.ServerLevel level) {
        int stage = org.tdddd.epca.impl.overworld.data.EvolutionManager.getStageForDimension(level);
        org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager manager =
                org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager.getInstance();
        for (int attempt = 0; attempt < 12; attempt++) {
            double dx = (level.random.nextDouble() * 2.0D - 1.0D) * INFEST_RADIUS;
            double dy = (level.random.nextDouble() * 2.0D - 1.0D) * INFEST_RADIUS;
            double dz = (level.random.nextDouble() * 2.0D - 1.0D) * INFEST_RADIUS;
            if (dx * dx + dy * dy + dz * dz > (double) (INFEST_RADIUS * INFEST_RADIUS)) {
                continue;
            }
            BlockPos target = this.worldPosition.offset((int) dx, (int) dy, (int) dz);
            if (target.equals(this.worldPosition) || !level.isLoaded(target)) {
                continue;
            }
            BlockState state = level.getBlockState(target);
            if (state.isAir() || state.getDestroySpeed(level, target) < 0.0F) {
                continue;
            }

            if (!hasInfestedNeighbour(level, target)) {
                continue;
            }
            boolean changed = stage >= 2
                    ? manager.convertBlockUsingStageIIConfig(level, target, state)
                    : manager.convertBlockUsingStageIConfig(level, target, state);
            if (changed) {
                return;
            }
        }
    }

    private static boolean hasInfestedNeighbour(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            BlockState neighbour = level.getBlockState(pos.relative(direction));
            if (neighbour.getBlock() instanceof org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface) {
                return true;
            }
        }
        return false;
    }

    private void spawnBiomassParticles(net.minecraft.server.level.ServerLevel level) {
        int count = 2 + level.random.nextInt(2);
        level.sendParticles(org.tdddd.epca.impl.overworld.registry.ModParticles.BIOMASS.get(),
                this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D, this.worldPosition.getZ() + 0.5D,
                count, 0.25D, 0.25D, 0.25D, 0.0D);
    }

    private void advanceCaptiveGrowth() {
        if (!(this.level instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(this.worldPosition).inflate(CAPTIVE_RADIUS);
        long now = level.getGameTime();
        for (org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Curbug bug
                : level.getEntitiesOfClass(org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Curbug.class, area)) {
            CompoundTag data = bug.getPersistentData();
            if (!data.contains(GROWTH_KEY)) {
                continue;
            }

            if (now - data.getLong(GROWTH_ACCEL_KEY) < GROWTH_CHECK_TICKS) {
                continue;
            }
            data.putLong(GROWTH_ACCEL_KEY, now);
            int left = data.getInt(GROWTH_KEY) - GROWTH_CHECK_TICKS;
            if (left > 0) {
                data.putInt(GROWTH_KEY, left);
                continue;
            }

            double x = bug.getX();
            double y = bug.getY();
            double z = bug.getZ();
            float yaw = bug.getYRot();
            float pitch = bug.getXRot();
            SoundSource source = bug.getSoundSource();
            bug.discard();
            net.minecraft.world.entity.Mob ripper =
                    org.tdddd.epca.impl.overworld.registry.ModEntities.RIPPER.get().create(level);
            if (ripper != null) {
                ripper.moveTo(x, y, z, yaw, pitch);
                level.addFreshEntity(ripper);
            }

            level.playSound(null, x, y, z,
                    org.tdddd.epca.impl.overworld.registry.ModSoundEvents.CURBUG_EVOLVE.get(),
                    source, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.EXPLOSION, x, y + 1.0D, z, 3, 1.0D, 1.0D, 1.0D, 0.1D);
        }
    }
}
