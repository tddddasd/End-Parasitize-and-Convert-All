package org.tdddd.epca.impl.client.organ.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.data.organ.OrganModelDisplay;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端只读数据：每个玩家（自己 + 其他玩家）的「内器官模型显示」开关与头/胸/四肢内部器官。
 * 全部由服务端 {@code SyncOrganModelDisplayPacket} 推送，客户端不自行决定状态。
 */
public final class OrganModelClientData {

    public record Display(boolean visible,
                          List<ItemStack> headInner,
                          List<ItemStack> torsoInner,
                          Map<OrganSlotGroup, List<ItemStack>> limbs) {

        public static final Display EMPTY = new Display(false, List.of(), List.of(), Map.of());

        public boolean hasAnyHeadOrTorso() {
            return hasAny(headInner) || hasAny(torsoInner);
        }

        public List<ItemStack> limb(OrganSlotGroup group) {
            List<ItemStack> stacks = limbs.get(group);
            return stacks == null ? List.of() : stacks;
        }
    }

    private static final Map<UUID, Display> DISPLAYS = new ConcurrentHashMap<>();

    private OrganModelClientData() {
    }

    public static void onSync(@Nullable UUID playerId, boolean visible, @Nullable CompoundTag organs) {
        if (playerId == null) return;
        if (!visible) {
            // 关闭时不保留任何物品数据，渲染层直接跳过。
            DISPLAYS.put(playerId, Display.EMPTY);
            return;
        }
        List<ItemStack> head = OrganModelDisplay.decodeGroup(organs, OrganModelDisplay.TAG_HEAD,
                OrganSlotGroup.HEAD_INNER.size());
        List<ItemStack> torso = OrganModelDisplay.decodeGroup(organs, OrganModelDisplay.TAG_TORSO,
                OrganSlotGroup.TORSO_INNER.size());

        Map<OrganSlotGroup, List<ItemStack>> limbs = new LinkedHashMap<>();
        for (OrganSlotGroup group : OrganSlotGroup.LIMB_INNER_GROUPS) {
            String key = OrganModelDisplay.limbTagKey(group);
            limbs.put(group, key == null ? List.of()
                    : OrganModelDisplay.decodeGroup(organs, key, group.size()));
        }
        DISPLAYS.put(playerId, new Display(true, head, torso, Map.copyOf(limbs)));
    }

    public static Display get(@Nullable UUID playerId) {
        if (playerId == null) return Display.EMPTY;
        Display display = DISPLAYS.get(playerId);
        return display == null ? Display.EMPTY : display;
    }

    /** 该玩家是否应该显示头部内器官窗口（开关打开 + 头内有器官）。 */
    public static boolean showHead(@Nullable UUID playerId) {
        if (playerId == null) return false;
        Display display = get(playerId);
        return display.visible() && hasAny(display.headInner());
    }

    /** 该玩家是否应该显示躯干内器官窗口（开关打开 + 胸内有器官）。 */
    public static boolean showTorso(@Nullable UUID playerId) {
        if (playerId == null) return false;
        Display display = get(playerId);
        return display.visible() && hasAny(display.torsoInner());
    }

    public static void clear() {
        DISPLAYS.clear();
    }

    public static void remove(@Nullable UUID playerId) {
        if (playerId != null) DISPLAYS.remove(playerId);
    }

    static boolean hasAny(@Nullable List<ItemStack> stacks) {
        if (stacks == null) return false;
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) return true;
        }
        return false;
    }
}
