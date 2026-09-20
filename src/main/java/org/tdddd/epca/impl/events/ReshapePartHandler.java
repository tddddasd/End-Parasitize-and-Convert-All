package org.tdddd.epca.impl.events;

import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import org.tdddd.epca.impl.epca;

@EventBusSubscriber(modid = epca.MODID)
public class ReshapePartHandler {

    private static final String TARGET_ID = "epca:reshape_part";

    
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        
        if (event.getLevel().isClientSide()) {
            return;
        }

        Level level = event.getLevel();
        
        AABB originBox = new AABB(BlockPos.ZERO).inflate(0.001);

        level.getEntities(null, originBox).forEach(entity -> {
            
            Identifier rl = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (rl != null && rl.toString().equals(TARGET_ID)) {
                
                if (entity.getX() == 0.0D && entity.getY() == 0.0D && entity.getZ() == 0.0D) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                }
            }
        });
    }
}