package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.BioTortIncarnation;
import org.tdddd.epca.impl.epca;

/**
 *  {@code YawningNyaRenderer}
 * {@code ModelLayers.PLAYER_SLIM}
 *
 * <p> {@code assets/epca/textures/entity/bio-tort_incarnation.png}
 *
 *  Yawning_Nya  {@code textures/entity/yawning_nya.png}
 * </p>
 */
public class BioTortIncarnationRenderer extends HumanoidMobRenderer<BioTortIncarnation, PlayerModel<BioTortIncarnation>> {

    /** assets/epca/textures/entity/bio-tort_incarnation.png */
    private static final ResourceLocation BIO_TORT_TEXTURE =
            new ResourceLocation(epca.MODID, "textures/entity/bio-tort_incarnation.png");

    /**  Yawning_Nya  */
    private static final ResourceLocation PLACEHOLDER_TEXTURE =
            new ResourceLocation(epca.MODID, "textures/entity/yawning_nya.png");

    public BioTortIncarnationRenderer(EntityRendererProvider.Context context) {
        super(context, createPlayerModel(context, true), 0.5F);
    }

    private static PlayerModel<BioTortIncarnation> createPlayerModel(EntityRendererProvider.Context context, boolean slim) {
        try {
            return new PlayerModel<>(context.bakeLayer(
                    slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER
            ), slim);
        } catch (Exception e) {
            //  YawningNyaRenderer
            System.err.println("Failed to create player model, using fallback: " + e.getMessage());
            return new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(BioTortIncarnation entity) {
        try {
            //  Yawning_Nya
            if (Minecraft.getInstance().getResourceManager().getResource(BIO_TORT_TEXTURE).isPresent()) {
                return BIO_TORT_TEXTURE;
            }
        } catch (Exception ignored) {
        }
        return PLACEHOLDER_TEXTURE;
    }
}

