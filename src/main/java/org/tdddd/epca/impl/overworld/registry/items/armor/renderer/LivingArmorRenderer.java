package org.tdddd.epca.impl.overworld.registry.items.armor.renderer;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for {@link LivingArmorItem}.
 *
 * <h2>GeckoLib 4 → 5.5.2</h2>
 * <ul>
 *   <li>{@code GeoArmorRenderer} has <b>two</b> type parameters now
 *       ({@code T} the item, {@code R} the humanoid render state); the vanilla
 *       {@link HumanoidRenderState} is the correct choice here.</li>
 *   <li>{@code getTextureLocation(T animatable)} became
 *       {@code getTextureLocation(R renderState)} — the animatable is no longer reachable when
 *       the renderer picks a texture, so the render state is what arrives.</li>
 *   <li>{@code applyBoneVisibilityBySlot(EquipmentSlot)} and the
 *       {@code setAllBonesVisible}/{@code setBoneVisible}/{@code this.head} family were removed
 *       along with mutable bones. <b>No port is needed</b>: GeckoLib 5's own
 *       {@code getSegmentsForSlot} + {@code getBoneNameForSegment} defaults implement exactly the
 *       mapping the old override hard-coded (HEAD→{@code armorHead},
 *       CHEST→{@code armorBody}/{@code armorLeftArm}/{@code armorRightArm},
 *       LEGS→{@code armorLeftLeg}/{@code armorRightLeg},
 *       FEET→{@code armorLeftBoot}/{@code armorRightBoot}).</li>
 * </ul>
 */
public class LivingArmorRenderer extends GeoArmorRenderer<LivingArmorItem, HumanoidRenderState> {

    public LivingArmorRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(epca.MODID, "armor/living_armor")));
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "textures/armor/living_armor.png");
    }
}
