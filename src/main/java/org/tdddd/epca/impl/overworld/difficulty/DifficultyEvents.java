package org.tdddd.epca.impl.overworld.difficulty;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = epca.MODID)
public class DifficultyEvents {
    
    private static final Map<UUID, Long> lastApplyTick = new ConcurrentHashMap<>();
    private static final int REFRESH_INTERVAL = 29 * 20; 
    private static final int BUFF_DURATION = 30 * 20;    
    
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (IParasite.isParasiteByTagOrInterface(event.getEntity())) {
            LivingEntity parasite = event.getEntity();
            ServerLevel level = (ServerLevel) parasite.level();

            if (parasite instanceof Player) {
                return;
            }

            
            if (!DifficultyEffects.shouldDropLoot(level)) {
                event.getDrops().clear();
                return;
            }

            if (DifficultyEffects.isRewardEnabled(level)) {
                
                float extraChance = DifficultyEffects.getExtraLootChance(level);
                if (extraChance > 0 && level.getRandom().nextFloat() < extraChance) {
                    List<ItemEntity> extraDrops = new ArrayList<>();
                    for (ItemEntity drop : event.getDrops()) {
                        ItemStack stack = drop.getItem().copy();
                        if (!stack.isEmpty()) {
                            extraDrops.add(new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), stack));
                        }
                    }
                    event.getDrops().addAll(extraDrops);
                }

                
                float multiplier = DifficultyEffects.getExtraLootMultiplier(level);
                if (multiplier > 1.0f) {
                    for (ItemEntity drop : event.getDrops()) {
                        ItemStack stack = drop.getItem();
                        int newCount = (int) (stack.getCount() * multiplier);
                        stack.setCount(newCount);
                    }
                }
            }
        }
        
        Level level = event.getEntity().level();
        if (!level.isClientSide() && DifficultyEffects.isLegendary(level)) {
            LivingEntity entity = event.getEntity();
            
            if (entity instanceof Player) return;
            
            

            
            List<ItemEntity> drops = (List<ItemEntity>) event.getDrops();
            List<ItemEntity> additionalDrops = new ArrayList<>();
            for (ItemEntity drop : drops) {
                ItemStack stack = drop.getItem();
                if (!stack.isEmpty()) {
                    ItemStack copy1 = stack.copy();
                    ItemStack copy2 = stack.copy();
                    additionalDrops.add(new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), copy1));
                    additionalDrops.add(new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), copy2));
                }
            }
            event.getDrops().addAll(additionalDrops);
        }
    }

    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) return;
        if (!DifficultyEffects.isCothEffectEnabled(level)) return;

        UUID uuid = player.getUUID();
        long currentTick = level.getGameTime();
        Long lastTick = lastApplyTick.get(uuid);
        if (lastTick == null || currentTick - lastTick >= REFRESH_INTERVAL) {
            applyBuffs(player);
            lastApplyTick.put(uuid, currentTick);
        }
    }

    private static void applyBuffs(Player player) {
        
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, BUFF_DURATION, 1, false, false, true));
        
        player.addEffect(new MobEffectInstance(ModEffects.SOUL_PROTECTION, BUFF_DURATION, 1, false, false, true));
    }
}