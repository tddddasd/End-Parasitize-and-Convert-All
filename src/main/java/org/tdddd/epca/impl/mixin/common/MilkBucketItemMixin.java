package org.tdddd.epca.impl.mixin.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.utils.ParaCurseRemovalGuard;

/**
 * 让「虫咒缚身 II 级及以上不能被牛奶解除」只作用于<b>喝牛奶</b>这一条路径。
 *
 * <p>Forge 1.20.1 的 {@code MilkBucketItem#finishUsingItem} 在方法开头就会调用
 * {@code LivingEntity#curePotionEffects(ItemStack)}（Forge 补丁把这行提前了），该方法逐个效果
 * post {@code MobEffectEvent.Remove}。这里只在牛奶生效的这段时间内打开
 * {@link ParaCurseRemovalGuard} 的线程内标记，事件处理器据此取消 II 级及以上的虫咒缚身移除：</p>
 *
 * <ul>
 *   <li>{@code /effect clear}（{@code LivingEntity#removeAllEffects()}）—— 标记为 false，照常清除；</li>
 *   <li>死亡清除 —— 同样不在这条路径上，照常清除；</li>
 *   <li>自然到期（{@code MobEffectEvent.Expired}）—— 不经过 Remove，照常到期；</li>
 *   <li>代码里的 {@code removeEffect(...)} —— 标记为 false，照常移除。</li>
 * </ul>
 *
 * <p>不直接在 {@code LivingEntity#curePotionEffects} 上注入，是因为它是 Forge 附加方法、没有
 * obfuscation 映射（Mixin AP 会直接报 "Unable to locate obfuscation mapping"）。</p>
 */
@Mixin(MilkBucketItem.class)
public abstract class MilkBucketItemMixin {

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void epca$beginMilkRemoval(ItemStack stack, Level level, LivingEntity entity,
                                       CallbackInfoReturnable<ItemStack> cir) {
        ParaCurseRemovalGuard.beginCurativeRemoval();
    }

    @Inject(method = "finishUsingItem", at = @At("RETURN"))
    private void epca$endMilkRemoval(ItemStack stack, Level level, LivingEntity entity,
                                     CallbackInfoReturnable<ItemStack> cir) {
        ParaCurseRemovalGuard.endCurativeRemoval();
    }
}
