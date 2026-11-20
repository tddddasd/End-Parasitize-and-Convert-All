package org.tdddd.epca.impl.client.organ.model;

import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 四肢内部器官的「外部显示」扩展点。
 *
 * <p>需求只要求预留接口：四肢（左臂 / 右臂 / 左腿 / 右腿）的内器官将来可以通过
 * 这个 Provider 追加 / 移除玩家人体模型上的部件或附加物品模型。当前没有任何
 * Provider 注册，{@link #extraParts} 默认返回空列表，因此四肢内器官目前不会改变玩家外观。</p>
 */
public final class OrganLimbModelExtension {

    /**
     * 一个附加显示部件：目前用「物品模型」表示，坐标是所在肢体 ModelPart 的本地单位（1/16 方块）。
     */
    public record ExtraPart(ItemStack stack, float x, float y, float z,
                            float scale, float yawDegrees, float pitchDegrees) {
    }

    /** 四肢外部显示提供者。返回空列表表示该肢体没有额外部件。 */
    public interface LimbPartProvider {

        List<ExtraPart> partsFor(Object player, OrganSlotGroup limbGroup, List<ItemStack> stacks);
    }

    private static final List<LimbPartProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private OrganLimbModelExtension() {
    }

    public static void register(@Nullable LimbPartProvider provider) {
        if (provider != null && !PROVIDERS.contains(provider)) {
            PROVIDERS.add(provider);
        }
    }

    public static void unregister(@Nullable LimbPartProvider provider) {
        if (provider != null) PROVIDERS.remove(provider);
    }

    /**
     * 查询某个肢体内部槽在玩家模型上追加的部件。默认（没有任何 Provider）返回空列表。
     */
    public static List<ExtraPart> extraParts(Object player, OrganSlotGroup limbGroup,
                                             List<ItemStack> stacks) {
        if (PROVIDERS.isEmpty() || limbGroup == null) return List.of();
        List<ExtraPart> result = new ArrayList<>(0);
        for (LimbPartProvider provider : PROVIDERS) {
            List<ExtraPart> parts = provider.partsFor(player, limbGroup, stacks);
            if (parts != null && !parts.isEmpty()) result.addAll(parts);
        }
        return result;
    }
}
