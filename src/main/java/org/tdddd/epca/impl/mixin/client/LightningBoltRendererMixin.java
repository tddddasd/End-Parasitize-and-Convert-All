package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.PurpleLightningGeometry;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.utils.IPurpleLightningBolt;


@Mixin(LightningBoltRenderer.class)
public class LightningBoltRendererMixin {

    
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LightningBolt;Lnet/minecraft/client/renderer/entity/state/LightningBoltRenderState;F)V",
            at = @At("HEAD")
    )
    private void epca$markPurpleBolt(LightningBolt entity, LightningBoltRenderState state, float partialTick,
                                     CallbackInfo ci) {
        boolean purple = false;
        if (entity != null && state != null) {
            Level level = entity.level();
            if (level != null) {
                
                BlockPos below = entity.blockPosition().below();
                purple = level.getBlockState(below).getBlock() instanceof InfestedBlockInterface;
            }
        }
        ((IPurpleLightningBolt) (Object) state).epca$setPurpleBolt(purple);
    }

    
    @Redirect(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LightningBoltRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V"
            )
    )
    private void epca$submitBolt(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType,
                                 SubmitNodeCollector.CustomGeometryRenderer vanillaGeometry,
                                 LightningBoltRenderState state, PoseStack submitPoseStack,
                                 SubmitNodeCollector submitCollector, CameraRenderState camera) {
        if (!((IPurpleLightningBolt) (Object) state).epca$isPurpleBolt()) {
            
            collector.submitCustomGeometry(poseStack, renderType, vanillaGeometry);
            return;
        }
        
        collector.submitCustomGeometry(poseStack, renderType, new PurpleLightningGeometry(state.seed));
    }
}
