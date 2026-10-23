package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.block.BeckonCore;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.BeckonCoreBlockEntity;
import org.tdddd.epca.impl.overworld.data.EntityBiomassCountManager;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.BioTortIncarnation;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import org.tdddd.yawning_neko_api.data.IAdaptationData;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

import java.util.Map;

@WailaPlugin
public class EPCAJadePlugin implements IWailaPlugin {

    public static final ResourceLocation DAMAGE_ADAPTATION_INFO =
            new ResourceLocation(epca.MODID, "damage_adaptation_info");
    public static final ResourceLocation BIOMASS_COUNT_INFO =
            new ResourceLocation(epca.MODID, "kill_count_info");
    public static final ResourceLocation UNKNOWN_HEALTH_INFO =
            new ResourceLocation(epca.MODID, "unknown_health");

    
    private static final ItemStack ICON_STACK = new ItemStack(ModItems.BIOMASS_COUNT_ICON.get());

    @Override
    public void register(IWailaCommonRegistration registration) {
        
        registration.registerEntityDataProvider(DamageAdaptationEntityProvider.INSTANCE, LivingEntity.class);
        
        registration.registerEntityDataProvider(BiomassCountEntityProvider.INSTANCE, LivingEntity.class);
        
        registration.registerBlockDataProvider(BiomassCountBlockProvider.INSTANCE, BeckonCoreBlockEntity.class);
        registration.registerBlockDataProvider(HiveNodeBiomassCountProvider.INSTANCE,
                org.tdddd.epca.impl.overworld.registry.blocks.block.entity.HiveNodeBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        
        registration.registerEntityComponent(DamageAdaptationEntityProvider.INSTANCE, LivingEntity.class);
        registration.registerEntityComponent(BiomassCountEntityProvider.INSTANCE, LivingEntity.class);

        
        registration.registerBlockComponent(BiomassCountBlockProvider.INSTANCE, BeckonCore.class);
        registration.registerBlockComponent(HiveNodeBiomassCountProvider.INSTANCE,
                org.tdddd.epca.impl.overworld.registry.blocks.block.HiveNode.class);

        // /Unknown
        registration.registerEntityComponent(UnknownHealthEntityProvider.INSTANCE, BioTortIncarnation.class);

        
        // Config keys may already be registered by yawningapi
        try {
            registration.addConfig(BIOMASS_COUNT_INFO, true);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            registration.addConfig(DAMAGE_ADAPTATION_INFO, true);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            registration.addConfig(UNKNOWN_HEALTH_INFO, true);
        } catch (IllegalArgumentException ignored) {
        }
    }

    

    /** Shows the stored biomass of a hive node, with the same icon and format as the beckon core. */
    public static class HiveNodeBiomassCountProvider
            implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        public static final HiveNodeBiomassCountProvider INSTANCE = new HiveNodeBiomassCountProvider();

        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!config.get(BIOMASS_COUNT_INFO)) return;
            CompoundTag serverData = accessor.getServerData();
            if (!serverData.contains("EPCA_BiomassCount")) return;
            int count = serverData.getInt("EPCA_BiomassCount");
            int max = serverData.getInt("EPCA_BiomassCountMax");
            if (count <= 0) return;
            IElementHelper helper = tooltip.getElementHelper();
            tooltip.add(helper.smallItem(ICON_STACK));
            tooltip.append(Component.literal(" " + count + "/" + max));
        }

        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (accessor.getBlockEntity()
                    instanceof org.tdddd.epca.impl.overworld.registry.blocks.block.entity.HiveNodeBlockEntity node) {
                tag.putInt("EPCA_BiomassCount", node.getBiomassCount());
                tag.putInt("EPCA_BiomassCountMax",
                        org.tdddd.epca.impl.overworld.registry.blocks.block.entity.HiveNodeBlockEntity.MAX_BIOMASS_COUNT);
            }
        }

        public ResourceLocation getUid() {
            return BIOMASS_COUNT_INFO;
        }
    }

    public static class BiomassCountEntityProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
        public static final BiomassCountEntityProvider INSTANCE = new BiomassCountEntityProvider();

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!config.get(BIOMASS_COUNT_INFO)) return;
            CompoundTag serverData = accessor.getServerData();
            if (!serverData.contains("EPCA_BiomassCount")) return;
            int count = serverData.getInt("EPCA_BiomassCount");
            if (count <= 0) return;

            IElementHelper helper = tooltip.getElementHelper();
            
            tooltip.add(helper.smallItem(ICON_STACK));
            tooltip.append(Component.literal(" " + count));
        }

        @Override
        public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
            Entity entity = accessor.getEntity();
            if (!(entity instanceof LivingEntity living)) return;
            int count = EntityBiomassCountManager.getCurrentBiomassCount(living);
            if (count > 0) {
                tag.putInt("EPCA_BiomassCount", count);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return BIOMASS_COUNT_INFO;
        }
    }

    
    public static class BiomassCountBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        public static final BiomassCountBlockProvider INSTANCE = new BiomassCountBlockProvider();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!config.get(BIOMASS_COUNT_INFO)) return;
            CompoundTag serverData = accessor.getServerData();
            if (!serverData.contains("EPCA_BiomassCount")) return;
            int count = serverData.getInt("EPCA_BiomassCount");
            int max = serverData.getInt("EPCA_BiomassCountMax");
            if (count <= 0) return;

            IElementHelper helper = tooltip.getElementHelper();
            tooltip.add(helper.smallItem(ICON_STACK));
            tooltip.append(Component.literal(" " + count + "/" + max));
        }

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            BlockEntity be = accessor.getBlockEntity();
            if (be instanceof BeckonCoreBlockEntity core) {
                int count = core.getBiomassCount();
                if (count > 0) {
                    tag.putInt("EPCA_BiomassCount", count);
                    tag.putInt("EPCA_BiomassCountMax", BeckonCoreBlockEntity.MAX_BIOMASS_COUNT);
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return BIOMASS_COUNT_INFO;
        }
    }

    /**
     *  Jade en_us: "Unknown"
     *
     * <p>{@code setHealth}/{@code setMaxHealth}
     * {@code getHealth()}/{@code getMaxHealth()}
     *  {@code jade.epca.unknown_health} </p>
     */
    public static class UnknownHealthEntityProvider implements IEntityComponentProvider {
        public static final UnknownHealthEntityProvider INSTANCE = new UnknownHealthEntityProvider();

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!config.get(UNKNOWN_HEALTH_INFO)) return;
            Entity entity = accessor.getEntity();
            if (!(entity instanceof BioTortIncarnation incarnation)) return;

            tooltip.add(Component.translatable("jade.epca.unknown_health"));
            //  2 " 2"
            if (incarnation.isPhaseTwo()) {
                tooltip.add(Component.translatable("entity.epca.bio_tort_incarnation.phase_two"));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UNKNOWN_HEALTH_INFO;
        }
    }

    public static class DamageAdaptationEntityProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {

        public static final DamageAdaptationEntityProvider INSTANCE = new DamageAdaptationEntityProvider();

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!config.get(DAMAGE_ADAPTATION_INFO)) return;

            Entity entity = accessor.getEntity();
            if (!(entity instanceof LivingEntity livingEntity)) return;

            CompoundTag serverData = accessor.getServerData();
            if (!serverData.contains("EPCA_DamageAdaptation")) return;

            CompoundTag data = serverData.getCompound("EPCA_DamageAdaptation");
            IElementHelper helper = tooltip.getElementHelper();

            if (data.contains("MaxDamageType")) {
                String damageType = data.getString("MaxDamageType");
                float multiplier = data.getFloat("MaxMultiplier");
                String formattedDamageType = formatDamageType(damageType);
                String formattedMultiplier = String.format("×%.1f", multiplier);
                tooltip.add(Component.literal("最大受击伤害类型倍率:")
                        .append(Component.literal(formattedDamageType).withStyle(net.minecraft.ChatFormatting.WHITE))
                        .append(Component.literal(formattedMultiplier)));
            }

            if (data.contains("MinBiomassCount")) {
                int minBiomassCount = data.getInt("MinBiomassCount");
                int maxMinBiomassCount = data.contains("MaxMinBiomassCount")
                        ? data.getInt("MaxMinBiomassCount") : minBiomassCount;
                if (false) { // minimum kill count removed
                    // A JSON value of n > 0 means the creature needs 1 + n kills, so it survives n
                    // fake deaths. The tooltip shows how many are still left.
                    int shown = minBiomassCount;
                    int deaths = data.contains("BiomassDeaths") ? data.getInt("BiomassDeaths") : 0;
                    int remaining = Math.max(shown - deaths, 0);
                    tooltip.add(tooltip.getElementHelper().smallItem(
                            new ItemStack(net.minecraft.world.item.Items.RESPAWN_ANCHOR)));
                    tooltip.append(Component.literal(" " + remaining + "/" + shown));
                }
            }

            if (data.contains("BrokenAdaptationTime")) {
                float remainingTime = data.getFloat("BrokenAdaptationTime");
                if (remainingTime > 0) {
                    tooltip.add(Component.literal("破适应性: " + String.format("%.1f秒", remainingTime)));
                }
            }

            if (data.contains("CurrentAdaptLevel") && data.contains("MaxAdaptLevel")) {
                int current = data.getInt("CurrentAdaptLevel");
                int max = data.getInt("MaxAdaptLevel");
                if (max > 0) {
                    String color = current >= max ? "§d" : "§a";
                    tooltip.add(Component.literal("适应性等级: ")
                            .append(Component.literal(color + current + "/" + max)));
                }
            }

            if (data.contains("LastDamageType") && data.contains("LastDamageAdaptLevel")) {
                String lastDamageType = data.getString("LastDamageType");
                int lastLevel = data.getInt("LastDamageAdaptLevel");
                int maxAdapt = data.contains("MaxAdaptLevel") ? data.getInt("MaxAdaptLevel") : 0;
                String formattedType = formatDamageType(lastDamageType);
                String levelColor = lastLevel >= maxAdapt ? "§d" : "§a";
                tooltip.add(Component.literal("上次伤害类型: ")
                        .append(Component.literal(formattedType).withStyle(net.minecraft.ChatFormatting.WHITE))
                        .append(Component.literal(" 适应等级: "))
                        .append(Component.literal(levelColor + lastLevel + "/" + maxAdapt)));
            }
        }

        @Override
        public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
            Entity entity = accessor.getEntity();
            if (!(entity instanceof LivingEntity livingEntity)) return;

            DamageAdaptationConfig config = DamageAdaptation.getEntityConfig(livingEntity);
            if (config == null) return;

            CompoundTag epcaData = new CompoundTag();

            
            Map<Object, Float> multipliers = config.getDamageMultipliers();
            Object maxDamageType = null;
            float maxMultiplier = 0;
            float maxMultiplierAbs = 0;
            for (Map.Entry<Object, Float> entry : multipliers.entrySet()) {
                float multiplier = entry.getValue();
                float absMultiplier = Math.abs(multiplier);
                if (absMultiplier > maxMultiplierAbs) {
                    maxMultiplierAbs = absMultiplier;
                    maxMultiplier = multiplier;
                    maxDamageType = entry.getKey();
                }
            }
            if (maxDamageType != null) {
                epcaData.putString("MaxDamageType", formatDamageTypeToString(maxDamageType));
                epcaData.putFloat("MaxMultiplier", maxMultiplier);
            }

            
            int minBiomassCount = config.getMinimumKillCount();
            if (false) { // minimum kill count removed
                epcaData.putInt("MinBiomassCount", minBiomassCount);
                epcaData.putInt("BiomassDeaths", DamageAdaptation.getDeathCount(livingEntity));
                epcaData.putInt("MaxMinBiomassCount",
                        org.tdddd.yawning_neko_api.data.AdaptationConfigResolver
                                .resolveMinimumKillCountUpperBound(livingEntity));
            }

            
            int currentAdaptLevel = 0;
            int maxAdaptLevel = config.getMaxAdaptations();
            IAdaptationData adaptationData = livingEntity.getCapability(IAdaptationData.CAPABILITY).orElse(null);
            if (adaptationData != null) {
                currentAdaptLevel = adaptationData.getAllAdaptations().values().stream()
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(0);
            }
            if (maxAdaptLevel > 0) {
                epcaData.putInt("CurrentAdaptLevel", currentAdaptLevel);
                epcaData.putInt("MaxAdaptLevel", maxAdaptLevel);
            }

            
            if (DamageAdaptation.isInBrokenAdaptation(livingEntity)) {
                float remainingTime = DamageAdaptation.getRemainingBrokenAdaptationTime(livingEntity);
                epcaData.putFloat("BrokenAdaptationTime", remainingTime);
            }

            
            CompoundTag persistentData = livingEntity.getPersistentData();
            if (persistentData.contains("EPCA_LastDamageType")) {
                String lastDamageTypeStr = persistentData.getString("EPCA_LastDamageType");
                int lastDamageLevel = 0;
                if (adaptationData != null) {
                    Map<String, Integer> allAdaptations = adaptationData.getAllAdaptations();
                    lastDamageLevel = allAdaptations.getOrDefault(lastDamageTypeStr, 0);
                }
                epcaData.putString("LastDamageType", lastDamageTypeStr);
                epcaData.putInt("LastDamageAdaptLevel", lastDamageLevel);
            }

            if (!epcaData.isEmpty()) {
                tag.put("EPCA_DamageAdaptation", epcaData);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return DAMAGE_ADAPTATION_INFO;
        }

        private String formatDamageTypeToString(Object damageType) {
            if (damageType instanceof ResourceLocation) {
                return ((ResourceLocation) damageType).toString();
            } else if (damageType instanceof TagKey) {
                return "#" + ((TagKey<?>) damageType).location().toString();
            }
            return damageType.toString();
        }
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