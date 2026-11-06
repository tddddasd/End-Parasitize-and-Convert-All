package org.tdddd.epca.impl.overworld.registry.effects.buff;

import org.tdddd.epca.impl.events.ParasiteEvents;
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
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

public class HyperplasiaEffect extends MobEffect {

    public static final Identifier HEALTH_MOD = Identifier.fromNamespaceAndPath("epca", "hyperplasia_health");

    public static final String HEALTH_MARK = "epca:hyperplasia_health";

    private static final double HEALTH_GROWTH = 2.0D;

    public HyperplasiaEffect() { super(MobEffectCategory.BENEFICIAL, 0x9C3B6E); }

    @Override public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) { return tickCount % 20 == 0; }

    @Override public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        boolean parasite = ParasiteEvents.isParasiteEntity(entity)
                || (entity instanceof Player player && HiveLeaderManager.isHiveLeader(player.getUUID()));

        if (parasite) { entity.heal(1.0F + amplifier); return true; }

        if (!(entity instanceof Player)) {
            AttributeInstance attribute = entity.getAttribute(Attributes.MAX_HEALTH);
            if (attribute != null) {
                AttributeModifier old = attribute.getModifier(HEALTH_MOD);
                double amount = (old == null ? 0.0D : old.amount()) + HEALTH_GROWTH + amplifier * HEALTH_GROWTH;
                attribute.addOrUpdateTransientModifier(
                        new AttributeModifier(HEALTH_MOD, amount, AttributeModifier.Operation.ADD_VALUE));

                entity.getPersistentData().putBoolean(HEALTH_MARK, true);
            }
        }

        minimum(level, entity, 1.0F + amplifier);
        return true;
    }

    public static void restoreHealth(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null && attribute.hasModifier(HEALTH_MOD)) {
            attribute.removeModifier(HEALTH_MOD);
        }

        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
        entity.getPersistentData().remove(HEALTH_MARK);
    }

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
