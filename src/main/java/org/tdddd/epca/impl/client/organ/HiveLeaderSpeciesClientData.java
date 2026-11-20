package org.tdddd.epca.impl.client.organ;

import org.tdddd.epca.impl.overworld.data.organ.species.HiveLeaderSpecies;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户端缓存：领巢者当前特化种阶 / 击杀数 / 种阶器官计数 / 模型开关 / 重塑种伤害适应性进度。
 *
 * <p>数据只来自服务端 {@code SyncHiveLeaderSpeciesPacket}，客户端不自行判定。
 * 适应性条目是只读副本（{@code DamageType id → 已适应层数}），用于器官界面读数。</p>
 */
public final class HiveLeaderSpeciesClientData {

    private static String speciesId = HiveLeaderSpecies.INFESTED.id();
    private static int kills;
    private static int infested;
    private static int onesent;
    private static int poverty;
    private static int reshape;
    private static boolean modelVisible;
    private static Map<String, Integer> adaptations = Map.of();

    private HiveLeaderSpeciesClientData() {
    }

    public static void update(String speciesId, int kills, int infested, int onesent,
                              int poverty, int reshape, boolean modelVisible,
                              @Nullable Map<String, Integer> adaptations) {
        HiveLeaderSpeciesClientData.speciesId = speciesId == null ? "" : speciesId;
        HiveLeaderSpeciesClientData.kills = kills;
        HiveLeaderSpeciesClientData.infested = infested;
        HiveLeaderSpeciesClientData.onesent = onesent;
        HiveLeaderSpeciesClientData.poverty = poverty;
        HiveLeaderSpeciesClientData.reshape = reshape;
        HiveLeaderSpeciesClientData.modelVisible = modelVisible;
        HiveLeaderSpeciesClientData.adaptations = adaptations == null
                ? Map.of() : Map.copyOf(new LinkedHashMap<>(adaptations));
    }

    public static void reset() {
        speciesId = HiveLeaderSpecies.INFESTED.id();
        kills = 0;
        infested = 0;
        onesent = 0;
        poverty = 0;
        reshape = 0;
        modelVisible = false;
        adaptations = Map.of();
    }

    public static String speciesId() {
        return speciesId;
    }

    public static HiveLeaderSpecies species() {
        HiveLeaderSpecies species = HiveLeaderSpecies.byId(speciesId);
        return species == null ? HiveLeaderSpecies.INFESTED : species;
    }

    public static int kills() {
        return kills;
    }

    public static int infested() {
        return infested;
    }

    public static int onesent() {
        return onesent;
    }

    public static int poverty() {
        return poverty;
    }

    public static int reshape() {
        return reshape;
    }

    public static boolean modelVisible() {
        return modelVisible;
    }

    /** 重塑种伤害适应性进度（只读副本，按层数从高到低）。 */
    public static Map<String, Integer> adaptations() {
        return adaptations;
    }
}
