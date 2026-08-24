package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.YawningNya;
import org.tdddd.epca.impl.epca;

public class YawningNyaRenderer extends HumanoidMobRenderer<YawningNya, PlayerModel<YawningNya>> {

    private static final ResourceLocation YAWNING_NYA_TEXTURE =
            new ResourceLocation(epca.MODID, "textures/entity/yawning_nya.png");

    public YawningNyaRenderer(EntityRendererProvider.Context context) {
        super(context, createPlayerModel(context, true), 0.5F);
    }

    private static PlayerModel<YawningNya> createPlayerModel(EntityRendererProvider.Context context, boolean slim) {
        try {
            return new PlayerModel<>(context.bakeLayer(
                    slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER
            ), slim);
        } catch (Exception e) {
            
            System.err.println("Failed to create player model, using fallback: " + e.getMessage());
            return new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(YawningNya entity) {
        return YAWNING_NYA_TEXTURE;
    }
}