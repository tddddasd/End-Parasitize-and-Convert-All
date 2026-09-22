package org.tdddd.epca.impl.client.render.sky;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;

/**
 * 世界结界破损的<b>绘制时机</b>。
 *
 * <p>挂在 Forge 的 {@code RenderLevelStageEvent.Stage.AFTER_SKY} 上 —— 这是
 * {@code LevelRenderer.renderLevel()} 里天光刚画完、地形还没画的那个瞬间
 * （已核对字节码：{@code renderSky} 在 438，AFTER_SKY 派发在 441，
 * 第一个 {@code renderChunkLayer} 在 557）。</p>
 *
 * <p>为什么必须在这里画，见 {@link SkyRuptureRenderer} 的类文档
 * 「⚠️ 为什么不能等世界画完再画」—— 简单说：原版云会把深度写满整块云几何
 * （包括全透明纹素），云平面外边界投影出来是一条<b>笔直的水平线</b>，
 * 画在它后面就会被这条线劈成两半。</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SkyRuptureStageListener {

    private SkyRuptureStageListener() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            SkyRuptureRenderer.renderAfterSky();
        }
    }
}
