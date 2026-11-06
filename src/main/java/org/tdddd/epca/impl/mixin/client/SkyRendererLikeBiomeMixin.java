package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.tdddd.epca.impl.client.LikeBiomeClientEffects;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererLikeBiomeMixin {

    @ModifyVariable(method = "renderSkyDisc", at = @At("HEAD"), argsOnly = true, require = 0)
    private int epca$likeBiomeSky(int color) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return color;
        }
        Integer like = LikeBiomeClientEffects.skyColor(minecraft.player.blockPosition());
        return like != null ? like : color;
    }
}
