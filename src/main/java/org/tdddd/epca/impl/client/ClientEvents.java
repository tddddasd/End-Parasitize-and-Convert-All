package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.client.effect.ArayaBgmManager;
import org.tdddd.epca.impl.client.effect.ArayaBossBgmManager;
import org.tdddd.epca.impl.client.effect.ArayaClientCache;
import org.tdddd.epca.impl.client.effect.ArayaSlashClientCache;
import org.tdddd.epca.impl.client.effect.SacrificeRitualClientCache;
import org.tdddd.epca.impl.client.effect.SoulProtectionClientCache;
import org.tdddd.epca.impl.client.entity.gas.GasCloudManager;
import org.tdddd.epca.impl.client.entity.heart.SoulProtectionHeartRenderer;
import org.tdddd.epca.impl.client.entity.layer.EndermanAfterimageLayer;
import org.tdddd.epca.impl.client.organ.HiveLeaderOrganClientInput;
import org.tdddd.epca.impl.client.render.araya.ArayaSceneCopy;
import org.tdddd.epca.impl.epca;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static int tickCounter = 0;

    private static ClientLevel ritualLevel;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % 20 == 0) {
            EndermanAfterimageLayer.cleanupOrphaned();
        }

        GasCloudManager.clientTick();

        SoulProtectionHeartRenderer.clientTick();

        SoulProtectionClientCache.clientTick();

        ClientLevel level = Minecraft.getInstance().level;
        if (level != ritualLevel) {
            ritualLevel = level;
            SacrificeRitualClientCache.clear();

            ArayaClientCache.clear();
            ArayaSlashClientCache.clear();
            ArayaBgmManager.stop();
            ArayaBgmManager.invalidateAssetCache();

            ArayaBossBgmManager.stop();
            ArayaBossBgmManager.invalidateAssetCache();
            ArayaSceneCopy.invalidate();
        }
        SacrificeRitualClientCache.clientTick();
        ArayaClientCache.clientTick(level == null ? 0L : level.getGameTime());
        ArayaSlashClientCache.clientTick(level == null ? 0L : level.getGameTime());

        ArayaBgmManager.clientTick();

        ArayaBossBgmManager.clientTick();

        HiveLeaderOrganClientInput.tick();
    }
}
