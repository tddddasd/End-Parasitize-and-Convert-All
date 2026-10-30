package org.tdddd.epca.impl.overworld.registry.effects.buff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.UUID;

/** Hyperplasia: regeneration for parasites; max-health growth for other mobs; decay for players. */
public class HyperplasiaEffect extends MobEffect {
    public static final UUID HEALTH_MOD = UUID.fromString("8f4c1c7e-6d0f-4d0e-9a1e-2b7a4c5d6e7f");
    public static final String HEALTH_MARK = "epca:hyperplasia_health";

    public HyperplasiaEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x9C3B6E);
    }

    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 20 == 0; }

    @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
        boolean parasite = entity instanceof IParasite
                || (entity instanceof Player player && HiveLeaderManager.isHiveLeader(player.getUUID()));
        if (parasite) {
            entity.heal(1.0F + amplifier);
        } else if (!(entity instanceof Player)) {
            var attribute = entity.getAttribute(Attributes.MAX_HEALTH);
            if (attribute != null) {
                var old = attribute.getModifier(HEALTH_MOD);
                double amount = (old == null ? 0.0D : old.getAmount()) + 2.0D + amplifier * 2.0D;
                if (old != null) attribute.removeModifier(HEALTH_MOD);
                attribute.addTransientModifier(new AttributeModifier(HEALTH_MOD, "epca:hyperplasia",
                        amount, AttributeModifier.Operation.ADDITION));
                entity.getPersistentData().putBoolean(HEALTH_MARK, true);
            }
            minimum(entity, 1.0F + amplifier);
        } else {
            minimum(entity, 1.0F + amplifier);
        }
    }

    private static void minimum(LivingEntity entity, float amount) {
        var registry = entity.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
        entity.hurt(new net.minecraft.world.damagesource.DamageSource(
                registry.getHolderOrThrow(org.tdddd.yawning_neko_api.damages.ModDamageTypes.MINIMUM)), amount);
    }
}