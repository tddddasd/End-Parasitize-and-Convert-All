package org.tdddd.epca.impl.client;

import java.util.List;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.tdddd.epca.impl.epca;


@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class WaterColorClientSetup {

    /** Same fallback the 1.20.1 mixins used when the vanilla lookup produced nothing. */
    private static final int DEFAULT_WATER_COLOR = 0x3F76E4;

    private WaterColorClientSetup() {}

    @SubscribeEvent
    public static void registerBlockTintSources(RegisterColorHandlersEvent.BlockTintSources event) {
        List<BlockTintSource> water = List.of(new EpcaWaterBlockTintSource());
        event.register(water, Blocks.WATER, Blocks.BUBBLE_COLUMN);
        event.register(List.of(new EpcaWaterCauldronTintSource()), Blocks.WATER_CAULDRON);
    }

    /** Block-state tint used for {@code Blocks.WATER} / {@code Blocks.BUBBLE_COLUMN} in the world. */
    private static final class EpcaWaterBlockTintSource implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            // No world context (inventory / GUI): the 1.20.1 mixin could not run either, so keep the
            // vanilla default rather than forcing a biome lookup that is impossible here.
            return -1;
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return applyEffect(pos, biomeWaterColor(level, pos));
        }

        @Override
        public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return applyEffect(pos, biomeWaterColor(level, pos));
        }
    }

    /** Cauldron water follows the same effect (1.20.1 tinted it through the biome water hook too). */
    private static final class EpcaWaterCauldronTintSource implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            return -1;
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return applyEffect(pos, biomeWaterColor(level, pos));
        }
    }

    private static int biomeWaterColor(BlockAndTintGetter level, BlockPos pos) {
        int color = BiomeColors.getAverageWaterColor(level, pos);
        return color == -1 ? DEFAULT_WATER_COLOR : color;
    }

    /** Exactly the 1.20.1 behaviour: hand the vanilla water colour to the effects manager. */
    private static int applyEffect(BlockPos pos, int originalColor) {
        if (pos == null) {
            return originalColor;
        }
        return WaterColorEffectsManager.getWaterColor(pos, originalColor);
    }
}
