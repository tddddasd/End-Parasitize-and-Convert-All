package org.tdddd.epca.impl.overworld.difficulty;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.epca;
import net.minecraft.nbt.CompoundTag;

@EventBusSubscriber(modid = epca.MODID)
public class DifficultyApplier {
    private static final String DIFFICULTY_APPLIED_TAG = "DifficultyApplied";

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();

        if (entity instanceof Player player && HiveLeaderManager.isHiveLeader(player.getUUID())) {
            // Players are no longer IParasite; the leader stat bonus is applied through the world data hooks.
            CompoundTag persistentData = player.getPersistentData();
            if (!persistentData.getBoolean(DIFFICULTY_APPLIED_TAG).orElse(false)) {
                org.tdddd.epca.impl.overworld.data.HiveLeaderPlayerHooks.applyDifficultyStatModifiers(player);
                persistentData.putBoolean(DIFFICULTY_APPLIED_TAG, true);
            }
        }

        if (entity instanceof LivingEntity && entity instanceof IParasite parasite && !(entity instanceof Player)) {
            CompoundTag persistentData = entity.getPersistentData();
            
            if (!persistentData.getBoolean(DIFFICULTY_APPLIED_TAG).orElse(false)) {
                parasite.applyDifficultyStatModifiers();
                persistentData.putBoolean(DIFFICULTY_APPLIED_TAG, true);
            }
        }
    }
}