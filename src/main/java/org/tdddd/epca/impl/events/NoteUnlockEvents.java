package org.tdddd.epca.impl.events;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncNoteUnlocksPacket;
import org.tdddd.epca.impl.overworld.data.EPCANoteDetailCosts;
import org.tdddd.epca.impl.overworld.data.EPCANoteUnlocks;

import java.util.List;
import java.util.Set;

/**
 * 虫咒笔记解锁的服务端逻辑（发现机制 + 详细数据解锁）。
 *
 * <p><b>发现</b>三条路径，满足任一即解锁该寄生体的笔记条目：</p>
 * <ol>
 *   <li>距离 ≤ 8 格：每个玩家每 20 tick 扫一次周围实体（{@link PlayerTickEvent.Post}）；</li>
 *   <li>被玩家攻击：{@link LivingIncomingDamageEvent}，{@code source.getEntity()} 是玩家；</li>
 *   <li>被玩家击杀：{@link LivingDeathEvent}，击杀者是玩家。</li>
 * </ol>
 *
 * <p><b>详细数据</b>：只处理客户端的请求包
 * （{@link org.tdddd.epca.impl.network.packet.c2s.NoteDetailUnlockRequestPacket}），
 * 服务端校验“已发现 / 未解锁过 / 材料够”，然后扣材料 + 写持久化 + 回同步包。
 * 客户端永远拿不到“本地就算解锁成功”的路径。</p>
 *
 * <p>所有写操作都落在玩家自己的 {@code persistentData}（见 {@link EPCANoteUnlocks}），
 * 换维度、退游戏、重进都不会丢。</p>
 *
 * <p><b>26.1.2 移植说明</b>（相对 1.20.1）：</p>
 * <ul>
 *   <li>{@code @Mod.EventBusSubscriber(modid = ..., bus = FORGE)} -&gt; 26 的
 *       {@code @EventBusSubscriber(modid = ...)}（游戏总线是默认值，且 26 会按事件类型自动分派）。</li>
 *   <li>{@code TickEvent.PlayerTickEvent(phase == END)} -&gt; {@code PlayerTickEvent.Post}。</li>
 *   <li>26 已删除 {@code LivingHurtEvent}，等价替身是 {@code LivingIncomingDamageEvent}
 *       （同样是“受到伤害时”，且 {@code getSource()} 语义一致）。</li>
 *   <li>{@code ForgeRegistries.ENTITY_TYPES / ITEMS} -&gt; {@code BuiltInRegistries.ENTITY_TYPE / ITEM}。</li>
 *   <li>{@code ResourceLocation} -&gt; {@code Identifier}。</li>
 * </ul>
 */
@EventBusSubscriber(modid = epca.MODID)
public final class NoteUnlockEvents {

    /** 8 格发现半径 */
    private static final double DISCOVER_RADIUS = 8.0D;

    /** 每 20 tick 检查一次附近实体 */
    private static final int SCAN_INTERVAL_TICKS = 20;

    /** 材料不足 */
    public static final String MSG_MISSING_MATERIALS = "epca.note.detail.missing";
    /** 还没发现这个寄生体 */
    public static final String MSG_LOCKED_ENTRY = "epca.note.detail.locked_entry";
    /** 解锁成功 */
    public static final String MSG_SUCCESS = "epca.note.detail.success";

    private NoteUnlockEvents() {
    }

    // ======================= 发现：距离 / 攻击 / 击杀 =======================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.tickCount % SCAN_INTERVAL_TICKS != 0) {
            return;
        }
        if (!scanNearby(player, level)) {
            return;
        }
        syncTo(player);
    }

    /** @return 是否新解锁了至少一个条目 */
    private static boolean scanNearby(ServerPlayer player, ServerLevel level) {
        boolean changed = false;
        try {
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(DISCOVER_RADIUS),
                    entity -> EPCANoteUnlocks.isNoteSpecies(entity.getType())
                            && entity.isAlive()
                            && player.distanceTo(entity) <= DISCOVER_RADIUS);
            for (LivingEntity entity : nearby) {
                Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                if (id != null && EPCANoteUnlocks.unlock(player, id)) {
                    changed = true;
                }
            }
        } catch (Throwable ignored) {
        }
        return changed;
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide()) {
            return;
        }
        if (!EPCANoteUnlocks.isNoteSpecies(victim.getType())) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (id != null && EPCANoteUnlocks.unlock(player, id)) {
            syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide()) {
            return;
        }
        if (!EPCANoteUnlocks.isNoteSpecies(victim.getType())) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (id != null && EPCANoteUnlocks.unlock(player, id)) {
            syncTo(player);
        }
    }

    // ======================= 详细数据解锁（C2S 请求的处理）=======================

    /**
     * 客户端点了「消耗 N 物品 解锁」。全部校验与扣除都在这里做。
     */
    public static void handleDetailUnlockRequest(ServerPlayer player, Identifier rawId) {
        Identifier id = EPCANoteUnlocks.normalize(rawId);
        if (id == null || !EPCANoteDetailCosts.isRegisteredEntity(id)) {
            return;
        }
        // 已解锁过：幂等，直接回同步（顺便让客户端刷新）
        if (EPCANoteUnlocks.isDetailUnlocked(player, id)) {
            syncTo(player);
            return;
        }
        // 必须先“发现”这个寄生体，才能解锁它的详细数据
        if (!EPCANoteUnlocks.isUnlocked(player, id)) {
            syncTo(player);
            return;
        }

        List<EPCANoteDetailCosts.Cost> costs = EPCANoteDetailCosts.costsFor(id);
        if (!hasMaterials(player, costs)) {
            syncTo(player);
            return;
        }

        consume(player, costs);
        EPCANoteUnlocks.unlockDetail(player, id);
        syncTo(player);
    }

    private static boolean hasMaterials(ServerPlayer player, List<EPCANoteDetailCosts.Cost> costs) {
        if (costs == null || costs.isEmpty()) {
            return true;
        }
        for (EPCANoteDetailCosts.Cost cost : costs) {
            if (count(player.getInventory(), cost) < cost.count()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从背包里扣材料：遍历 {@link Inventory} 暴露的所有槽位（主背包 + 副手 + 盔甲），逐槽扣减。
     */
    private static void consume(ServerPlayer player, List<EPCANoteDetailCosts.Cost> costs) {
        Inventory inventory = player.getInventory();
        for (EPCANoteDetailCosts.Cost cost : costs) {
            Item item = BuiltInRegistries.ITEM.getValue(cost.itemId());
            if (item == null) {
                continue;
            }
            int remaining = cost.count();
            for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty() || !stack.is(item)) {
                    continue;
                }
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                remaining -= taken;
                if (stack.isEmpty()) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static int count(Inventory inventory, EPCANoteDetailCosts.Cost cost) {
        Item item = BuiltInRegistries.ITEM.getValue(cost.itemId());
        if (item == null) {
            return 0;
        }
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    // ======================= 同步 =======================

    public static void syncTo(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Set<Identifier> unlocked = EPCANoteUnlocks.unlocked(player);
        Set<Identifier> detail = EPCANoteUnlocks.detailUnlocked(player);
        ModNetwork.sendToPlayer(player, new SyncNoteUnlocksPacket(
                player.getUUID(),
                EPCANoteUnlocks.toStringList(unlocked),
                EPCANoteUnlocks.toStringList(detail)));
    }
}
