package org.tdddd.epca.impl.overworld.data.organ.stats;

import java.util.LinkedHashMap;
import java.util.Map;

public record OrganStatProfile(
        double health,
        double armor,
        double armorToughness,
        double attackDamage,
        double knockbackResistance,
        double movementSpeed,
        double swimSpeed,
        double attackRange,
        double blockReach,
        double entityGravity,
        double minimumDamage,
        double adaptationReduction) {

    public static final OrganStatProfile ZERO = new OrganStatProfile(
            0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);

    public static final class Accumulator {
        private double health;
        private double armor;
        private double armorToughness;
        private double attackDamage;
        private double knockbackResistance;
        private double movementSpeed;
        private double swimSpeed;
        private double attackRange;
        private double blockReach;
        private double entityGravity;
        private double minimumDamage;
        private double adaptationReduction;

        public void add(OrganStatProfile profile) {
            if (profile == null) return;
            this.health += profile.health();
            this.armor += profile.armor();
            this.armorToughness += profile.armorToughness();
            this.attackDamage += profile.attackDamage();
            this.knockbackResistance += profile.knockbackResistance();
            this.movementSpeed += profile.movementSpeed();
            this.swimSpeed += profile.swimSpeed();
            this.attackRange += profile.attackRange();
            this.blockReach += profile.blockReach();
            this.entityGravity += profile.entityGravity();
            this.minimumDamage += profile.minimumDamage();
            this.adaptationReduction += profile.adaptationReduction();
        }

        public void addRepeated(OrganStatProfile profile, int count) {
            if (profile == null || count <= 0) return;
            for (int i = 0; i < count; i++) {
                add(profile);
            }
        }

        public double minimumDamage() {
            return minimumDamage;
        }

        public OrganStatProfile toImmutable() {
            return new OrganStatProfile(health, armor, armorToughness, attackDamage,
                    knockbackResistance, movementSpeed, swimSpeed, attackRange, blockReach,
                    entityGravity, minimumDamage, adaptationReduction);
        }
    }

    public OrganStatProfile plus(OrganStatProfile other) {
        if (other == null) return this;
        Accumulator accumulator = new Accumulator();
        accumulator.add(this);
        accumulator.add(other);
        return accumulator.toImmutable();
    }

    public double totalAttributeBonus() {
        return health + armor + armorToughness + attackDamage + knockbackResistance
                + movementSpeed + swimSpeed + attackRange + blockReach;
    }

    public boolean isZero() {
        return health == 0.0D && armor == 0.0D && armorToughness == 0.0D
                && attackDamage == 0.0D && knockbackResistance == 0.0D
                && movementSpeed == 0.0D && swimSpeed == 0.0D
                && attackRange == 0.0D && blockReach == 0.0D
                && entityGravity == 0.0D
                && minimumDamage == 0.0D && adaptationReduction == 0.0D;
    }

    public static OrganStatProfile fromJsonMap(Map<String, Double> values) {
        if (values == null || values.isEmpty()) return ZERO;
        return new OrganStatProfile(
                get(values, "health"),
                get(values, "armor"),
                get(values, "armor_toughness"),
                get(values, "attack_damage"),
                get(values, "knockback_resistance"),
                get(values, "movement_speed"),
                get(values, "swim_speed"),
                get(values, "attack_range"),
                get(values, "block_reach"),
                get(values, "entity_gravity"),
                get(values, "minimum_damage"),
                get(values, "adaptation_reduction"));
    }

    private static double get(Map<String, Double> values, String key) {
        Double value = values.get(key);
        return value == null ? 0.0D : value;
    }

    public String describeNonZero() {
        Map<String, String> parts = new LinkedHashMap<>();
        putIfNonZero(parts, "health", health);
        putIfNonZero(parts, "armor", armor);
        putIfNonZero(parts, "armor_toughness", armorToughness);
        putIfNonZero(parts, "attack_damage", attackDamage);
        putIfNonZero(parts, "knockback_resistance", knockbackResistance);
        putIfNonZero(parts, "movement_speed", movementSpeed);
        putIfNonZero(parts, "swim_speed", swimSpeed);
        putIfNonZero(parts, "attack_range", attackRange);
        putIfNonZero(parts, "block_reach", blockReach);
        putIfNonZero(parts, "entity_gravity", entityGravity);
        putIfNonZero(parts, "minimum_damage", minimumDamage);
        putIfNonZero(parts, "adaptation_reduction", adaptationReduction);
        return parts.toString();
    }

    private static void putIfNonZero(Map<String, String> parts, String key, double value) {

        if (OrganStatDefaults.round2(value) == 0.0D) return;
        parts.put(key, OrganStatDefaults.format(value));
    }
}
