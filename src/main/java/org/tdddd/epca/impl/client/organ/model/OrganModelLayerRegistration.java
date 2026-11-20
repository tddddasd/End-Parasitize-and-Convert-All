package org.tdddd.epca.impl.client.organ.model;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.tdddd.epca.impl.epca;

/**
 * 把 {@link OrganModelRenderLayer} 挂到 default / slim 两个玩家渲染器上。
 *
 * <p>26 的事件仍是 {@link EntityRenderersEvent.AddLayers}，但皮肤键类型变成了
 * {@link PlayerModelType}（原 1.20.1 是 {@code String}），渲染器类改名为 {@code AvatarRenderer}。</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class OrganModelLayerRegistration {

    private OrganModelLayerRegistration() {
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer == null) continue;
            renderer.addLayer(new OrganModelRenderLayer(renderer));
        }
    }
}
