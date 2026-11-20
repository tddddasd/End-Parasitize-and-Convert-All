package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.client.effect.ArayaBgmManager;
import org.tdddd.epca.impl.client.effect.ArayaBossBgmManager;
import org.tdddd.epca.impl.client.effect.ArayaClientCache;
import org.tdddd.epca.impl.client.effect.ArayaSlashClientCache;
import org.tdddd.epca.impl.client.effect.BioTortClientState;
import org.tdddd.epca.impl.client.effect.SacrificeRitualClientCache;
import org.tdddd.epca.impl.client.effect.SoulProtectionClientCache;
import org.tdddd.epca.impl.client.entity.gas.GasCloudManager;
import org.tdddd.epca.impl.client.entity.heart.SoulProtectionHeartRenderer;
import org.tdddd.epca.impl.client.entity.layer.EndermanAfterimageLayer;
import org.tdddd.epca.impl.client.render.araya.ArayaSceneCopy;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static int tickCounter = 0;

    private static ClientLevel ritualLevel;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        GasCloudManager.clientTick();

        SoulProtectionHeartRenderer.clientTick();

        SoulProtectionClientCache.clientTick();

        ClientLevel level = Minecraft.getInstance().level;
        if (level != ritualLevel) {
            ritualLevel = level;

            // 换维度 / 换世界：1.20.1 每次切维度都会新建一个 ClientLevel，所以这里是可靠的变化点。
            // 屏障是 DIMENSION 目标、只在创建/移除时广播，换维度后手上还是旧维度的数据，先丢掉；
            // 服务端 InfestedRaidManager#onChangedDimension 会马上把目标维度的列表单发过来。
            org.tdddd.epca.impl.client.render.ClientRaidBarriers.onDimensionChanged(
                    level == null ? null : level.dimension().location());

            SacrificeRitualClientCache.clear();

            ArayaClientCache.clear();
            ArayaSlashClientCache.clear();
            ArayaBgmManager.stop();
            ArayaBgmManager.invalidateAssetCache();

            ArayaBossBgmManager.stop();
            ArayaBossBgmManager.invalidateAssetCache();
            ArayaSceneCopy.invalidate();

            BioTortClientState.clear();
        }
        SacrificeRitualClientCache.clientTick();
        ArayaClientCache.clientTick(level == null ? 0L : level.getGameTime());
        ArayaSlashClientCache.clientTick(level == null ? 0L : level.getGameTime());

        BioTortClientState.clientTick();

        ArayaBgmManager.clientTick();

        ArayaBossBgmManager.clientTick();
        if (tickCounter % 20 == 0) {
            EndermanAfterimageLayer.cleanupOrphaned();
        }
    }

}
