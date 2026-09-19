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

/**
 * 26.1.2 replacement for the removed 1.20.1 client mixins {@code BlockColorsMixin} and
 * {@code BiomeColorsMixin}.
 *
 * <p>In 1.20.1 those two mixins hooked {@code BlockColors#getColor(...)} and the (now deleted)
 * {@code net.minecraft.client.renderer.BiomeColors#getAverageWaterColor(BlockAndTintGetter, BlockPos)}
 * and pushed the result through {@link WaterColorEffectsManager#getWaterColor(BlockPos, int)}. In 26.1.2
 * block tinting was reworked: {@code BlockColors} only owns {@code List<BlockTintSource>} per block, and
 * NeoForge exposes a supported extension point for exactly that, so no mixin is required for the
 * <b>block</b> path:
 *
 * <ul>
 *   <li>{@link RegisterColorHandlersEvent.BlockTintSources} - replaces the per-block tint source for
 *       {@code Blocks.WATER} / {@code Blocks.BUBBLE_COLUMN} / {@code Blocks.WATER_CAULDRON}. This covers
 *       chunk/section terrain tinting that goes through {@code ClientLevel#getBlockTint(BlockPos,
 *       BlockTintSource)}, the terrain particle path, and the cauldron.</li>
 * </ul>
 *
 * <h2>为什么这里没有（也做不到）注册 fluid 侧的 tint source</h2>
 *
 * <p>流动/静止的<b>水面</b>不是方块模型，而是由 {@code net.minecraft.client.renderer.block.FluidRenderer}
 * 画的（26.1.2 已经没有 {@code LiquidBlockRenderer}，该类在
 * {@code minecraft-patched-26.1.2.76.jar} 里不存在），并且它<b>完全不查 {@code BlockColors}</b>：
 * {@code FluidRenderer#tesselate(...)} 的字节码在偏移 294–322 处只做一件事——
 * <pre>
 *   FluidModel model = this.fluidModels.get(fluidState);            // FluidStateModelSet#get
 *   FluidTintSource ts = model.fluidTintSource();
 *   int tint = ts != null
 *       ? ts.colorInWorld(fluidState, blockState, level, pos)       // InterfaceMethod FluidTintSource.colorInWorld
 *       : -1;
 * </pre>
 * 所以水面颜色唯一的来源是「该流体在 {@code FluidStateModelSet} 里的那个 {@code FluidModel} 的
 * {@code fluidTintSource()}」。而水是<b>原版已经注册过</b>的流体，NeoForge 26.1.2 没有任何「替换已注册
 * fluid 模型」的受支持入口，证据（全部取自 {@code minecraft-patched-26.1.2.76.jar} 与
 * {@code neoforge-26.1.2.76-universal.jar} 的 {@code javap -c}）：
 *
 * <ol>
 *   <li>{@code FluidStateModelSet.bake(MaterialBaker)} 先算出
 *       {@code Map.of(WATER→waterModel, FLOWING_WATER→waterModel, LAVA→lavaModel, FLOWING_LAVA→lavaModel)}，
 *       再交给 {@code ClientHooks.gatherFluidModels(map, materials)}。也就是说事件触发时
 *       {@code Fluids.WATER} <b>已经在 map 里</b>。</li>
 *   <li>{@code ClientHooks.gatherFluidModels(...)} 把这个 map 复制成 {@code HashMap}，
 *       new {@code RegisterFluidModelsEvent} 并 {@code ModLoader.postEvent(...)}，之后只为
 *       {@code BuiltInRegistries.FLUID} 里<b>还不在 map 里</b>的流体补 missingModel。</li>
 *   <li>{@code RegisterFluidModelsEvent#register(Fluid, FluidModel)}（所有 public register 重载最终都走它）
 *       的实现是 {@code models.putIfAbsent(fluid, model)}，返回值非 null 就抛
 *       {@code IllegalStateException("Duplicate FluidModel registration for Fluid %s (old: %s, new: %s)")}
 *       —— 这正是之前那次「Duplicate FluidModel registration for Fluid minecraft:water」。
 *       事件类里<b>没有</b>任何 override/replace 方法，语义就是「只能新增，不能替换」。</li>
 *   <li>{@code net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions}
 *       在 26.1.2 里已经<b>没有 tint 方法</b>（只剩 {@code getRenderOverlayTexture} /
 *       {@code renderOverlay} / {@code modifyFogColor} / {@code modifyFogRender}），
 *       1.21 时代的 {@code getTintColor} 钩子不存在了；{@code RegisterClientExtensionsEvent}
 *       因此也帮不上忙。</li>
 *   <li>{@code FluidModel} 是 record（组件 final、无 setter），
 *       {@code FluidStateModelSet} 的 {@code modelByFluid} 是 private final map 且只有
 *       {@code get(FluidState)}；{@code ModelManager#getFluidStateModelSet()} 只读，
 *       {@code ModelBakery.BakingResult}（record）里也不含 {@code FluidStateModelSet}，
 *       所以 {@code ModelEvent.ModifyBakingResult} 同样够不到它。</li>
 *   <li>{@code RegisterColorHandlersEvent.BlockTintSources#register(List, Block...)} 只收方块；
 *       {@code ColorResolvers} 只能追加新的 {@code ColorResolver}，不能替换
 *       {@code BiomeColors.WATER_COLOR_RESOLVER}。</li>
 * </ol>
 *
 * <p>结论：NeoForge 26.1.2 <b>没有</b>受支持的 fluid tint 覆盖入口。本类因此不注册任何 fluid 模型
 * （重复注册会直接抛异常），只保留可用且已验证的方块路径。
 *
 * <p>要让水面真的跟着 {@link WaterColorEffectsManager#getWaterColor(BlockPos, int)} 变化，
 * 唯一可行的办法是回到 1.20.1 的注入点：{@code FluidTintSources.water()}（原版水模型的 tint source，
 * 见 {@code FluidTintSources$3}）的 {@code colorInWorld} 唯一做的事就是调用
 * {@code BiomeColors.getAverageWaterColor(level, pos)}；对
 * {@code BiomeColors#getAverageWaterColor(BlockAndTintGetter, BlockPos)} 做一次客户端 mixin
 * （{@code @Inject(at = @At("RETURN"), cancellable = true)} +
 * {@code CallbackInfoReturnable<Integer>}）即可同时覆盖方块路径与水面路径。
 * 但那样必须<b>同时</b>去掉 {@link EpcaWaterBlockTintSource} 里的 {@code applyEffect(...)}，
 * 否则同一次着色会被叠加两次（{@code getWaterColor} 的 {@code mixColors} 不幂等）。
 * 这属于新增 mixin／行为改动，本次未实施，仅在此记录。
 */
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
