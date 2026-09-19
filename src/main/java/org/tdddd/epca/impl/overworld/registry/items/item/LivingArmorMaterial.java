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

/**
 * 26.1.2 起 {@code ArmorMaterial} 不再是可实现的接口，而是一条数据 record
 * （durability 是统一乘数，防御值按 {@link ArmorType} 给，修理材料改成物品标签）。
 * 因此本类从「实现 ArmorMaterial 的材质对象」变成「持有那份 record 的常量类」，
 * 数值尽量与原 1.20.1 实现保持一致，无法表达的部分见 PORTING-NOTES-platform.md。
 */
public final class LivingArmorMaterial {

    /**
     * 修理材料标签。原实现是 {@code Ingredient.of(ModItems.RESHAPE_SHELL.get())}，
     * 26.1.2 的 ArmorMaterial 只接受 {@link TagKey}，
     * 需要 {@code data/epca/tags/item/living_armor_repair.json} 声明 reshape_shell。
     */
    public static final TagKey<Item> REPAIR_INGREDIENT =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("epca", "living_armor_repair"));

    /** 装备贴图资源 key，需要 {@code assets/epca/equipment/living_armor.json}。 */
    public static final ResourceKey<EquipmentAsset> ASSET_ID =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("epca", "living_armor"));

    public static final ArmorMaterial MATERIAL = create();

    private static ArmorMaterial create() {
        // 原 PROTECTION_VALUES = {4(boots), 8(leggings), 10(chest), 4(helmet)}，逐项保留。
        Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
        defense.put(ArmorType.BOOTS, 4);
        defense.put(ArmorType.LEGGINGS, 8);
        defense.put(ArmorType.CHESTPLATE, 10);
        defense.put(ArmorType.HELMET, 4);
        // 原 BASE_DURABILITY = {858(boots), 1056(leggings), 990(chest), 726(helmet)}；
        // 新模型只有统一乘数（HELMET 11 / CHESTPLATE 16 / LEGGINGS 15 / BOOTS 13），
        // 取 66 时 helmet 726、boots 858 完全一致，胸甲/护腿两者互换（±6%）。
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
