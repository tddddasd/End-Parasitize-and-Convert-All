package org.tdddd.epca.impl.client.render;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.overworld.raid.InfestedRaidBarrierData;

import java.util.List;

/**
 * 客户端手里的“活动屏障”快照（由 {@code RaidBarrierS2CPacket} 整表替换）。
 *
 * <p>0.148n 修复：{@code RaidBarrierS2CPacket} 是 DIMENSION 目标、只在屏障<b>创建 / 移除</b>时广播，
 * 换维度不会补发 → 走进另一个维度后本地列表里只有旧维度的条目，新维度一个屏障都画不出来。
 * 服务端现在在 {@code PlayerChangedDimensionEvent} 里给该玩家补发目标维度的活动屏障
 * （{@code InfestedRaidManager#onChangedDimension} → {@code RaidBarrierS2CPacket#sendAllTo}）。</p>
 *
 * <p>客户端侧同时加了 {@link #clear()}：登出时整表清空，避免跨服务器残留；
 * 换维度时调用 {@link #clearOtherDimensions(Identifier)} 只丢掉旧维度的条目。
 * 之所以换维度时不直接 {@code clear()} 整表：补发包是网络线程/主线程异步投递的，
 * 完全可能<b>先到</b>于客户端的维度切换被观测到 —— 那时整表清空会把刚收到的
 * <b>新维度</b>屏障一起抹掉，然后要等下一次屏障创建才会有包，屏障就一直不显示。
 * 按维度过滤既满足“换维度先清空旧数据”，又不依赖事件先后顺序。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ClientRaidBarriers {
    private static volatile List<InfestedRaidBarrierData.Barrier> barriers = List.of();

    private ClientRaidBarriers() { }

    public static void replace(List<InfestedRaidBarrierData.Barrier> next) {
        barriers = List.copyOf(next);
    }

    /** 登出 / 断开连接时整表清空（跨服务器残留修复）。 */
    public static void clear() {
        barriers = List.of();
    }

    /**
     * 换维度时清掉<b>不属于</b> {@code dimension} 的条目（旧维度的屏障）。
     *
     * <p>只保留目标维度的条目，这样即使服务端的补发包比客户端的维度切换先到，
     * 也不会被这次清理误删。{@code dimension} 为 null（已回到主菜单）时等价于 {@link #clear()}。</p>
     */
    public static void clearOtherDimensions(Identifier dimension) {
        if (dimension == null) {
            clear();
            return;
        }
        barriers = barriers.stream()
                .filter(b -> dimension.equals(b.dimension()))
                .toList();
    }

    public static List<InfestedRaidBarrierData.Barrier> all() {
        return barriers;
    }

    public static List<InfestedRaidBarrierData.Barrier> activeIn(Identifier dimension) {
        long now = System.currentTimeMillis();
        return barriers.stream()
                .filter(b -> b.dimension().equals(dimension) && b.active())
                .toList();
    }
}
