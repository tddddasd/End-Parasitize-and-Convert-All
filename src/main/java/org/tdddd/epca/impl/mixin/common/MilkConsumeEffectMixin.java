package org.tdddd.epca.impl.mixin.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.utils.ParaCurseRemovalGuard;

/**
 * 让「虫咒缚身 II 级及以上不能被牛奶解除」只作用于<b>喝牛奶</b>这一条路径。
 *
 * <p><b>26.1.2 与 1.20.1 的差异</b>：1.20.1 的注入点是
 * {@code MilkBucketItem#finishUsingItem(ItemStack, Level, LivingEntity)}（Forge 把
 * {@code curePotionEffects} 提到方法首行）。26 已经没有 {@code MilkBucketItem}：
 * 牛奶变成数据驱动的 {@code Consumables.MILK_BUCKET = defaultDrink()
 * .onConsume(ClearAllStatusEffectsConsumeEffect.INSTANCE).build()}，饮用时由
 * {@code ClearAllStatusEffectsConsumeEffect#apply(Level, ItemStack, LivingEntity)} 调
 * {@code LivingEntity#removeAllEffects()}。所以注入点换成 {@code apply}：</p>
 *
 * <pre>{@code
 * public boolean apply(Level level, ItemStack stack, LivingEntity user) {
 *     return user.removeAllEffects();   // 逐个效果 post MobEffectEvent.Remove（可取消）
 * }
 * }</pre>
 *
 * <p>这里只在牛奶生效的这段时间内打开 {@link ParaCurseRemovalGuard} 的线程内标记，事件处理器据此取消
 * II 级及以上的虫咒缚身移除：</p>
 *
 * <ul>
 *   <li>{@code /effect clear}（{@code LivingEntity#removeAllEffects()}，不经过 drink 路径）—— 标记为 false，照常清除；</li>
 *   <li>死亡清除 —— 同样不在这条路径上，照常清除；</li>
 *   <li>自然到期（{@code MobEffectEvent.Expired}）—— 不经过 {@code Remove}，照常到期；</li>
 *   <li>代码里的 {@code removeEffect(...)} —— 标记为 false，照常移除。</li>
 * </ul>
 *
 * <p>不直接注入 {@code LivingEntity#removeAllEffects}，是因为它同时被 {@code /effect clear} 用到，
 * 无法区分调用来源。全程<b>不发送任何 actionbar / 聊天提示</b>。</p>
 */
@Mixin(ClearAllStatusEffectsConsumeEffect.class)
public abstract class MilkConsumeEffectMixin {

    @Inject(method = "apply", at = @At("HEAD"))
    private void epca$beginMilkRemoval(Level level, ItemStack stack, LivingEntity user,
                                       CallbackInfoReturnable<Boolean> cir) {
        ParaCurseRemovalGuard.beginCurativeRemoval();
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void epca$endMilkRemoval(Level level, ItemStack stack, LivingEntity user,
                                     CallbackInfoReturnable<Boolean> cir) {
        ParaCurseRemovalGuard.endCurativeRemoval();
    }
}
