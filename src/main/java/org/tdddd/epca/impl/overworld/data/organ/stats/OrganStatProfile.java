package org.tdddd.epca.impl.overworld.data.organ.stats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SPEC  0  D6 json
 *
 * <h2></h2>
 * <p> <b>9  + 1 </b> -2%
 *  {@link OrganStatSummary#STAT_ATTRIBUTE_KEYS}</p>
 *
 * <table border="1">
 *   <caption> -&gt;  / </caption>
 *   <tr><th>JSON </th><th></th><th>26.1.2 </th></tr>
 *   <tr><td>{@code health}</td><td></td>
 *       <td>{@code Attributes.MAX_HEALTH}{@code minecraft:max_health}</td></tr>
 *   <tr><td>{@code armor}</td><td></td>
 *       <td>{@code Attributes.ARMOR}{@code minecraft:armor}</td></tr>
 *   <tr><td>{@code armor_toughness}</td><td></td>
 *       <td>{@code Attributes.ARMOR_TOUGHNESS}{@code minecraft:armor_toughness}</td></tr>
 *   <tr><td>{@code attack_damage}</td><td></td>
 *       <td>{@code Attributes.ATTACK_DAMAGE}</td></tr>
 *   <tr><td>{@code knockback_resistance}</td><td></td>
 *       <td>{@code Attributes.KNOCKBACK_RESISTANCE}</td></tr>
 *   <tr><td>{@code movement_speed}</td><td></td>
 *       <td>{@code Attributes.MOVEMENT_SPEED}</td></tr>
 *   <tr><td>{@code swim_speed}</td><td></td>
 *       <td>{@code NeoForgeMod.SWIM_SPEED} 1.0 {@code 0.05}  +5%</td></tr>
 *   <tr><td>{@code attack_range}</td><td></td>
 *       <td><b>26.1.2 </b>{@code Attributes.ENTITY_INTERACTION_RANGE}
 *           {@code minecraft:entity_interaction_range} 3.0
 *           1.20.1  {@code forge:entity_reach}  26.1.2 </td></tr>
 *   <tr><td>{@code block_reach}</td><td></td>
 *       <td><b>26.1.2 </b>{@code Attributes.BLOCK_INTERACTION_RANGE}
 *           {@code minecraft:block_interaction_range} 4.5
 *           1.20.1  {@code forge:block_reach}  26.1.2 </td></tr>
 *   <tr><td>{@code entity_gravity}</td><td><b></b></td>
 *       <td><b>26.1.2 </b>{@code Attributes.GRAVITY}{@code minecraft:gravity} 0.08
 *           1.20.1  {@code forge:entity_gravity}  26.1.2
 *           <b>JSON  {@code entity_gravity} </b> 1.20.1
 *           <b></b>
 *           {@link OrganStatSummary#STAT_ATTRIBUTE_KEYS} </td></tr>
 *   <tr><td>{@code minimum_damage}</td><td></td><td> C9</td></tr>
 *   <tr><td>{@code adaptation_reduction}</td><td>01</td>
 *       <td> C9</td></tr>
 * </table>
 *
 * <h2> + </h2>
 * <p>{@link #ZERO}  0{@link #addTo}
 * {@link #plus(OrganStatProfile)}  +
 * </p>
 *
 * <p> Minecraft API 1.20.1
 * {@code NestLeaderOrganEffects} </p>
 */
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

    /**  0  /  */
    public static final OrganStatProfile ZERO = new OrganStatProfile(
            0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);

    /** {@link #toImmutable()}  */
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

        /** {@code null}  {@link #ZERO} */
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

        /**  {@code count}  n  */
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

    /** {@code null}  {@link #ZERO} */
    public OrganStatProfile plus(OrganStatProfile other) {
        if (other == null) return this;
        Accumulator accumulator = new Accumulator();
        accumulator.add(this);
        accumulator.add(other);
        return accumulator.toImmutable();
    }

    /**  {@code entityGravity} /  */
    public double totalAttributeBonus() {
        return health + armor + armorToughness + attackDamage + knockbackResistance
                + movementSpeed + swimSpeed + attackRange + blockReach;
    }

    /**  0 */
    public boolean isZero() {
        return health == 0.0D && armor == 0.0D && armorToughness == 0.0D
                && attackDamage == 0.0D && knockbackResistance == 0.0D
                && movementSpeed == 0.0D && swimSpeed == 0.0D
                && attackRange == 0.0D && blockReach == 0.0D
                && entityGravity == 0.0D
                && minimumDamage == 0.0D && adaptationReduction == 0.0D;
    }

    /**
     *  -&gt;  JSON  {@link OrganStatProfile}
     *
     * <p><b></b>
     * {@code null} </p>
     */
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

    /**
     *  0
     *
     * <p> {@link OrganStatDefaults#format(double)} 2 </p>
     */
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
        //  2  0
        //  health=0
        if (OrganStatDefaults.round2(value) == 0.0D) return;
        parts.put(key, OrganStatDefaults.format(value));
    }
}

