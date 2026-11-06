package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderErosionEffect extends MobEffect implements RemovableEffect {

    private static final Map<UUID, Float> erosionValues = new HashMap<>();

    private static final Map<UUID, Integer> erosionTimers = new HashMap<>();

    public EnderErosionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B008B);

        NeoForge.EVENT_BUS.register(this);
    }

    private Holder<MobEffect> holder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this);
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplifier) {

        UUID uuid = entity.getUUID();
        float currentErosion = erosionValues.getOrDefault(uuid, 0f);
        float newErosion = currentErosion + (amplifier + 1) * 0.5f;
        erosionValues.put(uuid, newErosion);

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {

        return tickCount % 10 == 0;
    }

    @SubscribeEvent
    public void onLivingTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) {
            UUID uuid = event.getEntity().getUUID();
            if (event.getEntity().hasEffect(holder())) {

                int timer = erosionTimers.getOrDefault(uuid, 0);
                timer++;
                if (timer >= 10) {
                    timer = 0;
                }
                erosionTimers.put(uuid, timer);
            }
        }
    }

    @SubscribeEvent
    public void onEffectRemove(MobEffectEvent.Remove event) {
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance != null && effectInstance.getEffect().value() == this && event.getEntity() != null) {
            LivingEntity entity = event.getEntity();
            UUID uuid = entity.getUUID();

            float erosion = erosionValues.getOrDefault(uuid, 0f);

            if (erosion > 0 && !entity.level().isClientSide()) {
                ServerLevel serverLevel = (ServerLevel) entity.level();
                float minimumDamage = erosion * 0.25f;

                Registry<DamageType> registry = entity.level().registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE);
                Holder<DamageType> holder = registry.getOrThrow(ModDamageTypes.MINIMUM);
                DamageSource minimumSource = new DamageSource(holder);

                entity.hurtServer(serverLevel, minimumSource, minimumDamage);

                float magicDamage = erosion * 0.75f;
                entity.hurtServer(serverLevel, entity.damageSources().magic(), magicDamage);
            }

            erosionValues.remove(uuid);
            erosionTimers.remove(uuid);
        }
    }

    @SubscribeEvent
    public void onEffectAdded(MobEffectEvent.Added event) {
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance != null && effectInstance.getEffect().value() == this && event.getEntity() != null) {

            UUID uuid = event.getEntity().getUUID();
            erosionValues.put(uuid, 0f);
            erosionTimers.put(uuid, 0);
        }
    }

    @SubscribeEvent
    public void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance != null && effectInstance.getEffect().value() == this && event.getEntity() != null) {
            LivingEntity entity = event.getEntity();
            UUID uuid = entity.getUUID();

            float erosion = erosionValues.getOrDefault(uuid, 0f);

            if (erosion > 0 && !entity.level().isClientSide()) {
                ServerLevel serverLevel = (ServerLevel) entity.level();

                float setHealthDamage = erosion * 0.25f;
                float currentHealth = entity.getHealth();
                float newHealth = currentHealth - setHealthDamage;

                if (newHealth <= 0) {
                    entity.setHealth(0);
                    entity.die(entity.damageSources().magic());
                } else {
                    entity.setHealth(newHealth);
                }

                float magicDamage = erosion * 0.75f;
                entity.hurtServer(serverLevel, entity.damageSources().magic(), magicDamage);
            }

            erosionValues.remove(uuid);
            erosionTimers.remove(uuid);
        }
    }

    @SubscribeEvent
    public void onEntityDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().hasEffect(holder())) {

            UUID uuid = event.getEntity().getUUID();
            erosionValues.remove(uuid);
            erosionTimers.remove(uuid);
        }
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributeMap) {
        super.removeAttributeModifiers(attributeMap);

    }
}
