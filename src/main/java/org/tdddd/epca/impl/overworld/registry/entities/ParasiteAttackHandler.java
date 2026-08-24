package org.tdddd.epca.impl.overworld.registry.entities;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.ModDamageTypes;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedPumpkinHead;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSlimeSize0;
import org.tdddd.epca.impl.epca;
import org.tdddd.yawning_neko_api.data.MinimumDamageManager;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class ParasiteAttackHandler {
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }

        
        MinimumDamageManager manager = MinimumDamageManager.getInstance();
        if (manager != null && manager.hasMinimumDamageConfig(attacker)) {
            return;
        }

        if (attacker instanceof IOnesent) {
            
            LivingEntity target = event.getEntity();
            
            Registry<DamageType> registry = target.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE);
            Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
            DamageSource minimumSource = new DamageSource(holder);

            target.hurt(minimumSource, 0.1F);
        }

        if (attacker instanceof IInfested) {
            if (attacker instanceof InfestedSlimeSize0 || attacker instanceof InfestedPumpkinHead) {
                return;
            }
            
            LivingEntity target = event.getEntity();

            
            Registry<DamageType> registry = target.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE);
            Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
            DamageSource minimumSource = new DamageSource(holder);

            
            target.hurt(minimumSource, 0.5F);
        }

        if (attacker instanceof IReshape) {
            
            LivingEntity target = event.getEntity();

            
            Registry<DamageType> registry = target.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE);
            Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
            DamageSource minimumSource = new DamageSource(holder);

            
            target.hurt(minimumSource, 1.0F);
        }
    }
}