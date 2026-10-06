package org.tdddd.epca.impl.overworld.data.organ;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatProfile;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * STAGE 3 {@link OrganStatSummary} <b></b>
 * SPEC  2  3 +
 *
 * <h2> tick </h2>
 * <p>{@code epca#onPlayerTick}{@code PlayerTickEvent.Post}
 *  {@link #tick(Player)} {@link #notLeader(Player)}
 *  +
 *  tick  {@link #APPLIED}  return
 *  tick  {@link OrganStatSummary#compute} </p>
 *
 * <h2></h2>
 * <p>{@link #Slots#ALL}  {@link OrganStatSummary#STAT_ATTRIBUTE_KEYS} <b></b>
 * STAGE A  {@code armor_toughness}  {@code swim_speed}<b></b>
 * {@code entity_gravity} -2%
 *  {@link AttributeModifier.Operation#ADD_VALUE}<b> id</b>
 *  id  tick  id
 * </p>
 *
 * <h2>SPEC D6</h2>
 * <p>{@link #UNIQUE_EFFECTS}
 * </p>
 * <ol>
 *   <li><b></b>
 *        10  I  6 <b></b>SPEC
 *        1  10  10  0
 *        10 </li>
 *   <li><b></b>STAGE B +0.1 +2 </li>
 *   <li><b></b>STAGE B +0.15 +3 </li>
 *   <li><b></b>STAGE B +0.15 +2 </li>
 * </ol>
 * <p> +8%  JSON
 * {@code slot_bonuses}  {@code head_inner} </p>
 *
 * <h2>STAGE B</h2>
 * <p> 24
 * {@link FixedAttributeEffect} {@link OrganStatSummary#countOf(Identifier)}
 * &gt; 0 <b></b> tick
 * {@link #attributeSlots()}  {@link AttributeSlot} + {@link #applyAttribute(Player, String, double)}</p>
 * <p><b></b>
 * {@code "knockback_resistance_unique.< id>"} / {@code "armor_unique.< id>"}
 *  {@link #knockbackUniqueKey(String)}  {@link #armorUniqueKey(String)}
 *  {@link #applyAttribute}
 *  id +
 *  id </p>
 * <ul>
 *   <li> 1  10
 *       <b></b></li>
 *   <li> tick {@code APPLIED}  return
 *        tick </li>
 *   <li> 0{@link #applyAttribute}
 *        0 </li>
 *   <li>{@link #clear(Player)}  id  +  {@link AttributeSlot#NAME_PREFIX}
 *       //</li>
 *   <li><b></b>
 *        0.1 + 0.15 + 0.15 = 0.4 2 + 3 + 2 = 7
 *       SPEC
 *       " +2 "</li>
 * </ul>
 * <p><b></b>SPEC
 *  JSON </p>
 *
 * <h2>1.20.1 -&gt; 26.1.2  API </h2>
 * <ul>
 *   <li>{@code AttributeModifier}  26.1.2 <b></b>{@code new AttributeModifier(Identifier id,
 *       double amount, Operation operation)}1.20.1  {@code UUID + String name}
 *        {@link Identifier}{@code epca:organ_<>}
 *       {@code UUID.nameUUIDFromBytes(...)} = epca
 *       {@value AttributeSlot#NAME_PREFIX} </li>
 *   <li>{@code AttributeModifier.Operation.ADDITION} -&gt; {@code Operation.ADD_VALUE}
 *        _tmp_26src AttributeModifier.java  39 </li>
 *   <li>{@code AttributeInstance#removeModifier(UUID)} / {@code getModifier(UUID)} -&gt;
 *        {@link Identifier}AttributeInstance.java  64 / 120
 *       {@code getModifiers()}  {@code Set<AttributeModifier>}</li>
 *   <li>{@code ForgeMod.SWIM_SPEED.get()} -&gt; {@code NeoForgeMod.SWIM_SPEED}
 *       {@code Holder<Attribute>}{@code ForgeMod.ENTITY_REACH} -&gt;
 *       {@code Attributes.ENTITY_INTERACTION_RANGE}{@code ForgeMod.BLOCK_REACH} -&gt;
 *       {@code Attributes.BLOCK_INTERACTION_RANGE}{@code ForgeMod.ENTITY_GRAVITY} -&gt;
 *       {@code Attributes.GRAVITY}JSON  {@code entity_gravity}</li>
 *   <li>{@code Player#getPersistentData()}  Optional
 *       {@code getCompound(K)} -&gt; {@code getCompoundOrEmpty(K)}
 *       {@code getInt(K)} -&gt; {@code getIntOr(K, )}CompoundTag.java  327 / 379 </li>
 *   <li>{@code new ResourceLocation(ns, path)} -&gt; {@code Identifier.fromNamespaceAndPath(ns, path)}</li>
 * </ul>
 */
public final class NestLeaderOrganEffects {

    /**
     *  +  {@code entity_gravity}<b></b>
     *
     * <p> {@code static final List}{@code NeoForgeMod.SWIM_SPEED}
     * {@code Holder} {@code Attributes.*}  Holder
     *  1.20.1  tick /
     * </p>
     *
     * <p> {@link OrganStatSummary#STAT_ATTRIBUTE_KEYS}
     * {@code entity_gravity}</p>
     */
    private static final class Slots {
        static final List<AttributeSlot> ALL = List.of(
                new AttributeSlot("health", Attributes.MAX_HEALTH),
                new AttributeSlot("armor", Attributes.ARMOR),
                new AttributeSlot("armor_toughness", Attributes.ARMOR_TOUGHNESS),
                new AttributeSlot("attack_damage", Attributes.ATTACK_DAMAGE),
                new AttributeSlot("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE),
                new AttributeSlot("movement_speed", Attributes.MOVEMENT_SPEED),
                // STAGE A26.1.2 = NeoForgeMod.SWIM_SPEEDHolder<Attribute> 1.0
                new AttributeSlot("swim_speed", NeoForgeMod.SWIM_SPEED),
                // 26.1.2 1.20.1  forge:entity_reach  minecraft:entity_interaction_range
                new AttributeSlot("attack_range", Attributes.ENTITY_INTERACTION_RANGE),
                // 26.1.2 1.20.1  forge:block_reach  minecraft:block_interaction_range
                new AttributeSlot("block_reach", Attributes.BLOCK_INTERACTION_RANGE),
                // STAGE A  -2%
                // 26.1.2 forge:entity_gravity -> minecraft:gravity 0.08
                new AttributeSlot("entity_gravity", Attributes.GRAVITY));
    }

    /** = {@link #Slots#ALL} */
    private static List<AttributeSlot> attributeSlots() {
        return Slots.ALL;
    }

    /**
     * STAGE B {@link #Slots#ALL}
     *
     * <p> {@code "armor"} tick
     *  JSON
     *  =  id = </p>
     */
    static final String KEY_ARMOR_UNIQUE_PREFIX = "armor_unique.";
    /** STAGE B */
    static final String KEY_KNOCKBACK_UNIQUE_PREFIX = "knockback_resistance_unique.";

    /** {@code armor_unique.< id>} */
    static String armorUniqueKey(String effectId) {
        return KEY_ARMOR_UNIQUE_PREFIX + effectId;
    }

    /** {@code knockback_resistance_unique.< id>} */
    static String knockbackUniqueKey(String effectId) {
        return KEY_KNOCKBACK_UNIQUE_PREFIX + effectId;
    }

    /**
     *  /
     *
     * <p> {@link Slots#ALL} id
     *  {@link #UNIQUE_EFFECTS}  new
     * """"
     * </p>
     *
     * <p><b></b> {@link OrganStatSummary#STAT_ATTRIBUTE_KEYS}
     *  JSON </p>
     */
    @Nullable
    private static Holder<Attribute> uniqueAttributeOf(String key) {
        if (key == null) return null;
        if (key.startsWith(KEY_ARMOR_UNIQUE_PREFIX)) return Attributes.ARMOR;
        if (key.startsWith(KEY_KNOCKBACK_UNIQUE_PREFIX)) return Attributes.KNOCKBACK_RESISTANCE;
        return null;
    }

    /**  tick  */
    private static final Map<UUID, Map<String, Double>> APPLIED = new HashMap<>();

    /**
     *  tick
     *
     * <p>{@code epca#onPlayerTick}
     * {@link #tick(Player)} {@link #notLeader(Player)}
     * UUID  {@code NestLeaderManager} </p>
     */
    private static final Set<UUID> ACTIVE = new HashSet<>();

    /**
     *
     *
     * <p>STAGE B  {@link FixedAttributeEffect}
     * </p>
     */
    private static final List<UniqueEffect> UNIQUE_EFFECTS = List.of(
            new DiseasedHeartEffect(),
            new ReshapeFleshEffect(),
            new ReshapeShellEffect(),
            new TwistedBoneEffect());

    //  70% / 3  II 30  II
    //  NestLeaderHandler#onLivingHurt""

    private NestLeaderOrganEffects() {
    }


    /**
     *  tick
     *
     * <p>{@code epca#onPlayerTick}
     * /</p>
     */
    public static void tick(Player player) {
        if (player == null || player.level().isClientSide()) return;

        ACTIVE.add(player.getUUID());

        NestLeaderOrganData data = NestLeaderOrganSavedData.readOrCreate(player);
        OrganStatSummary summary = OrganStatSummary.compute(data);
        OrganStatProfile total = summary.totalBonus();

        applyAttribute(player, "health", total.health());
        applyAttribute(player, "armor", total.armor());
        applyAttribute(player, "armor_toughness", total.armorToughness());
        applyAttribute(player, "attack_damage", total.attackDamage());
        applyAttribute(player, "knockback_resistance", total.knockbackResistance());
        applyAttribute(player, "movement_speed", total.movementSpeed());
        applyAttribute(player, "swim_speed", total.swimSpeed());
        applyAttribute(player, "attack_range", total.attackRange());
        applyAttribute(player, "block_reach", total.blockReach());
        // STAGE A  -2%
        applyAttribute(player, "entity_gravity", total.entityGravity());

        for (UniqueEffect effect : UNIQUE_EFFECTS) {
            effect.tick(player, summary);
        }
    }

    /**
     * STAGE B
     *
     * <p> {@link FixedAttributeEffect}  {@link UniqueEffect#tick}
     *
     * {@link #tick(Player)}
     * {@link #applyAttribute(Player, String, double)}</p>
     */
    static void applyUniqueAttribute(Player player, String key, double value) {
        applyAttribute(player, key, value);
    }

    /**
     *
     *
     * <p>
     *  +  id
     *
     * STAGE B {@code armor_unique.<id>}
     *  {@link #attributeSlots()}
     *  id  {@link AttributeSlot#NAME_PREFIX} </p>
     */
    public static void clear(Player player) {
        if (player == null) return;
        APPLIED.remove(player.getUUID());
        ACTIVE.remove(player.getUUID());
        //  + entity_gravity
        for (AttributeSlot slot : attributeSlots()) {
            removeOrganModifiers(player, slot.attribute());
        }
        //  attributeSlots()
        //  Holder
        removeOrganModifiers(player, Attributes.ARMOR);
        removeOrganModifiers(player, Attributes.KNOCKBACK_RESISTANCE);
    }

    /**
     *  id  +
     *
     */
    private static void removeOrganModifiers(Player player, Holder<Attribute> attribute) {
        if (attribute == null) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        for (AttributeModifier modifier : List.copyOf(instance.getModifiers())) {
            if (isOrganModifier(modifier)) {
                instance.removeModifier(modifier);
            }
        }
    }

    /**
     * <b></b> {@code epca#onPlayerTick}
     *
     * <p> tick  tick
     * {@link Set#remove(Object)} </p>
     */
    public static void notLeader(Player player) {
        if (player == null || player.level().isClientSide()) return;
        if (ACTIVE.remove(player.getUUID())) {
            clear(player);
        }
    }

    /** / */
    public static boolean isApplied(UUID uuid) {
        return uuid != null && APPLIED.containsKey(uuid);
    }


    /**
     *
     *
     * <p> id
     * 26.1.2  {@code AttributeInstance#addTransientModifier}  id
     * {@code IllegalArgumentException}</p>
     *
     * <p> 0 </p>
     */
    private static void applyAttribute(Player player, String key, double value) {
        AttributeSlot slot = slotOf(key);
        if (slot == null) return;
        AttributeInstance instance = player.getAttribute(slot.attribute());
        if (instance == null) {
            //  tick
            applied(player).remove(key);
            return;
        }

        Map<String, Double> applied = applied(player);
        Double previous = applied.get(key);
        AttributeModifier existing = instance.getModifier(slot.modifierId());
        if (previous != null && previous == value && existing != null) {
            return;
        }

        instance.removeModifier(slot.modifierId());
        if (value != 0.0D) {
            instance.addTransientModifier(new AttributeModifier(
                    slot.modifierId(), value, AttributeModifier.Operation.ADD_VALUE));
        }
        applied.put(key, value);
    }

    private static Map<String, Double> applied(Player player) {
        return APPLIED.computeIfAbsent(player.getUUID(), ignored -> new LinkedHashMap<>());
    }

    @Nullable
    private static AttributeSlot slotOf(String key) {
        for (AttributeSlot slot : attributeSlots()) {
            if (slot.key().equals(key)) return slot;
        }
        // STAGE B uniqueAttributeOf
        Holder<Attribute> unique = uniqueAttributeOf(key);
        return unique == null ? null : new AttributeSlot(key, unique);
    }

    /**  id  +  {@link #clear(Player)} */
    private static boolean isOrganModifier(AttributeModifier modifier) {
        if (modifier == null) return false;
        Identifier id = modifier.id();
        return id != null && id.getNamespace().equals("epca")
                && id.getPath().startsWith(AttributeSlot.NAME_PREFIX);
    }

    /**
     *  +  + <b></b> id
     *
     * <p>26.1.2  {@code AttributeModifier}  {@code (Identifier id, double amount, Operation)}
     * 1.20.1  {@code UUID.nameUUIDFromBytes("epca:organ_" + )}  UUID
     *  {@link Identifier}{@code epca:organ_<>} {@code .}
     *  {@code entity_gravity}
     *  STAGE B  id id
     *  id </p>
     */
    private record AttributeSlot(String key, Holder<Attribute> attribute, Identifier modifierId) {

        /**  id  {@code clear}  */
        static final String NAME_PREFIX = "organ_";

        AttributeSlot(String key, Holder<Attribute> attribute) {
            this(key, attribute, Identifier.fromNamespaceAndPath("epca", NAME_PREFIX + key));
        }
    }

    //  SPEC D6

    /**
     * SPEC D6
     *  {@link #UNIQUE_EFFECTS}  =  /  =
     */
    public interface UniqueEffect {

        /**  id /  */
        String id();

        /**  tick {@code summary}  compute */
        void tick(Player player, OrganStatSummary summary);
    }

    /**
     *
     * <b> 10 </b>{@value #PERIOD_TICKS} tick<b> I  6 </b>
     * {@value #DURATION_TICKS} tick = 120 tick
     *
     * <p><b></b> N  10 SPEC=
     *  0  10 </p>
     *
     * <p> {@code NestLeaderOrganEffects}
     *
     * <b></b> {@code NestLeaderOrganSavedData}
     * {@code nestleader_organs.dat} UUID 10
     * <b></b>
     * <b></b> 10 </p>
     */
    static final class DiseasedHeartEffect implements UniqueEffect {

        /** 10  = 200 tick */
        static final int PERIOD_TICKS = 200;
        /**  I 6  = 120 tick */
        static final int DURATION_TICKS = 120;

        /**  */
        static final String PERSISTENT_KEY = "NestLeaderOrganEffects";
        /**  tick  */
        private static final String TAG_COUNTDOWN = "diseased_heart_countdown";

        /**  id {@link OrganStatSummary#countOf(Identifier)} */
        static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath("epca", "diseased_heart");

        @Override
        public String id() {
            return "diseased_heart";
        }

        @Override
        public void tick(Player player, OrganStatSummary summary) {
            // 26.1.2getCompound  Optional getCompoundOrEmptyCompoundTag.java  379
            CompoundTag tag = player.getPersistentData().getCompoundOrEmpty(PERSISTENT_KEY);
            int present = summary.countOf(ITEM_ID);
            if (present <= 0) {
                if (tag.contains(TAG_COUNTDOWN)) {
                    tag.remove(TAG_COUNTDOWN);
                    player.getPersistentData().put(PERSISTENT_KEY, tag);
                }
                return;
            }

            // 26.1.2getInt -> getIntOr(name, )CompoundTag.java  327
            int countdown = tag.getIntOr(TAG_COUNTDOWN, PERIOD_TICKS);
            countdown--;
            if (countdown <= 0) {
                // ambient=false, visible=true, showIcon=true
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 0, false, true, true));
                countdown = PERIOD_TICKS;
            }
            tag.putInt(TAG_COUNTDOWN, countdown);
            player.getPersistentData().put(PERSISTENT_KEY, tag);
        }
    }

    /**
     * STAGE BSPEC  /  /
     *
     * <p>
     *  id</p>
     *
     * <h2> / </h2>
     * <p>{@link DiseasedHeartEffect}  10
     * <b></b> tick
     * {@link #applyUniqueAttribute} {@link #applyAttribute}
     *  return/
     * {@link NestLeaderOrganEffects#tick} </p>
     *
     * <h2></h2>
     * <p> {@link OrganStatSummary#countOf(Identifier)}<b></b>
     * 6  + 39  + 33  &gt; 0  SPEC
     * +0.1  2
     *  {@link #present(OrganStatSummary)}
     * {@link OrganStatSummary#countIn(OrganSlotGroup, Identifier)}</p>
     */
    abstract static class FixedAttributeEffect implements UniqueEffect {

        /**  id */
        abstract Identifier itemId();

        /**
         *  -&gt;
         *
         * <p> {@link #attributeSlots()}
         * {@link #armorUniqueKey(String)} / {@link #knockbackUniqueKey(String)}
         *  {@code applyAttribute} </p>
         */
        abstract Map<String, Double> fixedBonuses();

        /**  =  */
        boolean present(OrganStatSummary summary) {
            return summary.countOf(itemId()) > 0;
        }

        @Override
        public void tick(Player player, OrganStatSummary summary) {
            boolean present = present(summary);
            for (Map.Entry<String, Double> bonus : fixedBonuses().entrySet()) {
                //  0applyAttribute  0 ""
                applyUniqueAttribute(player, bonus.getKey(), present ? bonus.getValue() : 0.0D);
            }
        }
    }

    /**
     *  {@code epca:reshape_flesh}
     *  <b>+0.1 +2 </b>SPEC +0.1  2
     *
     * <p> {@link #attributeSlots()}
     * {@link AttributeModifier.Operation#ADD_VALUE}
     * {@code 0.1}  +10% 0..11.0 =
     * {@code 2.0}  +2  JSON  {@code "armor": 2.5} </p>
     */
    static final class ReshapeFleshEffect extends FixedAttributeEffect {

        static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath("epca", "reshape_flesh");
        /** SPEC+0.1 */
        static final double KNOCKBACK_RESISTANCE = 0.1D;
        /** SPEC+2 */
        static final double ARMOR = 2.0D;

        @Override
        public String id() {
            return "reshape_flesh";
        }

        @Override
        Identifier itemId() {
            return ITEM_ID;
        }

        @Override
        Map<String, Double> fixedBonuses() {
            return Map.of(
                    knockbackUniqueKey(id()), KNOCKBACK_RESISTANCE,
                    armorUniqueKey(id()), ARMOR);
        }
    }

    /**
     *  {@code epca:reshape_shell}
     *  <b>+0.15 +3 </b>
     */
    static final class ReshapeShellEffect extends FixedAttributeEffect {

        static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath("epca", "reshape_shell");
        /** SPEC+0.15 */
        static final double KNOCKBACK_RESISTANCE = 0.15D;
        /** SPEC+3 */
        static final double ARMOR = 3.0D;

        @Override
        public String id() {
            return "reshape_shell";
        }

        @Override
        Identifier itemId() {
            return ITEM_ID;
        }

        @Override
        Map<String, Double> fixedBonuses() {
            return Map.of(
                    knockbackUniqueKey(id()), KNOCKBACK_RESISTANCE,
                    armorUniqueKey(id()), ARMOR);
        }
    }

    /**
     *  {@code epca:twisted_bone}
     *  <b>+0.15 +2 </b>
     *
     * <p><b></b>
     *  id</p>
     */
    static final class TwistedBoneEffect extends FixedAttributeEffect {

        static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath("epca", "twisted_bone");
        /** SPEC+0.15 */
        static final double KNOCKBACK_RESISTANCE = 0.15D;
        /** SPEC+2 */
        static final double ARMOR = 2.0D;

        @Override
        public String id() {
            return "twisted_bone";
        }

        @Override
        Identifier itemId() {
            return ITEM_ID;
        }

        @Override
        Map<String, Double> fixedBonuses() {
            return Map.of(
                    knockbackUniqueKey(id()), KNOCKBACK_RESISTANCE,
                    armorUniqueKey(id()), ARMOR);
        }
    }
}

