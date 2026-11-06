package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.YawningNya;
import org.tdddd.epca.impl.epca;

public class YawningNyaRenderer extends HumanoidMobRenderer<YawningNya, AvatarRenderState, PlayerModel> {

    private static final Identifier YAWNING_NYA_TEXTURE =
            Identifier.fromNamespaceAndPath(epca.MODID, "textures/entity/yawning_nya.png");

    public YawningNyaRenderer(EntityRendererProvider.Context context) {
        super(context, createPlayerModel(context, true), 0.5F);
    }

    private static PlayerModel createPlayerModel(EntityRendererProvider.Context context, boolean slim) {
        try {
            return new PlayerModel(context.bakeLayer(
                    slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER
            ), slim);
        } catch (Exception e) {

            System.err.println("Failed to create player model, using fallback: " + e.getMessage());
            return new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false);
        }
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return YAWNING_NYA_TEXTURE;
    }
}
