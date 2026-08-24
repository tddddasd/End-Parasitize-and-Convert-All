package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.common.ForgeSpawnEggItem;
import org.tdddd.epca.impl.overworld.registry.ModEntities;

import java.util.Random;

public class InfestedSlimeSpawnEgg extends ForgeSpawnEggItem {
    private static final Random RANDOM = new Random();

    public InfestedSlimeSpawnEgg(Properties properties) {
        
        super(() -> ModEntities.INFESTED_SLIME_SIZE0.get(), -1, -1, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack itemStack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

        EntityType<?> selectedType = selectRandomSlimeType();

        if (selectedType.spawn(serverLevel, itemStack, context.getPlayer(), pos, MobSpawnType.SPAWN_EGG, true, true) != null) {
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
                itemStack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private EntityType<?> selectRandomSlimeType() {
        int choice = RANDOM.nextInt(3);
        return switch (choice) {
            case 0 -> ModEntities.INFESTED_SLIME_SIZE0.get();
            case 1 -> ModEntities.INFESTED_SLIME_SIZE1.get();
            default -> ModEntities.INFESTED_SLIME_SIZE3.get();
        };
    }
}