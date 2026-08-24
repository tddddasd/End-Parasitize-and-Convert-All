package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.tdddd.epca.impl.overworld.registry.ModDamageTypes;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderErosionEffect extends MobEffect implements RemovableEffect {

    
    private static final Map<UUID, Float> erosionValues = new HashMap<>();
    
    private static final Map<UUID, Integer> erosionTimers = new HashMap<>();

    public EnderErosionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B008B); 
        
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public boolean isRemovable() {
        return false; 
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            
            UUID uuid = entity.getUUID();
            float currentErosion = erosionValues.getOrDefault(uuid, 0f);
            float newErosion = currentErosion + (amplifier + 1) * 0.5f; 
            erosionValues.put(uuid, newErosion);
        }
        super.applyEffectTick(entity, amplifier);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        
        return duration % 10 == 0;
    }

    @SubscribeEvent
    public void onLivingTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            UUID uuid = event.player.getUUID();
            if (event.player.hasEffect(this)) {
                
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
        if (effectInstance != null && effectInstance.getEffect() == this && event.getEntity() != null) {
            LivingEntity entity = event.getEntity();
            UUID uuid = entity.getUUID();

            float erosion = erosionValues.getOrDefault(uuid, 0f);

            if (erosion > 0 && !entity.level().isClientSide) {
                float minimumDamage = erosion * 0.25f;
                
                Registry<DamageType> registry = entity.level().registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE);
                Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
                DamageSource minimumSource = new DamageSource(holder);
                entity.hurt(minimumSource, minimumDamage);

                
                float magicDamage = erosion * 0.75f;
                entity.hurt(entity.damageSources().magic(), magicDamage);
            }

            
            erosionValues.remove(uuid);
            erosionTimers.remove(uuid);
        }
    }

    @SubscribeEvent
    public void onEffectAdded(MobEffectEvent.Added event) {
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance != null && effectInstance.getEffect() == this && event.getEntity() != null) {
            
            UUID uuid = event.getEntity().getUUID();
            erosionValues.put(uuid, 0f);
            erosionTimers.put(uuid, 0);
        }
    }

    @SubscribeEvent
    public void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance != null && effectInstance.getEffect() == this && event.getEntity() != null) {
            LivingEntity entity = event.getEntity();
            UUID uuid = entity.getUUID();

            float erosion = erosionValues.getOrDefault(uuid, 0f);

            if (erosion > 0 && !entity.level().isClientSide) {
                
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
                entity.hurt(entity.damageSources().magic(), magicDamage);
            }

            
            erosionValues.remove(uuid);
            erosionTimers.remove(uuid);
        }
    }

    @SubscribeEvent
    public void onEntityDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().hasEffect(this)) {
            
            UUID uuid = event.getEntity().getUUID();
            erosionValues.remove(uuid);
            erosionTimers.remove(uuid);
        }
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);
        
        if (!entity.level().isClientSide) {
            UUID uuid = entity.getUUID();
            erosionValues.put(uuid, 0f);
            erosionTimers.put(uuid, 0);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        
    }

    
    public static float getErosionValue(UUID uuid) {
        return erosionValues.getOrDefault(uuid, 0f);
    }

    
    public static void clearErosionValue(UUID uuid) {
        erosionValues.remove(uuid);
        erosionTimers.remove(uuid);
    }
}