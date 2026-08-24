package org.tdddd.epca.impl.overworld.registry.blocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.data.EntityKillCountManager;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.entity.link.StageIBeckon;
import org.tdddd.epca.impl.overworld.registry.entities.entity.link.StageIIBeckon;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;

import java.util.List;

public class BeckonCoreBlockEntity extends BlockEntity {
    private static final String KILL_COUNT_KEY = "killCount";
    private static final String GENERATION_TIMER_KEY = "generationTimer";
    private static final String IS_GENERATING_KEY = "isGenerating";

    private int killCount = 0;
    private int generationTimer = 0;
    private boolean isGenerating = false;

    private static final int REQUIRED_KILLS_STAGE_I = 30;
    private static final int REQUIRED_KILLS_STAGE_II = 55;
    private static final int GENERATION_DELAY_TICKS = 30 * 20; 
    private static final int ATTRACT_RADIUS = 48;
    private static final int ABSORB_RADIUS = 2;

    public BeckonCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BECKON_CORE.get(), pos, state);
    }

    
    public boolean isGenerating() {
        return isGenerating;
    }

    public int getKillCount() {
        return killCount;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.killCount = tag.getInt(KILL_COUNT_KEY);
        this.generationTimer = tag.getInt(GENERATION_TIMER_KEY);
        this.isGenerating = tag.getBoolean(IS_GENERATING_KEY);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(KILL_COUNT_KEY, this.killCount);
        tag.putInt(GENERATION_TIMER_KEY, this.generationTimer);
        tag.putBoolean(IS_GENERATING_KEY, this.isGenerating);
    }

    
    private void absorbKills(ServerLevel level) {
        AABB area = new AABB(this.worldPosition).inflate(ATTRACT_RADIUS);
        List<LivingEntity> parasites = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> IParasite.isParasiteByTagOrInterface(e) &&
                        EntityKillCountManager.getCurrentKillCount(e) > 0 &&
                        e.isAlive());

        for (LivingEntity parasite : parasites) {
            double dist = parasite.distanceToSqr(Vec3.atCenterOf(this.worldPosition));
            if (dist <= ABSORB_RADIUS * ABSORB_RADIUS) {
                int current = EntityKillCountManager.getCurrentKillCount(parasite);
                if (current > 0) {
                    EntityKillCountManager.setKillCount(parasite, current - 1);
                    this.killCount++;
                    this.setChanged();
                }
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BeckonCoreBlockEntity be) {
        if (level.isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) level;

        
        be.absorbKills(serverLevel);

        
        if (!be.isGenerating && be.killCount >= REQUIRED_KILLS_STAGE_I) {
            be.isGenerating = true;
            be.generationTimer = GENERATION_DELAY_TICKS;
            be.setChanged();
            
            return;
        }

        
        if (be.isGenerating) {
            
            EvolutionManager evo = EvolutionManager.forDimension(serverLevel);
            int stage = evo.getStage();
            if (stage < 3) {
                
                return;
            }
            
            be.generationTimer--;
            be.setChanged();
            if (be.generationTimer <= 0) {
                be.spawnBeckon(serverLevel);
            }
        }
    }

    
    private void spawnBeckon(ServerLevel level) {
        BlockPos above = this.worldPosition.above();
        if (!level.getBlockState(above).isAir()) return;

        int currentKills = this.killCount;
        boolean isStageII = currentKills > REQUIRED_KILLS_STAGE_II;
        int threshold = isStageII ? REQUIRED_KILLS_STAGE_II : REQUIRED_KILLS_STAGE_I;
        int remainingKills = Math.max(0, currentKills - threshold);

        LivingEntity beckon = null;

        if (isStageII) {
            StageIIBeckon stageII = ModEntities.STAGE_II_BECKON.get().create(level);
            if (stageII != null) {
                stageII.setRiseTarget(new Vec3(above.getX() + 0.5, above.getY(), above.getZ() + 0.5));
                level.addFreshEntity(stageII);
                beckon = stageII;
                
                for (ServerPlayer player : level.players()) {
                    if (player.level().dimension().equals(level.dimension())) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                ModSoundEvents.BECKON_STAGE2.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
                    }
                }
            }
        } else {
            StageIBeckon stageI = ModEntities.STAGE_I_BECKON.get().create(level);
            if (stageI != null) {
                stageI.setRiseTarget(new Vec3(above.getX() + 0.5, above.getY(), above.getZ() + 0.5));
                level.addFreshEntity(stageI);
                beckon = stageI;
                for (ServerPlayer player : level.players()) {
                    if (player.level().dimension().equals(level.dimension())) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                ModSoundEvents.BECKON_STAGE1.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
                    }
                }
            }
        }

        
        if (beckon != null) {
            EntityKillCountManager.setKillCount(beckon, remainingKills);
        }

        
        BlockConversionManager conversionManager = BlockConversionManager.getInstance();
        BlockPos center = this.worldPosition;

        
        final double RADIUS_HIGH = 4.5;   
        final double RADIUS_LOW = 5.5;    
        final double HIGH_SQR = RADIUS_HIGH * RADIUS_HIGH; 
        final double LOW_SQR = RADIUS_LOW * RADIUS_LOW;     
        final int MANHATTAN_FULL = 3;     
        final float CHANCE_HIGH = 0.7f;   
        final float CHANCE_LOW = 0.3f;    

        
        int maxRadius = (int) Math.ceil(RADIUS_LOW);
        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dy = -maxRadius; dy <= maxRadius; dy++) {
                for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                    
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    BlockPos targetPos = center.offset(dx, dy, dz);
                    BlockState targetState = level.getBlockState(targetPos);
                    if (targetState.isAir()) continue;

                    
                    int manhattanDist = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    double euclideanSqr = dx * dx + dy * dy + dz * dz;

                    boolean shouldConvert = false;

                    
                    if (manhattanDist <= MANHATTAN_FULL) {
                        shouldConvert = true;
                    }
                    
                    else if (euclideanSqr <= HIGH_SQR) {
                        if (level.random.nextFloat() < CHANCE_HIGH) {
                            shouldConvert = true;
                        }
                    }
                    
                    else if (euclideanSqr <= LOW_SQR) {
                        if (level.random.nextFloat() < CHANCE_LOW) {
                            shouldConvert = true;
                        }
                    }

                    if (shouldConvert) {
                        conversionManager.convertBlockUsingGeneralConfig(level, targetPos, targetState);
                    }
                }
            }
        }

        
        level.setBlock(this.worldPosition, ModBlocks.INFESTED_DIRT.get().defaultBlockState(), 3);
    }

    public void addKillCount(int amount) {
        this.killCount += amount;
        this.setChanged();
    }
}