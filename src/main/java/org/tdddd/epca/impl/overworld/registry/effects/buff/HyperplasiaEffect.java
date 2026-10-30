package org.tdddd.epca.impl.overworld.registry.effects.buff;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

/**
 * Hyperplasia (1.20.1 parity):
 * <ul>
 *   <li>parasites (incl. the nest/hive leader) heal {@code 1 + amplifier} per second and take no decay;</li>
 *   <li>any other non-player mob permanently grows its MAX_HEALTH by {@code 2 + 2 * amplifier} per second;</li>
 *   <li>everything that is not a parasite takes {@code 1 + amplifier} minimum damage per second.</li>
 * </ul>
 *
 * <p>Because the MAX_HEALTH bonus accumulates without a fixed cap it cannot be expressed through
 * {@link MobEffect#addAttributeModifier} (that template is per-amplifier and constant), so it is an
 * explicitly tracked transient modifier instead. The effect-ending recovery lives here:</p>
 * <ul>
 *   <li>{@link #HEALTH_MARK} records that this entity's MAX_HEALTH was inflated by the effect;</li>
 *   <li>{@link #restoreHealth(LivingEntity)} rolls the whole accumulated bonus back and clamps the
 *       current health, and is idempotent (safe to call on entities that never received the bonus);</li>
 *   <li>{@code SymbiosisDeathHandler} calls it as soon as the effect expires and, as a safety net,
 *       sweeps every marked entity once per second while it no longer has the effect (this second
 *       path is exactly the 1.20.1 {@code LivingTickEvent} behaviour and also covers removal by
 *       milk / {@code /effect clear} / death-less unload, where no expiry event is posted).</li>
 * </ul>
 */
public class HyperplasiaEffect extends MobEffect {
    /** Transient MAX_HEALTH modifier id. Same id as 1.20.1, so the rollback contract is unchanged. */
    public static final Identifier HEALTH_MOD = Identifier.fromNamespaceAndPath("epca", "hyperplasia_health");

    /**
     * Persistent-data flag meaning "this entity's MAX_HEALTH carries a hyperplasia bonus".
     * Kept byte-for-byte identical to the 1.20.1 key so both trees agree on saved data.
     */
    public static final String HEALTH_MARK = "epca:hyperplasia_health";

    /** Extra MAX_HEALTH per second at amplifier 0 (amplifier adds another {@code 2.0} each level). */
    private static final double HEALTH_GROWTH = 2.0D;

    public HyperplasiaEffect() { super(MobEffectCategory.BENEFICIAL, 0x9C3B6E); }

    @Override public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) { return tickCount % 20 == 0; }

    @Override public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        boolean parasite = entity instanceof IParasite
                || (entity instanceof Player player && HiveLeaderManager.isHiveLeader(player.getUUID()));

        // Parasites regenerate instead of decaying, and never receive the growth bonus.
        if (parasite) { entity.heal(1.0F + amplifier); return true; }

        if (!(entity instanceof Player)) {
            AttributeInstance attribute = entity.getAttribute(Attributes.MAX_HEALTH);
            if (attribute != null) {
                AttributeModifier old = attribute.getModifier(HEALTH_MOD);
                double amount = (old == null ? 0.0D : old.amount()) + HEALTH_GROWTH + amplifier * HEALTH_GROWTH;
                attribute.addOrUpdateTransientModifier(
                        new AttributeModifier(HEALTH_MOD, amount, AttributeModifier.Operation.ADD_VALUE));
                // Mark it so the bonus can be rolled back once the effect ends.
                entity.getPersistentData().putBoolean(HEALTH_MARK, true);
            }
        }

        // Players and non-parasite mobs both decay while the effect runs.
        minimum(level, entity, 1.0F + amplifier);
        return true;
    }

    /**
     * Rolls back the whole MAX_HEALTH bonus this effect accumulated and clamps the current health to
     * the restored maximum. Idempotent: calling it for an entity without the modifier (or without the
     * mark) only clears the mark and clamps the health.
     */
    public static void restoreHealth(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null && attribute.hasModifier(HEALTH_MOD)) {
            attribute.removeModifier(HEALTH_MOD);
        }
        // The bonus is gone, so health may now sit above the new maximum.
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
        entity.getPersistentData().remove(HEALTH_MARK);
    }

    /** True when this effect inflated the entity's MAX_HEALTH and the bonus has not been rolled back yet. */
    public static boolean hasHealthMark(LivingEntity entity) {
        return entity.getPersistentData().getBooleanOr(HEALTH_MARK, false);
    }

    private static void minimum(ServerLevel level, LivingEntity entity, float amount) {
        Holder<DamageType> holder = level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(ModDamageTypes.MINIMUM);
        entity.hurt(new DamageSource(holder), amount);
    }
}
