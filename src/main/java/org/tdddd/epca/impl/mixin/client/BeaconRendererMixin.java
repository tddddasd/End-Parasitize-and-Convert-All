package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedDiamond;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedEmerald;
import org.tdddd.epca.impl.utils.IBeaconMixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BeaconRenderer.class)
public class BeaconRendererMixin {
    @Unique private static final ThreadLocal<BeaconBlockEntity> CURRENT_BEACON = new ThreadLocal<>();

    @Shadow private static void renderBeaconBeam(PoseStack p_112177_, MultiBufferSource p_112178_,
                                                 float p_112179_, long p_112180_, int p_112181_,
                                                 int p_112182_, float[] p_112183_) { }

    @Inject(
            method = "render(Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD")
    )
    private void onRenderHead(BeaconBlockEntity beacon, float partialTick, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight, int packedOverlay,
                              CallbackInfo ci) {
        CURRENT_BEACON.set(beacon);
    }

    @Inject(
            method = "render(Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("RETURN")
    )
    private void onRenderReturn(BeaconBlockEntity beacon, float partialTick, PoseStack poseStack,
                                MultiBufferSource buffer, int packedLight, int packedOverlay,
                                CallbackInfo ci) {
        CURRENT_BEACON.remove();
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/BeaconRenderer;renderBeaconBeam(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;FJII[F)V"
            )
    )
    private void redirectRenderBeaconBeam(PoseStack poseStack, MultiBufferSource buffer,
                                          float partialTick, long gameTime, int startY,
                                          int height, float[] originalColor) {
        BeaconBlockEntity beacon = CURRENT_BEACON.get();
        if (beacon != null) {
            Item payment = ((IBeaconMixin) beacon).getPaymentItem();
            if (payment instanceof InfestedDiamond || payment instanceof InfestedEmerald) {
                Level level = beacon.getLevel();
                if (level != null) {
                    long gameTime2 = level.getGameTime();
                    long cycle = gameTime2 % 100;
                    float factor = (cycle < 50) ? cycle / 50.0f : (100 - cycle) / 50.0f;
                    float[] customColor = {
                            0.8f + (0.3f - 0.8f) * factor,
                            0.4f + (0.1f - 0.4f) * factor,
                            1.0f + (0.5f - 1.0f) * factor
                    };
                    renderBeaconBeam(poseStack, buffer, partialTick, gameTime, startY, height, customColor);
                    return;
                }
            }
        }
        renderBeaconBeam(poseStack, buffer, partialTick, gameTime, startY, height, originalColor);
    }
}