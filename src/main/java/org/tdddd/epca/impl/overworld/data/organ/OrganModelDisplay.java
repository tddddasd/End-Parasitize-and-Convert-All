package org.tdddd.epca.impl.overworld.data.organ;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncOrganModelDisplayPacket;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 「内器官模型显示」开关的权威状态 + 显示数据同步。
 *
 * <p>状态存放在玩家持久化数据（{@code player.getPersistentData()}）里，因此退出重进仍然保留；
 * 客户端不自行决定状态，只读取服务端广播（见 {@link SyncOrganModelDisplayPacket}）。</p>
 */
public final class OrganModelDisplay {

    /** 玩家持久化数据里的键：是否显示内器官模型（默认 false）。 */
    public static final String PERSISTENT_KEY = "EpcaOrganModelVisible";

    private static final String TAG_VISIBLE = "Visible";

    /** 同步用的复合标签键：头部内部 / 躯干内部 / 四肢内部物品。 */
    public static final String TAG_HEAD = "Head";
    public static final String TAG_TORSO = "Torso";
    public static final String TAG_LEFT_ARM = "LeftArm";
    public static final String TAG_RIGHT_ARM = "RightArm";
    public static final String TAG_LEFT_LEG = "LeftLeg";
    public static final String TAG_RIGHT_LEG = "RightLeg";
    public static final String TAG_ITEMS = "Items";

    private OrganModelDisplay() {
    }

    public static boolean isVisible(@Nullable Player player) {
        if (player == null) return false;
        CompoundTag tag = player.getPersistentData().getCompoundOrEmpty(PERSISTENT_KEY);
        return tag.getBooleanOr(TAG_VISIBLE, false);
    }

    public static void setVisible(@Nullable Player player, boolean visible) {
        if (player == null) return;
        CompoundTag tag = player.getPersistentData().getCompoundOrEmpty(PERSISTENT_KEY);
        if (tag.getBooleanOr(TAG_VISIBLE, false) == visible) return;
        tag.putBoolean(TAG_VISIBLE, visible);
        player.getPersistentData().put(PERSISTENT_KEY, tag);
    }

    public static boolean toggle(ServerPlayer player) {
        boolean next = !isVisible(player);
        setVisible(player, next);
        return next;
    }

    /**
     * 把「头部内部 + 躯干内部 + 四肢内部」的物品编码成一个复合标签。
     * 头部/躯干用于渲染内器官，四肢用于外部模型扩展点（{@code OrganLimbModelExtension}）。
     */
    public static CompoundTag encodeVisibleOrgans(@Nullable HiveLeaderOrganData data) {
        CompoundTag tag = new CompoundTag();
        tag.put(TAG_HEAD, encodeGroup(data, OrganSlotGroup.HEAD_INNER));
        tag.put(TAG_TORSO, encodeGroup(data, OrganSlotGroup.TORSO_INNER));
        tag.put(TAG_LEFT_ARM, encodeGroup(data, OrganSlotGroup.LEFT_ARM));
        tag.put(TAG_RIGHT_ARM, encodeGroup(data, OrganSlotGroup.RIGHT_ARM));
        tag.put(TAG_LEFT_LEG, encodeGroup(data, OrganSlotGroup.LEFT_LEG));
        tag.put(TAG_RIGHT_LEG, encodeGroup(data, OrganSlotGroup.RIGHT_LEG));
        return tag;
    }

    /** 四肢分组 → 同步标签键。 */
    @Nullable
    public static String limbTagKey(@Nullable OrganSlotGroup group) {
        if (group == OrganSlotGroup.LEFT_ARM) return TAG_LEFT_ARM;
        if (group == OrganSlotGroup.RIGHT_ARM) return TAG_RIGHT_ARM;
        if (group == OrganSlotGroup.LEFT_LEG) return TAG_LEFT_LEG;
        if (group == OrganSlotGroup.RIGHT_LEG) return TAG_RIGHT_LEG;
        return null;
    }

    private static ListTag encodeGroup(@Nullable HiveLeaderOrganData data, OrganSlotGroup group) {
        ListTag list = new ListTag();
        for (int local = 0; local < group.size(); local++) {
            ItemStack stack = data == null ? ItemStack.EMPTY : data.getItem(group.globalIndex(local));
            list.add(encodeStack(stack));
        }
        return list;
    }

    private static CompoundTag encodeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        Tag encoded = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElse(null);
        return encoded instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    /**
     * 客户端/渲染层解析：读取指定组的物品列表，长度固定为组大小，缺失补空。
     */
    public static List<ItemStack> decodeGroup(@Nullable CompoundTag tag, String key, int size) {
        List<ItemStack> stacks = new ArrayList<>(size);
        ListTag list = tag == null ? new ListTag() : tag.getListOrEmpty(key);
        for (int i = 0; i < size; i++) {
            if (i < list.size()) {
                CompoundTag entry = list.getCompoundOrEmpty(i);
                stacks.add(entry.isEmpty() ? ItemStack.EMPTY
                        : ItemStack.CODEC.parse(NbtOps.INSTANCE, entry).result().orElse(ItemStack.EMPTY));
            } else {
                stacks.add(ItemStack.EMPTY);
            }
        }
        return stacks;
    }

    /** 器官数据变化后：把该玩家的显示数据广播给所有人。 */
    public static void onOrgansChanged(@Nullable UUID uuid) {
        ServerPlayer player = findPlayer(uuid);
        if (player == null) return;
        broadcast(player);
    }

    public static void broadcast(ServerPlayer player) {
        if (player == null) return;
        HiveLeaderOrganData data = HiveLeaderOrganSavedData.read(player);
        ModNetwork.sendToAll(new SyncOrganModelDisplayPacket(player.getUUID(), isVisible(player), data));
    }

    public static void sendTo(@Nullable ServerPlayer viewer, @Nullable ServerPlayer target) {
        if (viewer == null || target == null) return;
        HiveLeaderOrganData data = HiveLeaderOrganSavedData.read(target);
        ModNetwork.sendToPlayer(viewer, new SyncOrganModelDisplayPacket(target.getUUID(),
                isVisible(target), data));
    }

    /** 玩家登录：把自己的状态广播出去，同时接收所有在线领巢者的状态。 */
    public static void onPlayerJoin(ServerPlayer player) {
        if (player == null) return;
        broadcast(player);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player) continue;
            if (!HiveLeaderOrganSavedData.hasData(other)) continue;
            sendTo(player, other);
        }
    }

    @Nullable
    private static ServerPlayer findPlayer(@Nullable UUID uuid) {
        if (uuid == null) return null;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getPlayerList().getPlayer(uuid);
    }
}
