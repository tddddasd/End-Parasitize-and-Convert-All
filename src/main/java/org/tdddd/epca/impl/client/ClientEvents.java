package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.client.effect.SacrificeRitualClientCache;
import org.tdddd.epca.impl.client.effect.SoulProtectionClientCache;
import org.tdddd.epca.impl.client.entity.gas.GasCloudManager;
import org.tdddd.epca.impl.client.entity.heart.SoulProtectionHeartRenderer;
import org.tdddd.epca.impl.client.entity.layer.EndermanAfterimageLayer;
import org.tdddd.epca.impl.epca;

/**
 * Forge-bus client subscribers only.
 *
 * <p>{@code TickEvent.ClientTickEvent} is a forge-bus event, so this class deliberately keeps the
 * default bus. Mod-bus client events (renderer registration, the custom core shader registration
 * and client setup) live in {@link ClientHandler}, which is annotated
 * {@code @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)}.
 * Mixing the two would silently drop the mod-bus handlers, because
 * {@code net.minecraftforge.client.event.RegisterShadersEvent} implements
 * {@code net.minecraftforge.fml.event.IModBusEvent} and is therefore never posted to the forge bus.</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static int tickCounter = 0;

    /** Level the ritual cache belongs to; a change resets it, so no old altar survives a teleport. */
    private static ClientLevel ritualLevel;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        GasCloudManager.clientTick();
        // Ages the per-entity fade state of the epca:soul_protection flame; the flame and its embers
        // are submitted from the LivingEntityRenderer hook, not from here.
        SoulProtectionHeartRenderer.clientTick();
        // Advances the local countdown of the soul-protection sync cache, so the flame also fades out
        // smoothly between two of the server's batches.
        SoulProtectionClientCache.clientTick();
        // Ages the sacrifice-ritual aura: the renderer reads it, this only drives the fade in/out.
        ClientLevel level = Minecraft.getInstance().level;
        if (level != ritualLevel) {
            ritualLevel = level;
            SacrificeRitualClientCache.clear();
        }
        SacrificeRitualClientCache.clientTick();
        if (tickCounter % 20 == 0) {
            EndermanAfterimageLayer.cleanupOrphaned();
        }
    }
}
