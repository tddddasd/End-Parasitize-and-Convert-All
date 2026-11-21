package org.tdddd.epca.impl.utils;

import net.minecraft.world.effect.MobEffectInstance;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

/**
 * 「虫咒缚身 II 级及以上无法被牛奶净化」的线程内守卫。
 *
 * <p>NeoForge 26 的 {@code MobEffectEvent.Remove} 依旧是<b>可取消</b>的
 * （见 {@code EventHooks#onEffectRemoved}，{@code LivingEntity#removeAllEffects()} /
 * {@code removeEffect(Holder)} 都会 post 它，且被取消的效果不会被移除），
 * 但它同时被多条路径触发：</p>
 * <ul>
 *   <li>{@code ClearAllStatusEffectsConsumeEffect#apply} —— 牛奶（{@code Consumables.MILK_BUCKET} 的
 *       饮用效果，内部就是 {@code LivingEntity#removeAllEffects()}）；</li>
 *   <li>{@code LivingEntity#removeEffect(MobEffect)} —— 代码 / 药水逻辑主动移除；</li>
 *   <li>{@code LivingEntity#removeAllEffects()} —— {@code /effect clear} 与其它清除逻辑。</li>
 * </ul>
 *
 * <p>直接无条件取消 {@code Remove} 会连 {@code /effect clear} 一起挡掉，所以这里只对「喝牛奶」这条路径生效：
 * {@code MilkConsumeEffectMixin} 在 {@code ClearAllStatusEffectsConsumeEffect#apply} 的 HEAD/RETURN
 * 维护一个线程内的调用深度，事件处理器只在深度 &gt; 0 时取消。</p>
 *
 * <p><b>26.1.2 与 1.20.1 的差异</b>：1.20.1 抹在 {@code MilkBucketItem#finishUsingItem} 上；
 * 26 已经没有 {@code MilkBucketItem} 类（牛奶改成了数据驱动的 {@code Consumables.MILK_BUCKET} +
 * {@code ClearAllStatusEffectsConsumeEffect}），所以注入点换成后者，语义完全等价，且范围更精确。</p>
 *
 * <p>结果：牛奶（II 级及以上）移除被阻止；I 级仍可被牛奶解除；
 * {@code /effect clear}、死亡清除、自然到期、代码里的 {@code removeEffect} 全部照常生效。
 * <b>本机制不发送任何 actionbar / 聊天提示</b>（静默）。</p>
 */
public final class ParaCurseRemovalGuard {

    /** 当前线程是否正在执行 curative item（牛奶）清理；用调用深度支持嵌套。 */
    private static final ThreadLocal<int[]> CURE_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    /** II 级（amplifier = 1）及以上不可被牛奶解除。 */
    private static final int UNREMOVABLE_AMPLIFIER = 1;

    private ParaCurseRemovalGuard() {
    }

    public static void beginCurativeRemoval() {
        CURE_DEPTH.get()[0]++;
    }

    public static void endCurativeRemoval() {
        int[] depth = CURE_DEPTH.get();
        if (depth[0] > 0) {
            depth[0]--;
        }
    }

    public static boolean isCurativeRemovalActive() {
        return CURE_DEPTH.get()[0] > 0;
    }

    /** 该效果实例此刻是否属于「被牛奶清理、但虫咒缚身等级过高不允许移除」的情况。 */
    public static boolean blocksCurativeRemoval(MobEffectInstance instance) {
        if (instance == null || !isCurativeRemovalActive()) {
            return false;
        }
        // 26 的 MobEffectInstance#getEffect() 返回 Holder<MobEffect>：比较 value() 得到的单例，
        // 避免 DELEGATE / Registry.Reference 之间用 == 比较 Holder 本身而漏判（NBT 读回来的效果就是 Reference）。
        return instance.getEffect().value() == ModEffects.PARA_CURSE.value()
                && instance.getAmplifier() >= UNREMOVABLE_AMPLIFIER;
    }
}
