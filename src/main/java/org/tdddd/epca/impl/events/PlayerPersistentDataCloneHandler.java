package org.tdddd.epca.impl.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.EPCANoteUnlocks;
import org.tdddd.epca.impl.overworld.data.organ.species.HiveLeaderSpeciesManager;
import org.tdddd.epca.impl.overworld.data.organ.species.HiveLeaderSpeciesState;

/**
 * 玩家「死亡 → 重生」（以及从末地返回时重建实体）把持久化数据里的进度搬到新实体。
 *
 * <p><b>26.1.2 依据</b>：NeoForge 26 仍然把玩家数据存在 {@code Player#getPersistentData()}
 * （{@code NeoForgeData} 复合标签，随玩家数据写进 playerdata），而且
 * {@code ServerPlayer#restoreFrom} 依旧被 NeoForge 补丁改成只搬 {@code PlayerPersisted} 子标签：</p>
 * <pre>{@code
 * var old = oldPlayer.getPersistentData();
 * if (old.contains(PERSISTED_NBT_TAG))
 *     getPersistentData().put(PERSISTED_NBT_TAG, old.get(PERSISTED_NBT_TAG));
 * net.neoforged.neoforge.event.EventHooks.onPlayerClone(this, oldPlayer, !restoreAll);
 * }</pre>
 * <p>也就是说玩家 persistentData 里的其它键（虫咒笔记的解锁集合、领巢者种阶状态）<b>不会</b>自动过去，
 * 死亡重生后笔记就会「恢复初始状态」。这里在 {@link PlayerEvent.Clone} 里手动搬运这几个键。</p>
 *
 * <p>搬运的键：</p>
 * <ul>
 *   <li>{@link EPCANoteUnlocks#KEY_UNLOCKED} —— 笔记已发现条目（{@code ListTag<String>}）；</li>
 *   <li>{@link EPCANoteUnlocks#KEY_DETAIL} —— 已购买详情（{@code ListTag<String>}）；</li>
 *   <li>{@link HiveLeaderSpeciesState#PERSISTENT_KEY} —— 领巢者种阶 / 击杀数 / 适应层数（{@code CompoundTag}）。</li>
 * </ul>
 *
 * <p>非死亡 clone（从末地返回）也走同一段复制：那条路径同样只搬 {@code PlayerPersisted}，
 * 复制是无损的（新实体此刻还没有这些键）。</p>
 */
@EventBusSubscriber(modid = epca.MODID)
public final class PlayerPersistentDataCloneHandler {

    /** 需要在 clone 时手动搬运的 persistentData 键（均位于玩家 persistentData 根下）。 */
    private static final String[] CLONE_KEYS = {
            EPCANoteUnlocks.KEY_UNLOCKED,
            EPCANoteUnlocks.KEY_DETAIL,
            HiveLeaderSpeciesState.PERSISTENT_KEY
    };

    private PlayerPersistentDataCloneHandler() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player replacement = event.getEntity();
        if (original == null || replacement == null) {
            return;
        }

        CompoundTag from = original.getPersistentData();
        CompoundTag to = replacement.getPersistentData();
        for (String key : CLONE_KEYS) {
            Tag value = from.get(key);
            if (value != null) {
                to.put(key, value.copy());
            }
        }

        if (!(replacement instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 领巢者状态有按 UUID 的内存缓存：让缓存与刚搬过来的 NBT 收敛一次（缓存更新时以缓存为准，
        // 缓存不存在时按刚复制的数据重建），保证新实体上的数据与旧实体一致。
        if (to.contains(HiveLeaderSpeciesState.PERSISTENT_KEY)) {
            try {
                HiveLeaderSpeciesManager.state(serverPlayer).save(serverPlayer);
            } catch (Throwable ignored) {
            }
        }

        // 客户端解锁缓存属于旧实体，重生后立刻补一份，避免笔记界面短暂显示为空。
        try {
            NoteUnlockEvents.syncTo(serverPlayer);
        } catch (Throwable ignored) {
        }
    }
}
