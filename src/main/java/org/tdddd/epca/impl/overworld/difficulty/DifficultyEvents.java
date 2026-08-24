package org.tdddd.epca.impl.overworld.difficulty;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DifficultyEvents {
    // ===== 定时刷新 Buff =====
    private static final Map<UUID, Long> lastApplyTick = new ConcurrentHashMap<>();
    private static final int REFRESH_INTERVAL = 29 * 20; // 29 秒（tick）
    private static final int BUFF_DURATION = 30 * 20;    // 30 秒（tick）
    
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
                if (extraChance > 0 && level.random.nextFloat() < extraChance) {
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
        // ===== 传说难度：额外掉落两份掉落物（非玩家实体） =====
        Level level = event.getEntity().level();
        if (!level.isClientSide && DifficultyEffects.isLegendary(level)) {
            LivingEntity entity = event.getEntity();
            // 排除玩家
            if (entity instanceof Player) return;
            // 如果需要，也可以排除寄生体？但需求说“所有生物”，可包括寄生体，但不排除
            // 但原逻辑已处理寄生体，如果寄生体也享受，则叠加。暂且不排除

            // 复制当前掉落物，每个物品额外添加两份
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

    // 玩家tick事件，定期刷新buff
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
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
        // 幸运 II（amplifier=1）
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, BUFF_DURATION, 1, false, false, true));
        // SoulProtection II（amplifier=1）
        player.addEffect(new MobEffectInstance(ModEffects.SOUL_PROTECTION.get(), BUFF_DURATION, 1, false, false, true));
    }
}