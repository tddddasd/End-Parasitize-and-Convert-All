package org.tdddd.epca.impl.overworld.data;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.overworld.registry.gui.menus.EPCANoteScreen;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端侧的笔记解锁缓存（按玩家 UUID）。
 *
 * <p>数据只可能来自服务端的 {@link org.tdddd.epca.impl.network.packet.s2c.SyncNoteUnlocksPacket}，
 * 客户端**没有任何**“我自己判断解锁了”的写入入口 —— 这是需求里“客户端不得自行判定成功”的落点。
 * 收到同步后如果笔记界面正开着，会顺手让它重排一次页，于是“走近怪物”这种实时解锁会立刻
 * 从黑色乱码变成可读文字。</p>
 *
 * <p>按 UUID 存而不是只存一份，是为了 LAN / 多账号切换时不串味；渲染时用本地玩家 UUID 查表。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class EPCANoteClientUnlocks {

    private static final Map<UUID, Set<ResourceLocation>> UNLOCKED = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<ResourceLocation>> DETAIL = new ConcurrentHashMap<>();

    private EPCANoteClientUnlocks() {
    }

    /** 收到 S2C 同步：整体替换该玩家的两份集合 */
    public static void apply(UUID playerId, List<String> unlocked, List<String> detailUnlocked) {
        if (playerId == null) {
            return;
        }
        UNLOCKED.put(playerId, Set.copyOf(EPCANoteUnlocks.fromStringList(unlocked)));
        DETAIL.put(playerId, Set.copyOf(EPCANoteUnlocks.fromStringList(detailUnlocked)));
        refreshOpenScreen();
    }

    /** 断线时清空，避免不同存档之间串数据 */
    public static void clear() {
        UNLOCKED.clear();
        DETAIL.clear();
    }

    public static boolean isUnlocked(ResourceLocation entityId) {
        return contains(UNLOCKED, EPCANoteUnlocks.normalize(entityId));
    }

    public static boolean isDetailUnlocked(ResourceLocation entityId) {
        return contains(DETAIL, EPCANoteUnlocks.normalize(entityId));
    }

    private static boolean contains(Map<UUID, Set<ResourceLocation>> source, ResourceLocation entityId) {
        if (entityId == null) {
            return false;
        }
        UUID local = localPlayerId();
        if (local == null) {
            return false;
        }
        Set<ResourceLocation> set = source.get(local);
        return set != null && set.contains(entityId);
    }

    private static UUID localPlayerId() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.player == null ? null : minecraft.player.getUUID();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void refreshOpenScreen() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.screen instanceof EPCANoteScreen screen) {
                screen.refreshUnlockState();
            }
        } catch (Throwable ignored) {
        }
    }
}
