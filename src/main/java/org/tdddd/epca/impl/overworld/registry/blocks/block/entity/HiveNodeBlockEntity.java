package org.tdddd.epca.impl.overworld.registry.blocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.blocks.block.HiveNode;
import org.tdddd.epca.impl.overworld.registry.entities.IOnesent;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Curbug;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Ripper;

/**
 * Stores the hive node's biomass and keeps the block state's texture band in sync.
 *
 * <p>26.1.2 notes: block entities serialise through ValueInput/ValueOutput instead of CompoundTag, and
 * entities have no persistent data any more, so the "born from a node" marker is an entity tag.</p>
 */
public class HiveNodeBlockEntity extends BlockEntity 
        implements org.tdddd.epca.impl.overworld.data.BiomassStorage {

    public static final int MAX_BIOMASS_COUNT = 64;

    private static final int BIOMASS_INTERVAL_TICKS = 45 * 20;
    private static final int CURBUG_INTERVAL_TICKS = 60 * 20;
    private static final int GROWTH_CHECK_TICKS = 20;
    private static final int GROWTH_MIN_TICKS = 15 * 20;
    private static final int GROWTH_MAX_TICKS = 30 * 20;
    private static final String GROWTH_TAG_PREFIX = "epca:hive_growth:";
    private static final double CAPTIVE_RADIUS = 8.0D;
    private static final int INFEST_RADIUS = 8;
    private static final int INFEST_MIN_TICKS = 20 * 20;
    private static final int INFEST_MAX_TICKS = 30 * 20;
    private static final int RIPPER_GROWTH_COST = 32;
    private static final int RIPPER_CHECK_TICKS = 20;
    private static final double RIPPER_RADIUS = 8.0D;

    private int biomassCount = 0;
    private int biomassTimer = 0;
    private int spawnTimer = 0;
    private int growthTimer = 0;
    private int infestTimer = 0;
    private int infestTarget = 0;
    private static final int STRUCTURE_LINK_DISTANCE = 24;
    private static final int STRUCTURE_MIN_NODES = 3;
    private static final int STRUCTURE_ESTABLISH_POINTS = 200;
    private static final int STRUCTURE_EXTRA_NODE_POINTS = 40;
    private static final int STRUCTURE_CHECK_TICKS = 100;

    private static final int LINK_CHECK_TICKS = 100;
    private static final int LINK_DURATION_TICKS = 200;
    private static final double LINK_RADIUS = 16.0D;
    private static final double FEED_ATTRACT_RADIUS = 32.0D;
    private static final double FEED_RADIUS = 4.0D;
    private static final double LEAVE_DISTANCE = 20.0D;
    private static final int FEED_CHECK_TICKS = 40;

    private int ripperTimer = 0;
    private int linkTimer = 0;
    private int feedTimer = 0;
    private int structureTimer = 0;
    /**
     * The structure scan walks a 49x49x49 cube (117,649 block lookups), so it only runs every
     * {@link #STRUCTURE_SCAN_EVERY}th structure check. The flag it recomputes changes over minutes, so
     * the coarser cadence is not observable in game but removes about 90% of the scan cost.
     */
    private int structureScanTimer = 0;
    private static final int STRUCTURE_SCAN_EVERY = 10;
    private boolean inStructure = false;
    private int knownStructureSize = 0;
    /** True once this node has paid out the structure bonus, so it is only granted once. */
    private boolean structureRewarded = false;

    public HiveNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HIVE_NODE.get(), pos, state);
    }

    @Override
    public int getMaxBiomassCount() {
        return MAX_BIOMASS_COUNT;
    }

    public int getBiomassCount() {
        return biomassCount;
    }

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

    private void updateStage() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        BlockState state = this.getBlockState();
        int stage = HiveNode.stageFor(this.biomassCount);
        if (state.hasProperty(HiveNode.STAGE) && state.getValue(HiveNode.STAGE) != stage) {
            this.level.setBlock(this.worldPosition, state.setValue(HiveNode.STAGE, stage), 3);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.biomassCount = Mth.clamp(input.getIntOr("biomassCount", 0), 0, MAX_BIOMASS_COUNT);
        this.biomassTimer = input.getIntOr("biomassTimer", 0);
        this.spawnTimer = input.getIntOr("spawnTimer", 0);
        this.infestTarget = input.getIntOr("infestTarget", 0);
        this.ripperTimer = input.getIntOr("ripperTimer", 0);
        this.inStructure = input.getBooleanOr("inStructure", false);
        this.structureRewarded = input.getBooleanOr("structureRewarded", false);
        this.knownStructureSize = input.getIntOr("knownStructureSize", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("biomassCount", this.biomassCount);
        output.putInt("biomassTimer", this.biomassTimer);
        output.putInt("spawnTimer", this.spawnTimer);
        output.putInt("infestTarget", this.infestTarget);
        output.putInt("ripperTimer", this.ripperTimer);
        output.putBoolean("inStructure", this.inStructure);
        output.putBoolean("structureRewarded", this.structureRewarded);
        output.putInt("knownStructureSize", this.knownStructureSize);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HiveNodeBlockEntity be) {
        if (level.isClientSide()) {
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
            if (be.biomassCount >= 1 && level instanceof ServerLevel server) {
                if (be.spawnCurbug(server)) {
                    be.setBiomassCount(be.biomassCount - 1);
                }
            }
        }
        if (++be.ripperTimer >= RIPPER_CHECK_TICKS) {
            be.ripperTimer = 0;
            if (level instanceof ServerLevel server) {
                be.absorbRipper(server);
            }
        }
        if (++be.infestTimer >= (be.infestTarget <= 0 ? INFEST_MIN_TICKS : be.infestTarget)) {
            be.infestTimer = 0;
            be.infestTarget = INFEST_MIN_TICKS + level.getRandom().nextInt(INFEST_MAX_TICKS - INFEST_MIN_TICKS + 1);
            if (level instanceof ServerLevel server) {
                be.infestNearby(server);
            }
        }
        if (++be.structureTimer >= STRUCTURE_CHECK_TICKS) {
            be.structureTimer = 0;
            if (++be.structureScanTimer >= STRUCTURE_SCAN_EVERY) {
                be.structureScanTimer = 0;
                if (level instanceof ServerLevel server) {
                    be.updateStructure(server);
                }
            }
        }
        if (++be.linkTimer >= LINK_CHECK_TICKS) {
            be.linkTimer = 0;
            if (level instanceof ServerLevel server) {
                be.applyLink(server);
            }
        }
        if (++be.feedTimer >= FEED_CHECK_TICKS) {
            be.feedTimer = 0;
            if (level instanceof ServerLevel server) {
                be.attractFeeders(server);
            }
        }
        if (++be.growthTimer >= GROWTH_CHECK_TICKS) {
            be.growthTimer = 0;
            be.advanceCaptiveGrowth();
        }
    }

    private boolean spawnCurbug(ServerLevel level) {
        Mob mob = ModEntities.CURBUG.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (mob == null) {
            return false;
        }
        mob.absSnapTo(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D,
                this.worldPosition.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        int ticks = GROWTH_MIN_TICKS + level.getRandom().nextInt(GROWTH_MAX_TICKS - GROWTH_MIN_TICKS + 1);
        mob.addTag(GROWTH_TAG_PREFIX + ticks);
        level.addFreshEntity(mob);
        spawnBiomassParticles(level);
        return true;
    }

    /** True once this node is part of a hive structure (three or more linked nodes). */
    public boolean isInStructure() {
        return this.inStructure;
    }

    private void updateStructure(ServerLevel level) {
        java.util.List<BlockPos> nodes = new java.util.ArrayList<>();
        int search = STRUCTURE_LINK_DISTANCE;
        for (BlockPos pos : BlockPos.betweenClosed(
                this.worldPosition.offset(-search, -search, -search),
                this.worldPosition.offset(search, search, search))) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(ModBlocks.HIVE_NODE.get())) {
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
                if (current.distSqr(other) <= (double) (search * search)) {
                    reached.add(other);
                    queue.add(other);
                }
            }
        }
        boolean now = reached.size() >= STRUCTURE_MIN_NODES;
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

    /** Only node-born curbugs and nearby onesent parasites get Link. */
    private void applyLink(ServerLevel level) {
        AABB area = new AABB(this.worldPosition).inflate(LINK_RADIUS);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
            boolean nodeBorn = growthLeft(living) >= 0;
            boolean onesent = living instanceof IOnesent;
            if (!nodeBorn && !onesent) {
                continue;
            }
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    org.tdddd.epca.impl.overworld.registry.ModEffects.LINK,
                    LINK_DURATION_TICKS, 0, true, false));
        }
    }

    /** Draws parasites in from 32 blocks and lets them feed; fed ones walk away afterwards. */
    private void attractFeeders(ServerLevel level) {
        AABB area = new AABB(this.worldPosition).inflate(FEED_ATTRACT_RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (!mob.isAlive()) {
                continue;
            }
            // Only parasites that carry biomass of their own bother approaching a hive node.
            int ownBiomass = org.tdddd.epca.impl.overworld.data.EntityBiomassCountManager.getCurrentBiomassCount(mob);
            if (ownBiomass <= 0) {
                continue;
            }
            double distance = Math.sqrt(mob.blockPosition().distSqr(this.worldPosition));
            if (distance <= FEED_RADIUS) {
                if (growthLeft(mob) >= 0) {
                    continue;                       // node children are not feeders
                }
                // Feeding transfers the feeder's own biomass: a parasite with no kill count gives nothing.
                if (ownBiomass > 0 && this.hasRoom()) {
                    org.tdddd.epca.impl.overworld.data.EntityBiomassCountManager.setBiomassCount(mob, ownBiomass - 1);
                    this.addBiomassCount(1);
                    spawnBiomassParticles(level);
                }
                double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
                mob.getNavigation().moveTo(this.worldPosition.getX() + 0.5D + Math.cos(angle) * LEAVE_DISTANCE,
                        this.worldPosition.getY(),
                        this.worldPosition.getZ() + 0.5D + Math.sin(angle) * LEAVE_DISTANCE, 1.2D);
            } else {
                mob.getNavigation().moveTo(this.worldPosition.getX() + 0.5D, this.worldPosition.getY(),
                        this.worldPosition.getZ() + 0.5D, 1.0D);
            }
        }
    }

    private static int growthLeft(Entity entity) {
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(GROWTH_TAG_PREFIX)) {
                try {
                    return Integer.parseInt(tag.substring(GROWTH_TAG_PREFIX.length()));
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static void setGrowthLeft(Entity entity, int ticks) {
        for (String tag : new java.util.ArrayList<>(entity.entityTags())) {
            if (tag.startsWith(GROWTH_TAG_PREFIX)) {
                entity.removeTag(tag);
            }
        }
        if (ticks > 0) {
            entity.addTag(GROWTH_TAG_PREFIX + ticks);
        }
    }

    private void advanceCaptiveGrowth() {
        if (!(this.level instanceof ServerLevel level)) {
            return;
        }
        AABB area = new AABB(this.worldPosition).inflate(CAPTIVE_RADIUS);
        for (Curbug bug : level.getEntitiesOfClass(Curbug.class, area)) {
            int left = growthLeft(bug);
            if (left < 0) {
                continue;
            }
            left -= GROWTH_CHECK_TICKS;
            if (left > 0) {
                setGrowthLeft(bug, left);
                continue;
            }
            setGrowthLeft(bug, 0);
            double x = bug.getX(), y = bug.getY(), z = bug.getZ();
            float yaw = bug.getYRot(), pitch = bug.getXRot();
            bug.discard();
            Mob ripper = ModEntities.RIPPER.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (ripper != null) {
                ripper.absSnapTo(x, y, z, yaw, pitch);
                level.addFreshEntity(ripper);
            }
        }
    }

    private void absorbRipper(ServerLevel level) {
        if (this.biomassCount < RIPPER_GROWTH_COST) {
            return;
        }
        AABB area = new AABB(this.worldPosition).inflate(RIPPER_RADIUS);
        for (Ripper ripper : level.getEntitiesOfClass(Ripper.class, area)) {
            if (!ripper.isAlive()) {
                continue;
            }
            this.setBiomassCount(this.biomassCount - RIPPER_GROWTH_COST);
            BlockPos target = ripper.blockPosition();
            ripper.discard();
            level.setBlock(target, ModBlocks.HIVE_NODE.get().defaultBlockState(), 3);
            spawnBiomassParticles(level);
            return;
        }
    }

    private void infestNearby(ServerLevel level) {
        int stage = EvolutionManager.getStageForDimension(level);
        BlockConversionManager manager = BlockConversionManager.getInstance();
        for (int attempt = 0; attempt < 12; attempt++) {
            double dx = (level.getRandom().nextDouble() * 2.0D - 1.0D) * INFEST_RADIUS;
            double dy = (level.getRandom().nextDouble() * 2.0D - 1.0D) * INFEST_RADIUS;
            double dz = (level.getRandom().nextDouble() * 2.0D - 1.0D) * INFEST_RADIUS;
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

    private static boolean hasInfestedNeighbour(ServerLevel level, BlockPos pos) {
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).getBlock() instanceof InfestedBlockInterface) {
                return true;
            }
        }
        return false;
    }

    private void spawnBiomassParticles(ServerLevel level) {
        int count = 2 + level.getRandom().nextInt(2);
        level.sendParticles(ModParticles.BIOMASS.get(),
                this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D, this.worldPosition.getZ() + 0.5D,
                count, 0.25D, 0.25D, 0.25D, 0.0D);
    }
}
