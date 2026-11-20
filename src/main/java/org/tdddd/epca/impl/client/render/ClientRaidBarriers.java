package org.tdddd.epca.impl.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.overworld.raid.InfestedRaidBarrierData;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 客户端屏障快照（{@code RaidBarrierS2CPacket} 整表替换）。
 *
 * <p>屏障包是 {@code PacketDistributor.DIMENSION} 目标、只在屏障创建 / 移除时广播，
 * <b>换维度不会自动补发</b>：玩家从主世界进下界后，客户端手里仍然是上一个维度的列表。
 * 服务端在 {@code InfestedRaidManager#onChangedDimension} 里按目标维度单发一份；
 * 客户端这边在换维度时先 {@link #clear()} 再接收，避免旧维度屏障残留。</p>
 *
 * <p>{@link #replace(List)} 会记下这批数据属于哪个维度，所以“服务端补发包先到”和
 * “客户端 tick 后检测到换维度”谁先谁后都不会把刚收到的新数据误清：
 * 包先到则维度已对上，{@link #onDimensionChanged(ResourceLocation)} 直接跳过。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ClientRaidBarriers {
    private static volatile List<InfestedRaidBarrierData.Barrier> barriers = List.of();
    /** 当前这份快照所属的维度；{@code null} = 手上没有数据。 */
    @Nullable
    private static ResourceLocation dimension;

    private ClientRaidBarriers() { }

    public static void replace(List<InfestedRaidBarrierData.Barrier> next) {
        List<InfestedRaidBarrierData.Barrier> copy = List.copyOf(next);
        barriers = copy;
        dimension = copy.isEmpty() ? null : copy.get(0).dimension();
    }

    /** 登出 / 换维度时清空：旧维度的屏障一条都不留。 */
    public static void clear() {
        barriers = List.of();
        dimension = null;
    }

    /**
     * 客户端主线程（{@code ClientTickEvent}，检测到 {@code minecraft.level} 变化）调用：
     * 当前维度与手上的快照维度不一致时清空。{@code current} 为 null（登出过程中）时不动，
     * 登出由 {@link #clear()} 负责。
     */
    public static void onDimensionChanged(@Nullable ResourceLocation current) {
        if (current == null) {
            return;
        }
        if (current.equals(dimension)) {
            return;
        }
        barriers = List.of();
        dimension = current;
    }

    public static List<InfestedRaidBarrierData.Barrier> all() {
        return barriers;
    }

    public static List<InfestedRaidBarrierData.Barrier> activeIn(ResourceLocation dimension) {
        return barriers.stream()
                .filter(b -> b.dimension().equals(dimension) && b.active())
                .toList();
    }
}
