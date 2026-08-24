package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.ContaminatedWater;

public class ContaminatedWaterRenderer extends EntityRenderer<ContaminatedWater> {
    public ContaminatedWaterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    
    @Override
    public ResourceLocation getTextureLocation(ContaminatedWater entity) {
        return null; 
    }

    
    
    public void render(ContaminatedWater entity, float yaw, float partialTicks, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
    }
}