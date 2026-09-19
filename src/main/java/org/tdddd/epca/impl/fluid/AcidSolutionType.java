package org.tdddd.epca.impl.fluid;

import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * 酸液流体类型。
 *
 * <p><b>26.1.2 改动</b>：{@code FluidType#initializeClient(Consumer&lt;IClientFluidTypeExtensions&gt;)}
 * 连同 {@code IClientFluidTypeExtensions#getStillTexture/getFlowingTexture/getTintColor} 一起被删除。
 * 26.1.2 把流体贴图 / 染色 / 渲染层搬进了 {@code FluidModel}，改由模组总线事件
 * {@code RegisterFluidModelsEvent} 注册（见 {@code ModFluids.ClientFluidModels}）。
 * 这里只保留服务端也需要的 {@link FluidType.Properties}。
 *
 * <p><b>为什么必须 isWaterLike(true)</b>：1.20.1 Forge 的 {@code LivingEntity#travel} 只要
 * {@code isInFluidType((type, h) -> canSwimInFluidType(type))} 成立就按水处理，而
 * {@code canSwim} 默认 true，所以自定义流体天然获得水中移动。26.1.2 把这件事改成了<b>显式开关</b>：
 * <ul>
 *   <li>{@code Entity#updateFluidInteraction} 用 {@code fluidType.getIsWaterLike()} 决定
 *       {@code wasTouchingWater}，也就是 {@code isInWater()} 的值；</li>
 *   <li>{@code LivingEntity#travelInFluid} 先问 {@code FluidType#move(entity, movement, gravity)}，
 *       其默认实现<b>恒返回 false</b>；只有返回 true 才会接管移动，否则回落到
 *       {@code isInWater() ? travelInWater : isInLava() ? travelInLava : 什么都不做}。</li>
 * </ul>
 * 不打开这个开关时，酸液里 {@code isInWater()} 与 {@code isInLava()} 都为 false，于是
 * {@code travelInFluid} 两个分支都不执行——流体里没有任何阻力 / 浮力 / 游泳物理，
 * 生物只能沿用陆地的惯性，表现为「酸液中的移速不正常」。{@code motionScale} 在 26.1.2 已不再被
 * 引擎读取（NeoForge 全库 0 处引用），所以它不能替代这个开关。
 */
public class AcidSolutionType extends FluidType {
    public AcidSolutionType() {
        super(Properties.create()
                .descriptionId("block.epca.acid_solution")
                .fallDistanceModifier(0F)
                .canExtinguish(true)
                .canConvertToSource(false)
                .supportsBoating(true)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                .motionScale(0.007D)
                .canHydrate(false)
                .lightLevel(0)
                .density(3000)
                .temperature(1300)
                .viscosity(6000)
                // 26.1.2: 必须显式声明，否则酸液里没有任何流体移动物理（见类注释）。
                .isWaterLike(true));
    }
}
