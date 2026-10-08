package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSpider;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model for both infested spider variants.
 *
 * <p>The three variants share the single {@code infested_spider} model and animation file and only
 * differ in the texture, which the entity itself resolves through
 * {@link InfestedSpider#getTextureResource()}. This is the same shape as
 * {@code InfestedFoxModel} / {@code RipperModel}, the mod's other variant mobs.</p>
 */
public class InfestedSpiderModel extends GeoModel<InfestedSpider> {

    /** Parent bone of all eight leg pairs in {@code infested_spider.geo.json}. */
    private static final String LEG_BONE = "legs";
    /** 1 pixel down. GeckoLib model units are pixels and +Y points up, so the drop is negative. */
    private static final float CAVE_SHOOT_LEG_OFFSET_Y = -1.0F;

    @Override
    public ResourceLocation getModelResource(InfestedSpider entity) {
        return new ResourceLocation("epca", "geo/entity/infested_spider.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(InfestedSpider entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(InfestedSpider entity) {
        return new ResourceLocation("epca", "animations/infested_spider.animation.json");
    }

    /**
     * User requirement: while the CAVE variant of the infested spider is FIRING a projectile, its leg
     * model drops by 1 pixel.
     *
     * <p>1 pixel is 1 model unit (16 units = 1 block) and GeckoLib's {@code +Y} points up, so the
     * drop is {@code setPosY(-1)}. This lives here rather than in the supplied animation file because
     * that file has only a plain {@code shoot} clip and no {@code shoot_cave} - the requirement is
     * variant-specific, which a shared clip cannot express.</p>
     *
     * <p>Bone: <b>{@code legs}</b>, the parent of all eight leg pairs in
     * {@code infested_spider.geo.json} (parent {@code Infested_Spider}). Offsetting that one parent
     * translates every leg by the same pixel and cannot make them drift apart, which is why it is
     * preferred over nudging the sixteen individual {@code *_leg} / {@code *_leg1} bones.</p>
     *
     * <p>The offset tracks the shooting window only. When the spit ends, the bone returns to the
     * clip's own value, because GeckoLib resets bone transforms from the animation each frame.</p>
     */
    @Override
    public void setCustomAnimations(InfestedSpider animatable, long instanceId,
                                    AnimationState<InfestedSpider> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        if (animatable.getVariant() != InfestedSpider.Variant.CAVE || !animatable.isShooting()) {
            return;
        }
        CoreGeoBone legs = this.getAnimationProcessor().getBone(LEG_BONE);
        if (legs != null) {
            legs.setPosY(CAVE_SHOOT_LEG_OFFSET_Y);
        }
    }
}
