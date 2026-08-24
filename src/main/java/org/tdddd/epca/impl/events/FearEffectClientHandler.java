package org.tdddd.epca.impl.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = epca.MODID)
public class FearEffectClientHandler {

    
    private static final int SHAKE_INTERVAL = 2;
    
    private static final float BASE_AMPLITUDE = 0.8f;

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        var effectInstance = player.getEffect(ModEffects.FEAR.get()); 
        if (effectInstance == null) return;

        int amplifier = effectInstance.getAmplifier();
        float amplitude = BASE_AMPLITUDE * (amplifier + 1);

        
        if (++tickCounter >= SHAKE_INTERVAL) {
            tickCounter = 0;

            
            float deltaYaw = (float) ((Math.random() - 0.5) * 1.25 * amplitude);
            float deltaPitch = (float) ((Math.random() - 0.5) * 1.25 * amplitude); 

            
            player.setYRot(player.getYRot() + deltaYaw);
            float newPitch = player.getXRot() + deltaPitch;
            
            newPitch = Math.min(90.0F, Math.max(-90.0F, newPitch));
            player.setXRot(newPitch);

            
            player.yRotO = player.getYRot();
            player.xRotO = player.getXRot();
        }
    }
}