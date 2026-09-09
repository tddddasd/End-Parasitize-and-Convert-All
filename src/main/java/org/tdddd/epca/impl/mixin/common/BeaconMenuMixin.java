package org.tdddd.epca.impl.mixin.common;

import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedDiamond;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedEmerald;
import org.tdddd.epca.impl.utils.IBeaconMixin;

import java.util.Optional;

@Mixin(BeaconMenu.class)
public abstract class BeaconMenuMixin {
    @Shadow private Container beacon;
    @Shadow private ContainerLevelAccess access;

    @Inject(method = "updateEffects", at = @At("HEAD"))
    private void onUpdateEffectsHead(Optional<MobEffect> primary, Optional<MobEffect> secondary, CallbackInfo ci) {
        ItemStack stack = this.beacon.getItem(0);
        Item item = stack.getItem();
        this.access.execute((level, pos) -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BeaconBlockEntity beaconBlock) {
                IBeaconMixin mixin = (IBeaconMixin) beaconBlock;
                if (item instanceof InfestedDiamond || item instanceof InfestedEmerald) {
                    mixin.setPaymentItem(item);
                } else {
                    mixin.setPaymentItem(null);
                }
            }
        });
    }
}