package org.tdddd.epca.impl.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.client.effect.SoulProtectionClientCache;
import org.tdddd.epca.impl.client.entity.gas.GasCloudManager;
import org.tdddd.epca.impl.client.entity.heart.SoulProtectionHeartRenderer;
import org.tdddd.epca.impl.client.entity.layer.EndermanAfterimageLayer;
import org.tdddd.epca.impl.epca;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % 20 == 0) {
            EndermanAfterimageLayer.cleanupOrphaned();
        }
        // Ages the shader-rendered gas clouds and reads the synced jet-skill state of the visible
        // reshape mobs. Client-only visual state; no packets, no server-side changes.
        GasCloudManager.clientTick();
        // Ages the per-entity fade state of the soul-protection flame, exactly like the 1.20.1 twin.
        SoulProtectionHeartRenderer.clientTick();
        // Ages the server-synced "which entities carry soul protection" cache: level-change clear plus
        // the local countdown of the durations the server reported.
        SoulProtectionClientCache.clientTick();
    }
}
