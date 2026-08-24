package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedPlayer;

@Mod.EventBusSubscriber
public class SpawnEggHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack stack = event.getItemStack();
        BlockPos pos = event.getPos();
        var face = event.getFace();
        var hand = event.getHand();

        if (level.isClientSide) return;

        if (!(stack.getItem() instanceof SpawnEggItem egg)) return;
        EntityType<?> type = egg.getType(stack.getTag());
        if (type != ModEntities.INFESTED_PLAYER.get()) return;

        BlockPos spawnPos = pos.relative(face);
        BlockState state = level.getBlockState(spawnPos);
        if (!state.isAir() && !state.canBeReplaced()) return;

        
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(false);

        
        if (level instanceof ServerLevel serverLevel) {
            InfestedPlayer infested = new InfestedPlayer(ModEntities.INFESTED_PLAYER.get(), level);
            infested.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            infested.copyFromPlayer(player);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            serverLevel.addFreshEntity(infested);
        }
    }
}