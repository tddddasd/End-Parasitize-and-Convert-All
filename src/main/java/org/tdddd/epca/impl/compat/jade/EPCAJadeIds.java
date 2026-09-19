package org.tdddd.epca.impl.compat.jade;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;

/**
 * 中立常量：服务端注册路径与客户端注册路径共用。
 *
 * <p>26.1.2 说明：Jade 会在<strong>专用服务器</strong>上实例化每一个 {@code @WailaPlugin} 类并调用
 * {@code IWailaPlugin#register(IWailaCommonRegistration)}（{@code @WailaPlugin} 没有 dist 过滤，
 * 这是 Jade 的设计）。因此 UID / 配置键 / NBT 载荷键必须放在一个完全不依赖客户端类型
 * （{@code net.minecraft.client.*}）的类里，供 register 与 registerClient 两侧引用。
 *
 * <p>本类只有纯常量，没有任何静态初始化器去访问注册表或物品。
 *
 * <p>这些取值必须保持字节级不变：UID 是 Jade 用来把服务端数据提供者与客户端渲染提供者
 * 配对的键，NBT 键是跨越客户端 ↔ 服务端的数据载荷键。
 */
public final class EPCAJadeIds {

    /** 伤害适应性信息的 UID（同时也是 Jade 配置键）。 */
    public static final Identifier DAMAGE_ADAPTATION_INFO =
            Identifier.fromNamespaceAndPath(epca.MODID, "damage_adaptation_info");

    /** 击杀数信息的 UID（同时也是 Jade 配置键）。 */
    public static final Identifier KILL_COUNT_INFO =
            Identifier.fromNamespaceAndPath(epca.MODID, "kill_count_info");

    /** 服务端 → 客户端的击杀数载荷键。 */
    public static final String NBT_KILL_COUNT = "EPCA_KillCount";

    /** 服务端 → 客户端的伤害适应性载荷键。 */
    public static final String NBT_DAMAGE_ADAPTATION = "EPCA_DamageAdaptation";

    private EPCAJadeIds() {
    }
}
