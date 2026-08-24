package org.tdddd.epca.impl.overworld.difficulty;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.epca;
import net.minecraft.nbt.CompoundTag;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class DifficultyApplier {

    
    private static final String DIFFICULTY_APPLIED_TAG = "DifficultyApplied";

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity && entity instanceof IParasite parasite) {
            CompoundTag persistentData = entity.getPersistentData();
            
            if (!persistentData.getBoolean(DIFFICULTY_APPLIED_TAG)) {
                parasite.applyDifficultyStatModifiers();
                persistentData.putBoolean(DIFFICULTY_APPLIED_TAG, true);
            }
        }
    }
}