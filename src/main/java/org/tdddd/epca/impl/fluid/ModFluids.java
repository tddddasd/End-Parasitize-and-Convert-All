package org.tdddd.epca.impl.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModItems;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, epca.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, epca.MODID);

    
    public static final DeferredHolder<FluidType, FluidType> ACID_SOLUTION_FLUID_TYPE = FLUID_TYPES.register(
            "acid_solution",
            AcidSolutionType::new
    );

    
    public static final DeferredHolder<Fluid, FlowingFluid> ACID_SOLUTION = FLUIDS.register(
            "acid_solution",
            () -> new AcidSolutionFluid.Source(createAcidSolutionProperties())
    );

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_ACID_SOLUTION = FLUIDS.register(
            "flowing_acid_solution",
            () -> new AcidSolutionFluid.Flowing(createAcidSolutionProperties())
    );

    
    private static final BlockBehaviour.Properties ACID_SOLUTION_BLOCK_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN)
            .replaceable()
            .noCollision()
            .strength(100.0F)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid();

    
    private static final Item.Properties ACID_SOLUTION_BUCKET_PROPERTIES = new Item.Properties()
            .craftRemainder(net.minecraft.world.item.Items.BUCKET)
            .stacksTo(1);

    
    private static BaseFlowingFluid.Properties createAcidSolutionProperties() {
        return new BaseFlowingFluid.Properties(
                ACID_SOLUTION_FLUID_TYPE,
                ACID_SOLUTION,
                FLOWING_ACID_SOLUTION
        )
                .bucket(() -> ModItems.ACID_SOLUTION_BUCKET.get())
                .block(() -> ModBlocks.ACID_SOLUTION_BLOCK.get())
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .explosionResistance(100F)
                .tickRate(20);
    }

    /**
     * 酸液的客户端外观。
     *
     * <p><b>26.1.2 改动</b>：1.20.1 在 {@code AcidSolutionType#initializeClient} 里返回
     * {@code getStillTexture() / getFlowingTexture() / getTintColor()}。26.1.2 的
     * {@code IClientFluidTypeExtensions} 只剩雾效相关的默认方法，贴图 / 染色 / 渲染层统一由
     * {@link net.minecraft.client.renderer.block.FluidModel} 描述，并在模组总线事件
     * {@code RegisterFluidModelsEvent} 里注册。原贴图与颜色的语义被完整保留：
     * <ul>
     *   <li>still   = {@code epca:block/acid}</li>
     *   <li>flowing = {@code epca:block/acid_move}</li>
     *   <li>tint    = {@code 0xFFFFFFFF}（原来的 {@code TINT_COLOR}，等价于不染色）</li>
     *   <li>overlay = 原实现没有覆盖层贴图，这里同样传 {@code null}
     *       （{@code FluidModel.Unbaked#bake} 对 null overlay 直接跳过）</li>
     *   <li>layer   = 不用手填：{@code bake} 按贴图透明度算成 {@code ChunkSectionLayer.TRANSLUCENT}，
     *       与原 1.20.1 流体渲染器的半透明表现一致</li>
     * </ul>
     *
     * <p>本类是纯客户端类（{@code FluidModel}/{@code Material}），用
     * {@code @EventBusSubscriber(value = Dist.CLIENT)} 包成静态内部类，保证专用服务器上 FML
     * 不会扫描、也就不会加载它（见 WAVE1-BRIEF 运行时事实 3）。
     */
    @net.neoforged.fml.common.EventBusSubscriber(
            modid = epca.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class ClientFluidModels {

        private ClientFluidModels() {
        }

        /** 原 {@code AcidSolutionType#TINT_COLOR}。 */
        private static final int TINT_COLOR = 0xFFFFFFFF;

        @net.neoforged.bus.api.SubscribeEvent
        public static void onRegisterFluidModels(
                net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
            event.register(
                    new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                            new net.minecraft.client.resources.model.sprite.Material(
                                    epca.asResource("block/acid")),
                            new net.minecraft.client.resources.model.sprite.Material(
                                    epca.asResource("block/acid_move")),
                            null,
                            net.neoforged.neoforge.client.fluid.FluidTintSources.constant(TINT_COLOR),
                            null),
                    ACID_SOLUTION,
                    FLOWING_ACID_SOLUTION);
        }
    }
}
