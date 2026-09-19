package org.tdddd.epca.impl.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 客户端专用（伤害适应性 / 实体）。
 *
 * <p>这里承载原来的 {@code appendTooltip(...)} 渲染逻辑与伤害类型格式化；只实现
 * {@link IEntityComponentProvider}，且只在 {@code registerClient(...)} 里被引用。
 *
 * <p>NBT 键与 UID 保持不变，读取的是 {@link DamageAdaptationEntityDataProvider} 写入的载荷。
 */
public class DamageAdaptationEntityComponentProvider implements IEntityComponentProvider {

    public static final DamageAdaptationEntityComponentProvider INSTANCE =
            new DamageAdaptationEntityComponentProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!config.get(EPCAJadeIds.DAMAGE_ADAPTATION_INFO)) return;

        Entity entity = accessor.getEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;

        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains(EPCAJadeIds.NBT_DAMAGE_ADAPTATION)) return;

        CompoundTag data = serverData.getCompoundOrEmpty(EPCAJadeIds.NBT_DAMAGE_ADAPTATION);

        if (data.getString("MaxDamageType").isPresent()) {
            String damageType = data.getStringOr("MaxDamageType", "");
            float multiplier = data.getFloatOr("MaxMultiplier", 0.0F);
            String formattedDamageType = formatDamageType(damageType);
            String formattedMultiplier = String.format("×%.1f", multiplier);
            tooltip.add(Component.literal("最大受击伤害类型倍率:")
                    .append(Component.literal(formattedDamageType).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(formattedMultiplier)));
        }

        if (data.getInt("MinKillCount").isPresent()) {
            int minKillCount = data.getIntOr("MinKillCount", 0);
            if (minKillCount > 0) {
                tooltip.add(Component.literal("最小击杀数: " + minKillCount));
            }
        }

        if (data.read("BrokenAdaptationTime", com.mojang.serialization.Codec.FLOAT).isPresent()) {
            float remainingTime = data.getFloatOr("BrokenAdaptationTime", 0.0F);
            if (remainingTime > 0) {
                tooltip.add(Component.literal("破适应性: " + String.format("%.1f秒", remainingTime)));
            }
        }

        if (data.getInt("CurrentAdaptLevel").isPresent() && data.getInt("MaxAdaptLevel").isPresent()) {
            int current = data.getIntOr("CurrentAdaptLevel", 0);
            int max = data.getIntOr("MaxAdaptLevel", 0);
            if (max > 0) {
                String color = current >= max ? "§d" : "§a";
                tooltip.add(Component.literal("适应性等级: ")
                        .append(Component.literal(color + current + "/" + max)));
            }
        }

        if (data.getString("LastDamageType").isPresent() && data.getInt("LastDamageAdaptLevel").isPresent()) {
            String lastDamageType = data.getStringOr("LastDamageType", "");
            int lastLevel = data.getIntOr("LastDamageAdaptLevel", 0);
            int maxAdapt = data.getInt("MaxAdaptLevel").isPresent() ? data.getIntOr("MaxAdaptLevel", 0) : 0;
            String formattedType = formatDamageType(lastDamageType);
            String levelColor = lastLevel >= maxAdapt ? "§d" : "§a";
            tooltip.add(Component.literal("上次伤害类型: ")
                    .append(Component.literal(formattedType).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" 适应等级: "))
                    .append(Component.literal(levelColor + lastLevel + "/" + maxAdapt)));
        }
    }

    @Override
    public Identifier getUid() {
        return EPCAJadeIds.DAMAGE_ADAPTATION_INFO;
    }

    private static String formatDamageType(String damageTypeStr) {
        if (damageTypeStr.startsWith("minecraft:")) {
            String path = damageTypeStr.substring(10);
            String[] words = path.split("_");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    result.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1))
                            .append(" ");
                }
            }
            return result.toString().trim();
        } else if (damageTypeStr.startsWith("#")) {
            String tagPath = damageTypeStr.substring(1);
            if (tagPath.startsWith("minecraft:")) {
                String path = tagPath.substring(10);
                return "Tag: " + path.replace("_", " ");
            }
            return "Tag: " + tagPath;
        }
        return damageTypeStr;
    }
}
