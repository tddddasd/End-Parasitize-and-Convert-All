package org.tdddd.epca.impl.overworld.registry.items.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.ThrownCopperSpear;

public class CopperSpear extends Item {
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;
    public static final int MAX_CHARGE = 12000; 
    public static final int FULL_CHARGE_TICKS = 20; 
    public static final float MAX_SPEED = 2.5F; 
    public static final float MIN_SPEED = 1.2F; 

    public CopperSpear(Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        
        
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 6.0, AttributeModifier.Operation.ADDITION));
        
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.8, AttributeModifier.Operation.ADDITION));
        
        builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier("weapon_reach", 1.0, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return MAX_CHARGE;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR; 
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (!(livingEntity instanceof Player player)) return;
        int charge = this.getUseDuration(stack) - timeCharged;
        
        int effectiveCharge = Math.min(charge, FULL_CHARGE_TICKS);
        float chargePercent = (float) effectiveCharge / FULL_CHARGE_TICKS;
        float speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * chargePercent;
        if (speed < 0.1F) return;

        if (!level.isClientSide) {
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
    }

    
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        return enchantment == Enchantments.MENDING ||
                enchantment == Enchantments.UNBREAKING ||
                enchantment == Enchantments.SHARPNESS ||
                enchantment == Enchantments.SMITE ||
                enchantment == Enchantments.BANE_OF_ARTHROPODS ||
                enchantment == Enchantments.FIRE_ASPECT ||
                enchantment == Enchantments.LOYALTY;
    }

    
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.getAbilities().instabuild) {
            
            stack.hurtAndBreak(1, attacker, (entity) -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return true;
    }

    
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        
        if (miningEntity instanceof Player player && !player.getAbilities().instabuild) {
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness > 0.0F) {
                stack.hurtAndBreak(1, miningEntity, (entity) -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }
        }
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 13;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.COPPER_INGOT);
    }
}