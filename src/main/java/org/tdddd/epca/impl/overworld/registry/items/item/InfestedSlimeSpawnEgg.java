package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSlime;

import java.util.Random;

/**
 * 虫染史莱姆刷怪蛋：单实体 + {@code Size} 驱动体型，出生时从三种自然体型（0/1/3）里随机。
 */
public class InfestedSlimeSpawnEgg extends SpawnEggItem {
    private static final Random RANDOM = new Random();

    public InfestedSlimeSpawnEgg(Properties properties) {
        super(properties.spawnEgg(ModEntities.INFESTED_SLIME.get()));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack itemStack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

        EntityType<?> selectedType = ModEntities.INFESTED_SLIME.get();

        var spawned = selectedType.spawn(serverLevel, itemStack, context.getPlayer(), pos,
                EntitySpawnReason.SPAWN_ITEM_USE, true, true);

        if (spawned instanceof InfestedSlime slime) {
            slime.setSize(InfestedSlime.NATURAL_SIZES[RANDOM.nextInt(InfestedSlime.NATURAL_SIZES.length)], true);
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
                itemStack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
