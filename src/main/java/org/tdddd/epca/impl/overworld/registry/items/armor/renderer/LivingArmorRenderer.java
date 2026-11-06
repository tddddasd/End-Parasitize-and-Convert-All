package org.tdddd.epca.impl.overworld.registry.items.armor.renderer;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;

public class LivingArmorRenderer extends GeoArmorRenderer<LivingArmorItem, HumanoidRenderState> {

    public LivingArmorRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(epca.MODID, "armor/living_armor")));
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "textures/armor/living_armor.png");
    }
}
