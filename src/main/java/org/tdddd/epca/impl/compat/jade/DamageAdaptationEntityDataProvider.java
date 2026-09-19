package org.tdddd.epca.impl.compat.jade;

import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import org.tdddd.yawning_neko_api.data.IAdaptationData;
import org.tdddd.yawning_neko_api.events.CapabilityEventHandler;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * 服务端专用（伤害适应性 / 实体）。
 *
 * <p>这里承载原来的 {@code appendServerData(...)} 逻辑：只往 NBT 里写数据，不做任何渲染，
 * 签名 / 字段 / import 里没有客户端类型，因此可在专用服务器上被 register 加载。
 *
 * <p>NBT 载荷键保持原样（{@code EPCA_DamageAdaptation} 及其子键），客户端读取端见
 * {@link DamageAdaptationEntityComponentProvider}。
 */
public class DamageAdaptationEntityDataProvider implements IServerDataProvider<EntityAccessor> {

    public static final DamageAdaptationEntityDataProvider INSTANCE = new DamageAdaptationEntityDataProvider();

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
        int minKillCount = config.getMinimumKillCount();
        if (minKillCount > 0) {
            epcaData.putInt("MinKillCount", minKillCount);
        }
        int currentAdaptLevel = 0;
        int maxAdaptLevel = config.getMaxAdaptations();
        IAdaptationData adaptationData = livingEntity.getExistingDataOrNull(CapabilityEventHandler.ADAPTATION_DATA);
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
            String lastDamageTypeStr = persistentData.getStringOr("EPCA_LastDamageType", "");
            int lastDamageLevel = 0;
            if (adaptationData != null) {
                Map<String, Integer> allAdaptations = adaptationData.getAllAdaptations();
                lastDamageLevel = allAdaptations.getOrDefault(lastDamageTypeStr, 0);
            }
            epcaData.putString("LastDamageType", lastDamageTypeStr);
            epcaData.putInt("LastDamageAdaptLevel", lastDamageLevel);
        }

        if (!epcaData.isEmpty()) {
            tag.put(EPCAJadeIds.NBT_DAMAGE_ADAPTATION, epcaData);
        }
    }

    @Override
    public Identifier getUid() {
        return EPCAJadeIds.DAMAGE_ADAPTATION_INFO;
    }

    private String formatDamageTypeToString(Object damageType) {
        if (damageType instanceof Identifier) {
            return ((Identifier) damageType).toString();
        } else if (damageType instanceof TagKey) {
            return "#" + ((TagKey<?>) damageType).location().toString();
        }
        return damageType.toString();
    }
}
