package org.tdddd.epca.impl.events;

import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import java.util.ArrayList;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

import java.util.Collection;

@EventBusSubscriber(modid = epca.MODID)
public class EnderBladeAttackHandler {
    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        Level level = event.getEntity().level();
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player)) return;
        if (!(source.getDirectEntity() instanceof LivingEntity)) return;
        LivingEntity target = event.getEntity();

        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) return;
        
        CompoundTag tag = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.getBooleanOr("epca:ender_blade", false)) return;
        
        float baseAttack = getWeaponAttackDamage(weapon);
        float extra = (baseAttack + 1) * 0.25f;

        Registry<DamageType> registry = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getOrThrow(ModDamageTypes.MINIMUM);
        DamageSource minimumSource = new DamageSource(holder);
        target.hurt(minimumSource, extra);
    }

    private static float getWeaponAttackDamage(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getAttributeModifiers();
        Collection<AttributeModifier> damages = new ArrayList<>();
        modifiers.forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) {
                damages.add(modifier);
            }
        });
        return (float) damages.stream().mapToDouble(AttributeModifier::amount).sum();
    }
}