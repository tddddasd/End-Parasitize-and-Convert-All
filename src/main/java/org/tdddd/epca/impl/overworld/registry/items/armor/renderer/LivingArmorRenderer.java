package org.tdddd.epca.impl.overworld.registry.items.armor.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class LivingArmorRenderer extends GeoArmorRenderer<LivingArmorItem> {
    public LivingArmorRenderer() {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(epca.MODID, "armor/living_armor")));
    }

    @Override
    public ResourceLocation getTextureLocation(LivingArmorItem animatable) {
        return new ResourceLocation(epca.MODID, "textures/armor/living_armor.png");
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        setAllBonesVisible(false);

        switch (currentSlot) {
            case HEAD -> {
                setBoneVisible(this.head, true);
                getGeoModel().getBone("armorHead").ifPresent(bone -> bone.setHidden(false));
            }
            case CHEST -> {
                setBoneVisible(this.body, true);
                setBoneVisible(this.rightArm, true);
                setBoneVisible(this.leftArm, true);
                getGeoModel().getBone("armorBody").ifPresent(bone -> bone.setHidden(false));
                getGeoModel().getBone("armorRightArm").ifPresent(bone -> bone.setHidden(false));
                getGeoModel().getBone("armorLeftArm").ifPresent(bone -> bone.setHidden(false));
            }
            case LEGS -> {
                setBoneVisible(this.rightLeg, true);
                setBoneVisible(this.leftLeg, true);
                getGeoModel().getBone("armorRightLeg").ifPresent(bone -> bone.setHidden(false));
                getGeoModel().getBone("armorLeftLeg").ifPresent(bone -> bone.setHidden(false));
            }
            case FEET -> {
                setBoneVisible(this.rightBoot, true);
                setBoneVisible(this.leftBoot, true);
                getGeoModel().getBone("armorRightBoot").ifPresent(bone -> bone.setHidden(false));
                getGeoModel().getBone("armorLeftBoot").ifPresent(bone -> bone.setHidden(false));
            }
        }
    }
}