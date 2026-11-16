package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedSweetBerryBush;

public class InfestedSweetBerries extends Item {
    public InfestedSweetBerries(Properties properties) {
        super(properties.food(createFoodProperties()));
    }

    private static FoodProperties createFoodProperties() {

        return new FoodProperties.Builder()
                .nutrition(2)
                .saturationMod(0.4F)
                .effect(() -> new MobEffectInstance(
                        ModEffects.PARA_CURSE.get(),
                        10 * 20,
                        0
                ), 1.0F)
                .effect(() -> new MobEffectInstance(
                        MobEffects.HUNGER,
                        10 * 20,
                        0
                ), 1.0F)
                .effect(() -> new MobEffectInstance(
                        MobEffects.CONFUSION,
                        10 * 20,
                        0
                ), 1.0F)
                .build();
    }

    /**
     * 虫染甜浆果可以像原版甜浆果一样右键放置虫染甜浆果丛，并且总是放置未结果（age=0）的方块。
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos target = level.getBlockState(clicked).canBeReplaced(new BlockPlaceContext(context))
                ? clicked
                : clicked.relative(context.getClickedFace());

        BlockState bushState = ModBlocks.INFESTED_SWEET_BERRY_BUSH.get()
                .defaultBlockState()
                .setValue(InfestedSweetBerryBush.AGE, 0);

        if (!level.isEmptyBlock(target) || !bushState.canSurvive(level, target)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        level.setBlock(target, bushState, 11);
        level.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, target);

        SoundType soundType = bushState.getSoundType(level, target, context.getPlayer());
        level.playSound(null, target, soundType.getPlaceSound(), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

        if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
            ItemStack stack = context.getItemInHand();
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
