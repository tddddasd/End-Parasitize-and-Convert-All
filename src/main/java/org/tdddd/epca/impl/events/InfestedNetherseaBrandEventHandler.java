package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedNetherseaBrandGrown;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedNetherseaBrandSolid;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import java.lang.reflect.Method;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

@EventBusSubscriber(modid = epca.MODID)
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

    
    private static void removeEffect(Player player, String modid, String path) {
        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(Identifier.fromNamespaceAndPath(modid, path)).orElse(null);
        if (effect != null) {
            player.removeEffect(effect);
        }
    }

    private static void giveItem(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack) && player.level() instanceof ServerLevel serverLevel) {
            player.spawnAtLocation(serverLevel, stack);
        }
    }

    
    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        BlockState state = event.getState();
        if (state.getBlock() instanceof InfestedNetherseaBrandGrown || state.getBlock() instanceof InfestedNetherseaBrandSolid) {
            LivingEntity livingEntity = event.getPlayer();
            if (livingEntity != null && !(IParasite.isParasiteByTagOrInterface(livingEntity))) {
                LevelAccessor levelAccessor = event.getLevel();
                if (levelAccessor instanceof Level level) {
                    applySanityDamage(livingEntity, level, 16 + level.getRandom().nextInt(33));
                }
            }
        }
    }

    
    @SubscribeEvent
    public static void onPlayerEat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        
        if (event.getItem().getItem() == ModItems.INFESTED_NETHERSEA_ICECREAM.get()) {
            if (sanityAvailable) {
                
                var data = player.getPersistentData();
                data.putInt("icecream_damage_remaining", 10);
                data.putInt("icecream_damage_cooldown", 5);
            }
            
            removeEffect(player, "caerula_arbor", "frozen");
            removeEffect(player, "epca", "deep_sneak");

            giveItem(player, new ItemStack(ModItems.RESHAPE_SHELL.get()));

            return; 
        }

        
        if (event.getItem().getItem() == ModItems.INFESTED_NETHERSEA_BRAND_MOR.get() && sanityAvailable) {
            applySanityDamage(player, player.level(), 50.0);
        }
    }

    
    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        var data = player.getPersistentData();
        int remaining = data.getInt("icecream_damage_remaining").orElse(0);
        if (remaining <= 0) {
            
            if (data.contains("icecream_damage_remaining")) {
                data.remove("icecream_damage_remaining");
                data.remove("icecream_damage_cooldown");
            }
            return;
        }

        
        int cooldown = data.getInt("icecream_damage_cooldown").orElse(0);
        cooldown--;
        if (cooldown <= 0) {
            
            applySanityDamage(player, player.level(), 5.0);
            remaining--;
            
            cooldown = 5;
        }

        
        data.putInt("icecream_damage_remaining", remaining);
        data.putInt("icecream_damage_cooldown", cooldown);
    }
}