package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedNetherseaBrandGrown;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedNetherseaBrandSolid;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import java.lang.reflect.Method;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.event.entity.living.LivingEvent;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InfestedNetherseaBrandEventHandler {
    private static boolean sanityAvailable = false;
    private static Method deductMethod = null;

    static {
        try {
            Class<?> clazz = Class.forName("net.mcreator.caerulaarbor.procedures.DeductPlayerSanityProcedure");
            deductMethod = clazz.getMethod("execute", Entity.class, double.class);
            sanityAvailable = true;
        } catch (Exception e) {
            // Ignore, sanity system not available
        }
    }

    private static void applySanityDamage(LivingEntity target, Level level, double amount) {
        if (!sanityAvailable || target == null || level == null) return;
        try {
            deductMethod.invoke(null, target, amount);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * 移除玩家身上的指定药水效果
     */
    private static void removeEffect(Player player, String modid, String path) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(modid, path));
        if (effect != null) {
            player.removeEffect(effect);
        }
    }

    private static void giveItem(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.spawnAtLocation(stack);
        }
    }

    // ========== 破坏虫染溟痕方块时的理智伤害 ==========
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (state.getBlock() instanceof InfestedNetherseaBrandGrown || state.getBlock() instanceof InfestedNetherseaBrandSolid) {
            LivingEntity livingEntity = event.getPlayer();
            if (livingEntity != null && !(IParasite.isParasiteByTagOrInterface(livingEntity))) {
                LevelAccessor levelAccessor = event.getLevel();
                if (levelAccessor instanceof Level level) {
                    applySanityDamage(livingEntity, level, 16 + level.random.nextInt(33));
                }
            }
        }
    }

    // ========== 食用物品事件 ==========
    @SubscribeEvent
    public static void onPlayerEat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // 处理虫染溟痕冰淇淋：每5tick扣5理智，共50点，并移除效果
        if (event.getItem().getItem() == ModItems.INFESTED_NETHERSEA_ICECREAM.get()) {
            if (sanityAvailable) {
                // 初始化玩家数据：剩余10次，冷却5tick
                var data = player.getPersistentData();
                data.putInt("icecream_damage_remaining", 10);
                data.putInt("icecream_damage_cooldown", 5);
            }
            // 移除两个药水效果
            removeEffect(player, "caerula_arbor", "frozen");
            removeEffect(player, "epca", "deep_sneak");

            giveItem(player, new ItemStack(ModItems.RESHAPE_SHELL.get()));

            return; // 冰淇淋处理完毕，不再执行后续
        }

        // 处理原有的虫染溟痕块（MOR）——一次性扣50理智，不处理效果
        if (event.getItem().getItem() == ModItems.INFESTED_NETHERSEA_BRAND_MOR.get() && sanityAvailable) {
            applySanityDamage(player, player.level(), 50.0);
        }
    }

    // ========== 玩家每Tick事件：处理冰淇淋持续伤害 ==========
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Player player)) return;

        var data = player.getPersistentData();
        int remaining = data.getInt("icecream_damage_remaining");
        if (remaining <= 0) {
            // 清除残留数据
            if (data.contains("icecream_damage_remaining")) {
                data.remove("icecream_damage_remaining");
                data.remove("icecream_damage_cooldown");
            }
            return;
        }

        // 冷却递减
        int cooldown = data.getInt("icecream_damage_cooldown");
        cooldown--;
        if (cooldown <= 0) {
            // 造成5点理智伤害
            applySanityDamage(player, player.level(), 5.0);
            remaining--;
            // 重置冷却为5tick
            cooldown = 5;
        }

        // 更新数据
        data.putInt("icecream_damage_remaining", remaining);
        data.putInt("icecream_damage_cooldown", cooldown);
    }
}