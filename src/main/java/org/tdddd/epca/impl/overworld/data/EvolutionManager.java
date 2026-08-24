package org.tdddd.epca.impl.overworld.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.ModConfig;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncEvolutionStagePacket;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;

public class EvolutionManager {
    private static boolean isTwilightForestInstalled() {
        try {
            Class.forName("twilightforest.TFConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    
    public static final int[] STAGE_THRESHOLDS = {
            -100,      
            -50,       
            0,         
            400,       
            800,       
            1800,      
            20000,     
            200000,    
            5000000,   
            25000000,  
            500000000, 
            1000000000,
            1800000000 
    };

    private static final int COOLDOWN_TICKS = 20 * 60 * 20;

    private long cooldownEndTime;

    
    private static final SoundEvent[] STAGE_SOUNDS = {
            ModSoundEvents.PHASE0.get(),
            ModSoundEvents.PHASE1.get(),   
            ModSoundEvents.PHASE2.get(),   
            ModSoundEvents.PHASE3.get(),   
            ModSoundEvents.PHASE4.get(),   
            ModSoundEvents.PHASE5.get(),   
            ModSoundEvents.PHASE6.get(),   
            ModSoundEvents.PHASE7.get(),   
            ModSoundEvents.PHASE8.get(),   
            ModSoundEvents.PHASE9.get(),   
            ModSoundEvents.PHASE10.get()   
    };

    public static Component getStageDisplayName(int stage) {
        return Component.translatable("epca.stage." + stage);
    }

    private static final int MIN_EVOLUTION_POINTS = -100;
    private static final int MAX_EVOLUTION_POINTS = 2100000000;

    private int evolutionPoints;
    private int lastStage;
    private final ServerLevel level;
    private final EvolutionDataStorage dataStorage;

    
    public static EvolutionManager forOverworld(ServerLevel level) {
        return new EvolutionManager(level, 0);
    }

    public static EvolutionManager forNether(ServerLevel level) {
        return new EvolutionManager(level, -50);
    }

    public static EvolutionManager forEnd(ServerLevel level) {
        return new EvolutionManager(level, -50);
    }

    public static EvolutionManager forTwilightForest(ServerLevel level) {return new EvolutionManager(level, -50);}
    
    
    public void addPoints(int amount) {
        
        if (isInCooldown()) {
            return;
        }

        int oldStage = getStage();

        
        if (oldStage == -2) {
            return;
        }

        
        double multiplier = ModConfig.getPointsMultiplier(oldStage);
        
        multiplier *= DifficultyEffects.getEvolutionPointsMultiplier(level);
        int adjustedAmount = (int) Math.round(amount * multiplier);

        int newPoints = evolutionPoints + adjustedAmount;

        
        if (oldStage >= 0 && newPoints < 0) {
            newPoints = 0;
        }

        evolutionPoints = clampPoints(newPoints);
        dataStorage.setPointsForDimension(level.dimension(), evolutionPoints);
        checkForStageChange(oldStage);
    }

    
    public void setPoints(int points) {
        int oldStage = getStage();
        evolutionPoints = clampPoints(points);
        dataStorage.setPointsForDimension(level.dimension(), evolutionPoints);
        checkForStageChange(oldStage);

        
        int newStage = getStage();
        if (newStage >= 5 && newStage <= 13) {
            grantProgressToAllPlayers(newStage);
        }
    }

    
    private void grantSenseOfCrisisAdvancement(ServerPlayer player) {
        try {
            
            Advancement advancement = player.getServer().getAdvancements()
                    .getAdvancement(new ResourceLocation("epca", "sense_of_crisis"));

            if (advancement != null) {
                
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                if (!progress.isDone()) {
                    
                    player.getAdvancements().award(advancement, "unlock");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    private void grantProgressToAllPlayers(int stage) {
        if (stage < 5 || stage > 13) return; 

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            
            grantSenseOfCrisisAdvancement(player);
        }
    }

    
    public int getPoints() {
        return evolutionPoints;
    }

    
    private boolean isInCooldown() {
        long currentTime = level.getGameTime();
        return currentTime < cooldownEndTime;
    }

    
    private void startCooldown() {
        long currentTime = level.getGameTime();
        cooldownEndTime = currentTime + COOLDOWN_TICKS;
        
        dataStorage.setCooldownEndForDimension(level.dimension(), cooldownEndTime);
    }

    
    private void notifyStageChange(int newStage) {
        
        Component message = Component.translatable("epca.stage." + newStage);

        for (ServerPlayer player : level.players()) {
            if (player.level().dimension().equals(level.dimension())) {
                player.sendSystemMessage(message, false);
                
                if (newStage >= 0 && newStage <= 10) {
                    SoundEvent sound = getStageSound(newStage);
                    if (sound != null) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                sound, SoundSource.VOICE, 1.0f, 1.0f);
                    }
                }
            }
        }
    }

    
    private SoundEvent getStageSound(int stage) {
        
        if (stage >= 0 && stage <= 10) {
            return STAGE_SOUNDS[stage - 0];
        }
        return null; 
    }

    
    private int calculateStage() {
        double[] thresholds = ModConfig.getStageThresholds();
        for (int stage = thresholds.length - 1; stage >= 0; stage--) {
            if (evolutionPoints >= thresholds[stage]) {
                return stage - 2; 
            }
        }
        return -2;
    }

    
    private int clampPoints(int points) {
        return Math.min(Math.max(points, MIN_EVOLUTION_POINTS), MAX_EVOLUTION_POINTS);
    }

    
    private static boolean isTwilightForestDimension(ServerLevel level) {
        return isTwilightForestInstalled() &&
                level.dimension().location().toString().equals("twilightforest:twilight_forest");
    }

    
    public String getDimensionName() {
        if (level.dimension().equals(Level.OVERWORLD)) {
            return "主世界";
        } else if (level.dimension().equals(Level.NETHER)) {
            return "下界";
        } else if (level.dimension().equals(Level.END)) {
            return "末地";
        }else if (isTwilightForestDimension(level)) {
            return "暮色森林";
        }
        return level.dimension().location().toString();
    }

    public static EvolutionManager forDimension(ServerLevel level) {
        if (level.dimension().equals(Level.OVERWORLD)) {
            return forOverworld(level);
        } else if (level.dimension().equals(Level.NETHER)) {
            return forNether(level);
        } else if (level.dimension().equals(Level.END)) {
            return forEnd(level);
        } else if (isTwilightForestDimension(level)) {
            return forTwilightForest(level);
        } else {
            return forOverworld(level);
        }
    }

    public static int getStageForDimension(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0; 
        }

        if (serverLevel.dimension().equals(Level.OVERWORLD)) {
            return forOverworld(serverLevel).getStage();
        } else if (serverLevel.dimension().equals(Level.NETHER)) {
            return forNether(serverLevel).getStage();
        } else if (serverLevel.dimension().equals(Level.END)) {
            return forEnd(serverLevel).getStage();
        } else if (isTwilightForestDimension(serverLevel)) {
            return forTwilightForest(serverLevel).getStage();
        } else {
            return forOverworld(serverLevel).getStage();
        }
    }

    public static int getPointsForDimension(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0; 
        }

        if (serverLevel.dimension().equals(Level.OVERWORLD)) {
            return forOverworld(serverLevel).getPoints();
        } else if (serverLevel.dimension().equals(Level.NETHER)) {
            return forNether(serverLevel).getPoints();
        } else if (serverLevel.dimension().equals(Level.END)) {
            return forEnd(serverLevel).getPoints();
        } else if (isTwilightForestDimension(serverLevel)) {
            return forTwilightForest(serverLevel).getPoints();
        } else {
            return forOverworld(serverLevel).getPoints();
        }
    }

    
    private void broadcastCurrentStage() {
        ResourceLocation dimId = level.dimension().location();
        int stage = getStage();
        ModNetwork.sendToAll(new SyncEvolutionStagePacket(dimId, stage));
    }

    public void syncToPlayer(ServerPlayer player) {
        ResourceLocation dimId = level.dimension().location();
        int stage = getStage();
        ModNetwork.sendToPlayer(player, new SyncEvolutionStagePacket(dimId, stage));
    }

    public int getAttractionRadius() {
        if (!DifficultyEffects.isAttractionRangeEnabled(level)) return 0;
        int stage = getStage();
        switch (stage) {
            case 5: return 32;
            case 6: return 48;
            case 7: return 64;
            case 8: return 96;
            case 9: return 112;
            case 10: return 128;
            default: return 0; 
        }
    }

    private Integer overriddenStage = null;  

    
    public EvolutionManager(ServerLevel level, int initialPoints) {
        this.level = level;
        this.dataStorage = EvolutionDataStorage.get(level);
        this.evolutionPoints = dataStorage.getPointsForDimension(level.dimension());
        if (this.evolutionPoints == 0 && initialPoints != 0) {
            this.evolutionPoints = initialPoints;
            dataStorage.setPointsForDimension(level.dimension(), this.evolutionPoints);
        }
        
        this.overriddenStage = dataStorage.getOverriddenStage(level.dimension());
        this.lastStage = getStage();
        this.cooldownEndTime = dataStorage.getCooldownEndForDimension(level.dimension());
    }

    
    public int getStage() {
        if (overriddenStage != null) {
            return overriddenStage;
        }
        return calculateStage();
    }

    public void setOverriddenStage(int stage) {
        int oldStage = getStage();                      
        this.overriddenStage = stage;
        dataStorage.setOverriddenStage(level.dimension(), stage);
        checkForStageChange(oldStage);                  
    }

    public void clearOverriddenStage() {
        int oldStage = getStage();                      
        this.overriddenStage = null;
        dataStorage.clearOverriddenStage(level.dimension());
        checkForStageChange(oldStage);                  
    }

    
    private void checkForStageChange(int oldStage) {
        int newStage = getStage();
        if (newStage != oldStage) {
            if (newStage < 11) {
                startCooldown();
            }
            broadcastCurrentStage();
        }
        if (newStage > oldStage && newStage >= -1 && newStage <= 13) {
            notifyStageChange(newStage);
        }
        lastStage = newStage;
    }

    
    public double getParasiteDamageMultiplier() {
        int stage = getStage();
        switch (stage) {
            case 11: return 1.25;
            case 12: return 2.0;
            case 13: return 3.0;
            default: return 1.0;
        }
    }
}