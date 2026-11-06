package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.ThrownCopperSpear;

public class CopperSpear extends Item {
    private final ItemAttributeModifiers defaultModifiers;
    public static final int MAX_CHARGE = 12000;
    public static final int FULL_CHARGE_TICKS = 20;
    public static final float MAX_SPEED = 2.5F;
    public static final float MIN_SPEED = 1.2F;

    public CopperSpear(Properties properties) {
        super(properties);
                this.defaultModifiers = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.8, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(Identifier.fromNamespaceAndPath("epca", "weapon_reach"), 1.0,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return this.defaultModifiers;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return MAX_CHARGE;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {

        return ItemUseAnimation.TRIDENT;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME.heldItemTransformedTo(player.getItemInHand(hand));
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (!(livingEntity instanceof Player player)) return false;
        int charge = this.getUseDuration(stack, livingEntity) - timeCharged;

        int effectiveCharge = Math.min(charge, FULL_CHARGE_TICKS);
        float chargePercent = (float) effectiveCharge / FULL_CHARGE_TICKS;
        float speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * chargePercent;
        if (speed < 0.1F) return false;

        if (!level.isClientSide()) {
            boolean isCreative = player.getAbilities().instabuild;
            ThrownCopperSpear spear = new ThrownCopperSpear(level, player, stack, isCreative);
            spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, 1.0F);
            if (charge >= FULL_CHARGE_TICKS) {
                spear.setCritArrow(true);
            }
            level.addFreshEntity(spear);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.awardStat(Stats.ITEM_USED.get(this));

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.is(Items.ENCHANTED_BOOK) ||
                enchantment.is(Enchantments.MENDING) ||
                enchantment.is(Enchantments.UNBREAKING) ||
                enchantment.is(Enchantments.SHARPNESS) ||
                enchantment.is(Enchantments.SMITE) ||
                enchantment.is(Enchantments.BANE_OF_ARTHROPODS) ||
                enchantment.is(Enchantments.FIRE_ASPECT) ||
                enchantment.is(Enchantments.LOYALTY);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.getAbilities().instabuild) {

            stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {

        if (miningEntity instanceof Player player && !player.getAbilities().instabuild) {
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness > 0.0F) {
                stack.hurtAndBreak(1, miningEntity, EquipmentSlot.MAINHAND);
            }
        }
        return true;
    }

}
