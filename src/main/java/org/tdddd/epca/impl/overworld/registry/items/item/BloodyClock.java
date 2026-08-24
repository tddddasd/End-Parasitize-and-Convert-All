package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;

public class BloodyClock extends Item {
    public BloodyClock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            
            int stage = EvolutionManager.getStageForDimension(level);
            int points = EvolutionManager.getPointsForDimension(level);

            
            Component message = Component.literal("侵蚀阶段" + stage + " [侵蚀点数" + points + "]");

            
            serverPlayer.displayClientMessage(message, true);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
}