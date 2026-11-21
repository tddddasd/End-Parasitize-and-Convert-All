package org.tdddd.epca.impl.utils;

import net.minecraft.world.effect.MobEffectInstance;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

/**
 * 「虫咒缚身 II 级及以上无法被牛奶净化」的线程内守卫。
 *
 * <p>Forge 1.20.1 的 {@code MobEffectEvent.Remove} 是<b>可取消</b>的，但它同时被三条路径触发：</p>
 * <ul>
 *   <li>{@code LivingEntity#curePotionEffects(ItemStack)} —— 牛奶（{@code MilkBucketItem}）的清理路径（我们要拦的）；</li>
 *   <li>{@code LivingEntity#removeEffect(MobEffect)} —— 代码 / 药水逻辑主动移除；</li>
 *   <li>{@code LivingEntity#removeAllEffects()} —— {@code /effect clear} 与死亡清除。</li>
 * </ul>
 *
 * <p>直接无条件取消 {@code Remove} 会连 {@code /effect clear} 和死亡清除一起挡掉，所以这里只对
 * 「喝牛奶」这条路径生效：{@code MilkBucketItemMixin} 在 {@code MilkBucketItem#finishUsingItem} 的
 * HEAD/RETURN 维护一个线程内的调用深度（该方法的开头就是 Forge 提前的 {@code curePotionEffects} 调用），
 * 事件处理器只在深度 &gt; 0 时取消。</p>
 *
 * <p>结果：牛奶（II 级及以上）移除被阻止并给 actionbar 提示；I 级仍可被牛奶解除；
 * 死亡清除、{@code /effect clear}、自然到期、代码里的 {@code removeEffect} 全部照常生效。</p>
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
        return instance.getEffect() == ModEffects.PARA_CURSE.get()
                && instance.getAmplifier() >= UNREMOVABLE_AMPLIFIER;
    }
}
