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

public final class HiveLeaderOrganEffects {

    private static final class Slots {
        static final List<AttributeSlot> ALL = List.of(
                new AttributeSlot("health", Attributes.MAX_HEALTH),
                new AttributeSlot("armor", Attributes.ARMOR),
                new AttributeSlot("armor_toughness", Attributes.ARMOR_TOUGHNESS),
                new AttributeSlot("attack_damage", Attributes.ATTACK_DAMAGE),
                new AttributeSlot("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE),
                new AttributeSlot("movement_speed", Attributes.MOVEMENT_SPEED),

                new AttributeSlot("swim_speed", NeoForgeMod.SWIM_SPEED),

                new AttributeSlot("attack_range", Attributes.ENTITY_INTERACTION_RANGE),

                new AttributeSlot("block_reach", Attributes.BLOCK_INTERACTION_RANGE),

                new AttributeSlot("entity_gravity", Attributes.GRAVITY));
    }

    private static List<AttributeSlot> attributeSlots() {
        return Slots.ALL;
    }

    static final String KEY_ARMOR_UNIQUE_PREFIX = "armor_unique.";

    static final String KEY_KNOCKBACK_UNIQUE_PREFIX = "knockback_resistance_unique.";

    static String armorUniqueKey(String effectId) {
        return KEY_ARMOR_UNIQUE_PREFIX + effectId;
    }

    static String knockbackUniqueKey(String effectId) {
        return KEY_KNOCKBACK_UNIQUE_PREFIX + effectId;
    }

    @Nullable
    private static Holder<Attribute> uniqueAttributeOf(String key) {
        if (key == null) return null;
        if (key.startsWith(KEY_ARMOR_UNIQUE_PREFIX)) return Attributes.ARMOR;
        if (key.startsWith(KEY_KNOCKBACK_UNIQUE_PREFIX)) return Attributes.KNOCKBACK_RESISTANCE;
        return null;
    }

    private static final Map<UUID, Map<String, Double>> APPLIED = new HashMap<>();

    private static final Set<UUID> ACTIVE = new HashSet<>();

    private static final List<UniqueEffect> UNIQUE_EFFECTS = List.of(
            new DiseasedHeartEffect(),
            new ReshapeFleshEffect(),
            new ReshapeShellEffect(),
            new TwistedBoneEffect());

    private HiveLeaderOrganEffects() {
    }

    public static void tick(Player player) {
        if (player == null || player.level().isClientSide()) return;

        ACTIVE.add(player.getUUID());

        HiveLeaderOrganData data = HiveLeaderOrganSavedData.readOrCreate(player);
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

        applyAttribute(player, "entity_gravity", total.entityGravity());

        for (UniqueEffect effect : UNIQUE_EFFECTS) {
            effect.tick(player, summary);
        }
    }

    static void applyUniqueAttribute(Player player, String key, double value) {
        applyAttribute(player, key, value);
    }

    public static void clear(Player player) {
        if (player == null) return;
        APPLIED.remove(player.getUUID());
        ACTIVE.remove(player.getUUID());

        for (AttributeSlot slot : attributeSlots()) {
            removeOrganModifiers(player, slot.attribute());
        }

        removeOrganModifiers(player, Attributes.ARMOR);
        removeOrganModifiers(player, Attributes.KNOCKBACK_RESISTANCE);
    }

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

    public static void notLeader(Player player) {
        if (player == null || player.level().isClientSide()) return;
        if (ACTIVE.remove(player.getUUID())) {
            clear(player);
        }
    }

    public static boolean isApplied(UUID uuid) {
        return uuid != null && APPLIED.containsKey(uuid);
    }

    private static void applyAttribute(Player player, String key, double value) {
        AttributeSlot slot = slotOf(key);
        if (slot == null) return;
        AttributeInstance instance = player.getAttribute(slot.attribute());
        if (instance == null) {

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

        Holder<Attribute> unique = uniqueAttributeOf(key);
        return unique == null ? null : new AttributeSlot(key, unique);
    }

    private static boolean isOrganModifier(AttributeModifier modifier) {
        if (modifier == null) return false;
        Identifier id = modifier.id();
        return id != null && id.getNamespace().equals("epca")
                && id.getPath().startsWith(AttributeSlot.NAME_PREFIX);
    }

    private record AttributeSlot(String key, Holder<Attribute> attribute, Identifier modifierId) {

        static final String NAME_PREFIX = "organ_";

        AttributeSlot(String key, Holder<Attribute> attribute) {
            this(key, attribute, Identifier.fromNamespaceAndPath("epca", NAME_PREFIX + key));
        }
    }

    public interface UniqueEffect {

        String id();

        void tick(Player player, OrganStatSummary summary);
    }

    static final class DiseasedHeartEffect implements UniqueEffect {

        static final int PERIOD_TICKS = 200;

        static final int DURATION_TICKS = 120;

        static final String PERSISTENT_KEY = "HiveLeaderOrganEffects";

        private static final String TAG_COUNTDOWN = "diseased_heart_countdown";

        static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath("epca", "diseased_heart");

        @Override
        public String id() {
            return "diseased_heart";
        }

        @Override
        public void tick(Player player, OrganStatSummary summary) {

            CompoundTag tag = player.getPersistentData().getCompoundOrEmpty(PERSISTENT_KEY);
            int present = summary.countOf(ITEM_ID);
            if (present <= 0) {
                if (tag.contains(TAG_COUNTDOWN)) {
                    tag.remove(TAG_COUNTDOWN);
                    player.getPersistentData().put(PERSISTENT_KEY, tag);
                }
                return;
            }

            int countdown = tag.getIntOr(TAG_COUNTDOWN, PERIOD_TICKS);
            countdown--;
            if (countdown <= 0) {

                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 0, false, true, true));
                countdown = PERIOD_TICKS;
            }
            tag.putInt(TAG_COUNTDOWN, countdown);
            player.getPersistentData().put(PERSISTENT_KEY, tag);
        }
    }

    abstract static class FixedAttributeEffect implements UniqueEffect {

        abstract Identifier itemId();

        abstract Map<String, Double> fixedBonuses();

        boolean present(OrganStatSummary summary) {
            return summary.countOf(itemId()) > 0;
        }

        @Override
        public void tick(Player player, OrganStatSummary summary) {
            boolean present = present(summary);
            for (Map.Entry<String, Double> bonus : fixedBonuses().entrySet()) {

                applyUniqueAttribute(player, bonus.getKey(), present ? bonus.getValue() : 0.0D);
            }
        }
    }

    static final class ReshapeFleshEffect extends FixedAttributeEffect {

        static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath("epca", "reshape_flesh");

        static final double KNOCKBACK_RESISTANCE = 0.1D;

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

    static final class ReshapeShellEffect extends FixedAttributeEffect {

        static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath("epca", "reshape_shell");

        static final double KNOCKBACK_RESISTANCE = 0.15D;

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

    static final class TwistedBoneEffect extends FixedAttributeEffect {

        static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath("epca", "twisted_bone");

        static final double KNOCKBACK_RESISTANCE = 0.15D;

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
