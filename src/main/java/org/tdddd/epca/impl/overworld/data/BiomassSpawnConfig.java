package org.tdddd.epca.impl.overworld.data;

import java.util.List;

public class BiomassSpawnConfig {
    private List<SpawnEntry> water_spawns;
    private List<SpawnEntry> land_spawns;

    public List<SpawnEntry> getWaterSpawns() { return water_spawns; }
    public List<SpawnEntry> getLandSpawns() { return land_spawns; }

    public static class SpawnEntry {
        private String entity;               
        private int weight;                   
        private int min_count;                 
        private int max_count;                 
        private int life_time;                 
        private List<EffectEntry> effects;     

        public String getEntity() { return entity; }
        public int getWeight() { return weight; }
        public int getMinCount() { return min_count; }
        public int getMaxCount() { return max_count; }
        public int getLifeTime() { return life_time; }
        public List<EffectEntry> getEffects() { return effects; }
    }

    public static class EffectEntry {
        private String effect;                 
        private int duration;                   
        private int amplifier;                   
        private boolean ambient;                 
        private boolean visible;                  
        private boolean icon;                     

        public String getEffect() { return effect; }
        public int getDuration() { return duration; }
        public int getAmplifier() { return amplifier; }
        public boolean isAmbient() { return ambient; }
        public boolean isVisible() { return visible; }
        public boolean isIcon() { return icon; }
    }
}