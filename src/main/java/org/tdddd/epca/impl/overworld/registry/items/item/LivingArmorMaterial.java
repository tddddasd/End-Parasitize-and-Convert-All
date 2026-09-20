package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.Map;


public final class LivingArmorMaterial {

    
    public static final TagKey<Item> REPAIR_INGREDIENT =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("epca", "living_armor_repair"));

    
    public static final ResourceKey<EquipmentAsset> ASSET_ID =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("epca", "living_armor"));

    public static final ArmorMaterial MATERIAL = create();

    private static ArmorMaterial create() {
        
        Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
        defense.put(ArmorType.BOOTS, 4);
        defense.put(ArmorType.LEGGINGS, 8);
        defense.put(ArmorType.CHESTPLATE, 10);
        defense.put(ArmorType.HELMET, 4);
        
        
        
        return new ArmorMaterial(
                66,
                defense,
                10,
                SoundEvents.ARMOR_EQUIP_LEATHER,
                3.0F,
                0.0F,
                REPAIR_INGREDIENT,
                ASSET_ID
        );
    }

    private LivingArmorMaterial() {
    }
}
