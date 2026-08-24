package org.tdddd.epca.impl.mixin.client;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import net.minecraft.client.model.HumanoidModel;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends LivingEntity> {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        if (!player.isUsingItem()) return;
        ItemStack activeItem = player.getUseItem();
        if (activeItem.getItem() != ModItems.WOODEN_SPEAR.get() && activeItem.getItem() != ModItems.STONE_SPEAR.get() &&
                activeItem.getItem() != ModItems.FLINT_SPEAR.get() && activeItem.getItem() != ModItems.COPPER_SPEAR.get() &&
                activeItem.getItem() != ModItems.IRON_SPEAR.get() && activeItem.getItem() != ModItems.GOLDEN_SPEAR.get() &&
                activeItem.getItem() != ModItems.DIAMOND_SPEAR.get() && activeItem.getItem() != ModItems.NETHERITE_SPEAR.get()) return;

        HumanoidModel<T> model = (HumanoidModel<T>) (Object) this;
        
        HumanoidArm usingArm = player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        
        if (usingArm == HumanoidArm.RIGHT) {
            model.rightArm.yRot += (float) Math.PI; 
        } else {
            model.leftArm.yRot += (float) Math.PI;
        }
    }
}