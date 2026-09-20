package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ReshapePartHandler {

    private static final String TARGET_ID = "epca:reshape_part";

    
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) {
            return;
        }

        Level level = event.level;
        
        AABB originBox = new AABB(BlockPos.ZERO).inflate(0.001);

        level.getEntities(null, originBox).forEach(entity -> {
            
            ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (rl != null && rl.toString().equals(TARGET_ID)) {
                
                if (entity.getX() == 0.0D && entity.getY() == 0.0D && entity.getZ() == 0.0D) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                }
            }
        });
    }
}