package org.tdddd.epca.impl.events;

import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModDamageTypes;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class EnderBladeAttackHandler {
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Level level = event.getEntity().level();
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player)) return;
        if (!(source.getDirectEntity() instanceof LivingEntity)) return;
        LivingEntity target = event.getEntity();

        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) return;
        // 检测 NBT 标记
        CompoundTag tag = weapon.getTag();
        if (tag == null || !tag.getBoolean("epca:ender_blade")) return;
        // 获取武器基础攻击力（面板）
        float baseAttack = getWeaponAttackDamage(weapon);
        float extra = (baseAttack + 1) * 0.25f;

        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
        DamageSource minimumSource = new DamageSource(holder);
        target.hurt(minimumSource, extra);
    }

    private static float getWeaponAttackDamage(ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        Collection<AttributeModifier> damages = modifiers.get(Attributes.ATTACK_DAMAGE);
        return (float) damages.stream().mapToDouble(AttributeModifier::getAmount).sum();
    }
}