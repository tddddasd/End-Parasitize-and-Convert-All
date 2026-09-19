package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import org.tdddd.epca.impl.overworld.registry.items.armor.renderer.LivingArmorRenderer;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Living armor item.
 *
 * <h2>26.1.2: {@code ArmorItem} no longer exists</h2>
 * <p>Armor became data-driven. {@code net.minecraft.world.item.ArmorItem} is gone; the armour
 * behaviour is attached to a plain {@link Item} through
 * {@code Item.Properties#humanoidArmor(ArmorMaterial, ArmorType)} (verified at
 * {@code Item.java:579}). The constructor therefore takes the material and the
 * {@link ArmorType} and applies that property itself, which keeps the platform owner's call site
 * {@code new LivingArmorItem(material, type, properties)} unchanged.</p>
 *
 * <h2>GeckoLib 4 → 5.5.2</h2>
 * <p>GeckoLib 4 supplied the armour model through NeoForge's
 * {@code IClientItemExtensions#getHumanoidArmorModel}, returning the {@code GeoArmorRenderer}
 * itself (it used to be a {@code HumanoidModel}). In GeckoLib 5 {@code GeoArmorRenderer} is a
 * renderer, not a model, and the supported hook is
 * {@link GeoItem#createGeoRenderer(Consumer)} + {@link GeoRenderProvider#getGeoArmorRenderer};
 * GeckoLib's {@code HumanoidArmorLayerMixin} calls it. That is what this class now implements.</p>
 */
public final class LivingArmorItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LivingArmorItem(ArmorMaterial material, ArmorType type, Properties properties) {
        super(properties.humanoidArmor(material, type));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private LivingArmorRenderer renderer;

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack itemStack, EquipmentSlot equipmentSlot) {
                if (this.renderer == null) {
                    this.renderer = new LivingArmorRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // no controllers: the living armor is a static model
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        // living armor is never damaged (the old override of ArmorItem#isDamageable)
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        // living armor is never damaged, so it never shows a durability bar
        return false;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        entity.discard();
        return true;
    }
}
