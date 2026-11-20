package org.tdddd.epca.impl.compat.jade;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;

/**
 * EPCA 的 Jade provider id（同时是 Jade 配置项的 key）。
 *
 * <h2>为什么不是所有 provider 共用一个 id</h2>
 * <p>Jade 的 {@code HierarchyLookup#register} 用 <b>UID 作为 byKey 的 key</b>，同一个注册表里出现
 * 两个不同 provider 实例用同一个 UID 时会打警告：</p>
 * <pre>
 * Found different provider instances with same id epca:kill_count_info, this may cause issues:
 *     ...HiveNodeBiomassCountDataProvider@... and ...BiomassCountBlockDataProvider@...
 * </pre>
 * <p>（26.1.11 实测：方块数据提供器列表里两个 provider 同用 {@code kill_count_info} 触发了这条警告。）
 * 因此每个“注册表里的一个 entry”都拿一个独立的 id：数据提供器的 id 只是同步/优先级用的 key，
 * 方块 tooltip 组件虽然换了 id，配置开关仍然统一读 {@link #BIOMASS_COUNT_INFO}
 * —— 玩家在 Jade 配置里看到的还是一个“击杀计数信息”开关。</p>
 */
public final class EPCAJadeIds {

    /** 实体上的“击杀计数”条目（配置项 {@code config.jade.plugin_epca.kill_count_info} 的 key）。 */
    public static final Identifier BIOMASS_COUNT_INFO =
            Identifier.fromNamespaceAndPath(epca.MODID, "kill_count_info");

    /**
     * 巢节点（HiveNode）方块实体的服务端数据 id。
     *
     * <p>与 {@link #BIOMASS_COUNT_INFO} 分开：{@code HiveNodeBiomassCountDataProvider} 和
     * {@code BiomassCountBlockDataProvider} 是两个不同实例、且写入的 NBT 语义不同
     * （巢节点恒定写入当前/上限，召唤柱只在上限大于 0 时写入），共用 id 会触发 Jade 的重复 id 警告。</p>
     */
    public static final Identifier HIVE_NODE_BIOMASS_COUNT_DATA =
            Identifier.fromNamespaceAndPath(epca.MODID, "hive_node_biomass_count_data");

    /** 召唤柱（BeckonCore）方块实体的服务端数据 id，与巢节点分开的理由见上。 */
    public static final Identifier BIOMASS_COUNT_BLOCK_DATA =
            Identifier.fromNamespaceAndPath(epca.MODID, "biomass_count_block_data");

    /**
     * 巢节点方块的 tooltip 组件 id。
     *
     * <p>只用于在 Jade 里区分注册项（避免与召唤柱的组件同 id）；开关仍然读
     * {@link #BIOMASS_COUNT_INFO}，所以玩家侧只有一个“击杀计数信息”选项。</p>
     */
    public static final Identifier HIVE_NODE_BIOMASS_COUNT =
            Identifier.fromNamespaceAndPath(epca.MODID, "hive_node_biomass_count");

    /** 召唤柱方块的 tooltip 组件 id；开关同样读 {@link #BIOMASS_COUNT_INFO}。 */
    public static final Identifier BIOMASS_COUNT_BLOCK =
            Identifier.fromNamespaceAndPath(epca.MODID, "biomass_count_block");

    /** 伤害适应信息（实体数据 + 实体组件，分属两个注册表，因此可以共用一个 id）。 */
    public static final Identifier DAMAGE_ADAPTATION_INFO =
            Identifier.fromNamespaceAndPath(epca.MODID, "damage_adaptation_info");

    public static final String NBT_BIOMASS_COUNT_MAX = "EPCA_BiomassCountMax";
    public static final String NBT_BIOMASS_COUNT = "EPCA_BiomassCount";

    public static final String NBT_DAMAGE_ADAPTATION = "EPCA_DamageAdaptation";

    private EPCAJadeIds() {
    }
}
