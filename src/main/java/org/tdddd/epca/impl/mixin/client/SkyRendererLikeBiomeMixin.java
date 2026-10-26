package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.tdddd.epca.impl.client.LikeBiomeClientEffects;

/**
 * Lets a like biome recolour the sky.
 *
 * <p>Verified against the 26.1.2 client jar: the sky disc is drawn by
 * {@code SkyRenderer.renderSkyDisc(int)}, whose single argument is the sky colour packed as RGB. Replacing
 * that argument is the narrowest hook, and it only touches the disc, not the clouds or the sun/moon.</p>
 */
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
