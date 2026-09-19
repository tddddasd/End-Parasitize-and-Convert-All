package org.tdddd.epca.impl.events;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModItems;

@EventBusSubscriber(modid = epca.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class SpearAttackHandler {

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        // 1. 确保在客户端执行
        if (!(event.getEntity().level().isClientSide())) return;

        // 2. 获取玩家（攻击者）
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;

        // 3. 判断是否手持矛类武器
        ItemStack mainHand = player.getMainHandItem();
        boolean isSpear = mainHand.getItem() == ModItems.WOODEN_SPEAR.get() ||
                mainHand.getItem() == ModItems.STONE_SPEAR.get() ||
                mainHand.getItem() == ModItems.FLINT_SPEAR.get() ||
                mainHand.getItem() == ModItems.COPPER_SPEAR.get() ||
                mainHand.getItem() == ModItems.IRON_SPEAR.get() ||
                mainHand.getItem() == ModItems.GOLDEN_SPEAR.get() ||
                mainHand.getItem() == ModItems.DIAMOND_SPEAR.get() ||
                mainHand.getItem() == ModItems.NETHERITE_SPEAR.get();

        // 4. 判断目标是否为生物（LivingEntity）
        boolean isLiving = event.getTarget() instanceof LivingEntity;

        // 5. 条件满足时播放动画
        if (isSpear && isLiving) {
            // 获取与玩家关联的 ModifierLayer（已在工厂中注册）
            ModifierLayer<IAnimation> animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                    .getPlayerAssociatedData(player)
                    .get(Identifier.fromNamespaceAndPath(epca.MODID, "stab"));

            if (animation != null) {
                // 从注册表获取 KeyframeAnimation，并用 KeyframeAnimationPlayer 包装
                var keyframe = PlayerAnimationRegistry.getAnimation(Identifier.fromNamespaceAndPath(epca.MODID, "stab"));
                if (keyframe != null) {
                    animation.setAnimation(new KeyframeAnimationPlayer(keyframe));
                }
            }
        }
    }
}