package org.tdddd.epca.impl.overworld.registry.entities.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ParasiteEvents;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 给每只寄生体注入指挥 Goal（{@link HiveCommandGoal}，优先级 -1，占用 MOVE + LOOK 标志）。
 *
 * <p>用 {@link WeakHashMap} 记录已经注入过的实体实例：实体在区块重载时是新对象，
 * 会重新走一遍 {@link EntityJoinLevelEvent} 注入；旧对象被 GC 后记录自动消失，
 * 不会出现重复 Goal。没有指令时 Goal 的 {@code canUse()} 返回 false，
 * 行为与没有这个 Goal 完全一致。</p>
 *
 * <p>注入前还会扫一遍已有的 Goal 列表（{@link #hasCommandGoal(Mob)}）：万一
 * 指令系统之外又调了一次 {@link #ensure(Mob)}（例如菜单选中、下达指令都会调），
 * 或者实体被插件重新注入，也不会出现两个指挥 Goal 同时抢 MOVE/LOOK —— 那会让
 * 两个 Goal 各自下发移动/朝向，表现为原地打转。</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HiveCommandGoalInjector {

    private static final Set<Mob> INJECTED = Collections.newSetFromMap(new WeakHashMap<>());

    private HiveCommandGoalInjector() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Mob mob && ParasiteEvents.isParasiteEntity(mob)) {
            ensure(mob);
        }
    }

    /** 幂等：同一只实体实例只注入一次。 */
    public static void ensure(Mob mob) {
        if (mob == null || mob.level().isClientSide()) {
            return;
        }
        if (hasCommandGoal(mob)) {
            // 列表里已经有指挥 Goal（可能来自另一次 ensure）：只补登记，绝不重复注入。
            INJECTED.add(mob);
            return;
        }
        if (!INJECTED.add(mob)) {
            return;
        }
        // 优先级 -1：高于所有原版/种阶 Goal（它们最低用 0），保证跟随/看守/巡逻拿到移动权；
        // 同时占用 MOVE 与 LOOK：只抢 MOVE 的话，原版 RandomLookAroundGoal / LookAtPlayerGoal
        // 仍会跑，和本 Goal 抢 LookControl，脑袋/身体在两个朝向之间来回拧。
        // 没指令时 canUse 返回 false，不会影响原有 AI 的选择。
        mob.goalSelector.addGoal(-1, new HiveCommandGoal(mob));
    }

    /** 实体现有的 Goal 里是否已经有指挥 Goal（防止重复注入）。 */
    private static boolean hasCommandGoal(Mob mob) {
        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            Goal goal = wrapped.getGoal();
            if (goal instanceof HiveCommandGoal) {
                return true;
            }
        }
        return false;
    }
}
