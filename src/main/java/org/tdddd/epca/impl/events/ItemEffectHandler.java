package org.tdddd.epca.impl.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModItems;

@Mod.EventBusSubscriber
public class ItemEffectHandler {

    private static final int EFFECT_DURATION = 600; 
    private static final int CHECK_INTERVAL = 300;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity.tickCount % CHECK_INTERVAL != 0) return;

        checkAndApplyEffects(entity);
    }

    private static void checkAndApplyEffects(LivingEntity entity) {
        boolean hasInfested = false;

        if (entity instanceof Player player) {
            // 玩家检查整个背包
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && isInfestedItem(stack)) {
                    hasInfested = true;
                    break;
                }
            }
        } else {
            // 非玩家生物检查主手、副手、盔甲栏
            for (ItemStack stack : entity.getArmorSlots()) {
                if (!stack.isEmpty() && isInfestedItem(stack)) {
                    hasInfested = true;
                    break;
                }
            }
            if (!hasInfested) {
                ItemStack mainHand = entity.getMainHandItem();
                if (!mainHand.isEmpty() && isInfestedItem(mainHand)) {
                    hasInfested = true;
                }
            }
            if (!hasInfested) {
                ItemStack offHand = entity.getOffhandItem();
                if (!offHand.isEmpty() && isInfestedItem(offHand)) {
                    hasInfested = true;
                }
            }
        }

        if (hasInfested) {
            applyCOTHEffect(entity);
        }
    }

    private static boolean isInfestedItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.is(ModItems.INFESTED_RUBBISH.get())) return true;
        if (stack.is(ModItems.INFESTED_STICK.get())) return true;
        if (stack.is(ModItems.INFESTED_DIRT.get())) return true;
        if (stack.is(ModItems.INFESTED_LOG.get())) return true;
        if (stack.is(ModItems.INFESTED_GRASS.get())) return true;
        if (stack.is(ModItems.INFESTED_FERN.get())) return true;
        if (stack.is(ModItems.INFESTED_SWEET_BERRY_BUSH.get())) return true;
        if (stack.is(ModItems.PARASITE_VISCERA.get())) return true;
        if (stack.is(ModItems.DISEASED_HEART.get())) return true;
        if (stack.is(ModItems.FINS_FIN.get())) return true;
        if (stack.is(ModItems.INFESTED_BONE.get())) return true;
        if (stack.is(ModItems.WEIRD_MINCED_FLESH.get())) return true;
        if (stack.is(ModItems.INFESTED_FLESH.get())) return true;
        if (stack.is(ModItems.BECKON_MEMBRANE.get())) return true;
        if (stack.is(ModItems.INFESTED_LEAVES.get())) return true;
        if (stack.is(ModItems.INFESTED_FLOWERING_LEAVES.get())) return true;
        if (stack.is(ModItems.INFESTED_VINE.get())) return true;
        if (stack.is(ModItems.INFESTED_STRIPPED_LOG.get())) return true;
        if (stack.is(ModItems.INFESTED_PLANKS.get())) return true;
        if (stack.is(ModItems.INFESTED_PLANKS_SLAB.get())) return true;
        if (stack.is(ModItems.INFESTED_PLANKS_STAIRS.get())) return true;
        if (stack.is(ModItems.INFESTED_PLANKS_FENCE.get())) return true;
        if (stack.is(ModItems.INFESTED_SLIME_BALL.get())) return true;
        if (stack.is(ModItems.INFESTED_SAND.get())) return true;
        if (stack.is(ModItems.RESHAPE_FLESH.get())) return true;
        if (stack.is(ModItems.RESHAPE_SHELL.get())) return true;
        if (stack.is(ModItems.TWISTED_BONE.get())) return true;
        if (stack.is(ModItems.TIGHT_TENDONS.get())) return true;
        if (stack.is(ModItems.GASBAG_DEBRIS.get())) return true;
        if (stack.is(ModItems.INFESTED_WOOD.get())) return true;
        if (stack.is(ModItems.INFESTED_STRIPPED_WOOD.get())) return true;
        if (stack.is(ModItems.INFESTED_STONE.get())) return true;
        if (stack.is(ModItems.INFESTED_STONE_SLAB.get())) return true;
        if (stack.is(ModItems.INFESTED_STONE_STAIRS.get())) return true;
        if (stack.is(ModItems.INFESTED_STONE_WALL.get())) return true;
        if (stack.is(ModItems.INFESTED_COBBLESTONE.get())) return true;
        if (stack.is(ModItems.INFESTED_COBBLESTONE_SLAB.get())) return true;
        if (stack.is(ModItems.INFESTED_COBBLESTONE_STAIRS.get())) return true;
        if (stack.is(ModItems.INFESTED_COBBLESTONE_WALL.get())) return true;
        if (stack.is(ModItems.INFESTED_STONE_BRICKS.get())) return true;
        if (stack.is(ModItems.INFESTED_STONE_BRICKS_SLAB.get())) return true;
        if (stack.is(ModItems.INFESTED_STONE_BRICKS_STAIRS.get())) return true;
        if (stack.is(ModItems.INFESTED_STONE_BRICKS_WALL.get())) return true;
        if (stack.is(ModItems.INFESTED_CRACKED_STONE_BRICKS.get())) return true;
        if (stack.is(ModItems.INFESTED_CHISELED_STONE_BRICKS.get())) return true;
        if (stack.is(ModItems.INFESTED_POLISHED_STONE.get())) return true;
        if (stack.is(ModItems.INFESTED_POLISHED_STONE_SLAB.get())) return true;
        if (stack.is(ModItems.INFESTED_POLISHED_STONE_STAIRS.get())) return true;
        if (stack.is(ModItems.INFESTED_SANDSTONE.get())) return true;
        if (stack.is(ModItems.INFESTED_SANDSTONE_SLAB.get())) return true;
        if (stack.is(ModItems.INFESTED_SANDSTONE_STAIRS.get())) return true;
        if (stack.is(ModItems.INFESTED_SANDSTONE_WALL.get())) return true;
        if (stack.is(ModItems.INFESTED_CHISELED_RED_SANDSTONE.get())) return true;
        if (stack.is(ModItems.INFESTED_CHISELED_SANDSTONE.get())) return true;
        if (stack.is(ModItems.INFESTED_SMOOTH_SANDSTONE.get())) return true;
        if (stack.is(ModItems.INFESTED_SMOOTH_SANDSTONE_SLAB.get())) return true;
        if (stack.is(ModItems.INFESTED_SMOOTH_SANDSTONE_STAIRS.get())) return true;
        if (stack.is(ModItems.INFESTED_CUT_SANDSTONE.get())) return true;
        if (stack.is(ModItems.INFESTED_CUT_SANDSTONE_SLAB.get())) return true;
        if (stack.is(ModItems.INFESTED_COAL.get())) return true;
        if (stack.is(ModItems.INFESTED_RAW_COPPER.get())) return true;
        if (stack.is(ModItems.INFESTED_RAW_IRON.get())) return true;
        if (stack.is(ModItems.INFESTED_RAW_GOLD.get())) return true;
        if (stack.is(ModItems.INFESTED_LAPIS_LAZULI.get())) return true;
        if (stack.is(ModItems.INFESTED_EMERALD.get())) return true;
        if (stack.is(ModItems.INFESTED_REDSTONE.get())) return true;
        if (stack.is(ModItems.INFESTED_DIAMOND.get())) return true;
        if (stack.is(ModItems.INFESTED_COAL_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_COPPER_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_IRON_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_GOLD_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_LAPIS_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_REDSTONE_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_EMERALD_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_DIAMOND_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_SNOW.get())) return true;
        if (stack.is(ModItems.INFESTED_SNOW_BLOCK.get())) return true;
        if (stack.is(ModItems.INFESTED_INFESTED_COBBLESTONE.get())) return true;
        if (stack.is(ModItems.INFESTED_INFESTED_STONE.get())) return true;
        if (stack.is(ModItems.INFESTED_INFESTED_STONE_BRICKS.get())) return true;
        if (stack.is(ModItems.INFESTED_INFESTED_CRACKED_STONE_BRICKS.get())) return true;
        if (stack.is(ModItems.INFESTED_INFESTED_CHISELED_STONE_BRICKS.get())) return true;
        if (stack.is(ModItems.INFESTED_NETHERSEA_BRAND_GROWN.get())) return true;
        if (stack.is(ModItems.INFESTED_NETHERSEA_BRAND_SOLID.get())) return true;
        if (stack.is(ModItems.INFESTED_NETHERSEA_BRAND_MOR.get())) return true;
        if (stack.is(ModItems.INFESTED_NETHERSEA_ICECREAM.get())) return true;
        if (stack.is(ModItems.INFESTED_ENDER_PEARL.get())) return true;
        if (stack.is(ModItems.INFESTED_POINTED_DRIPSTONE.get())) return true;
        if (stack.is(ModItems.BECKON_CORE.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_STONE.get())) return true;
        if (stack.is(ModItems.INFESTED_INFESTED_HEAVY_STONE.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_COAL_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_COPPER_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_IRON_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_GOLD_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_LAPIS_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_REDSTONE_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_EMERALD_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_DIAMOND_ORE.get())) return true;
        if (stack.is(ModItems.INFESTED_DUSTLIKE.get())) return true;
        if (stack.is(ModItems.INFESTED_PLANKSLIKE.get())) return true;
        if (stack.is(ModItems.INFESTED_ROCKLIKE.get())) return true;
        if (stack.is(ModItems.INFESTED_METALLIKE.get())) return true;
        if (stack.is(ModItems.INFESTED_HARDLIKE.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_COBBLESTONE.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_COBBLESTONE_STAIRS.get())) return true;
        if (stack.is(ModItems.ENDER_BLADE_SCRAP.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_COBBLESTONE_SLAB.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_COBBLESTONE_WALL.get())) return true;
        if (stack.is(ModItems.INFESTED_CHISELED_DEEPSLATE.get())) return true;
        if (stack.is(ModItems.INFESTED_POLISHED_HEAVY_STONE.get())) return true;
        if (stack.is(ModItems.INFESTED_POLISHED_HEAVY_STONE_STAIRS.get())) return true;
        if (stack.is(ModItems.INFESTED_POLISHED_HEAVY_STONE_SLAB.get())) return true;
        if (stack.is(ModItems.INFESTED_POLISHED_HEAVY_STONE_WALL.get())) return true;
        if (stack.is(ModItems.INFESTED_SWEET_BERRIES.get())) return true;
        if (stack.is(ModItems.INFESTED_LILY_PAD.get())) return true;
        if (stack.is(ModItems.INFESTED_CRACKED_HEAVY_BRICKS.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_BRICKS.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_BRICKS_SLAB.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_BRICKS_STAIRS.get())) return true;
        if (stack.is(ModItems.INFESTED_HEAVY_BRICKS_WALL.get())) return true;
        if (stack.is(ModItems.INFESTED_CARVED_PUMPKIN.get())) return true;
        if (stack.is(ModItems.INFESTED_PUMPKIN.get())) return true;
        if (stack.is(ModItems.INFESTED_TALL_GRASS.get())) return true;
        if (stack.is(ModItems.INFESTED_TALL_FERN.get())) return true;
        if (stack.is(ModItems.INFESTED_SHORT_GRASS.get())) return true;
        if (stack.is(ModItems.INFESTED_CACTUS.get())) return true;
        if (stack.is(ModItems.INFESTED_SUGAR_CANE.get())) return true;

        return false;
    }

    private static void applyCOTHEffect(LivingEntity entity) {
        MobEffectInstance current = entity.getEffect(ModEffects.COTH.get());
        if (current == null) {
            entity.addEffect(new MobEffectInstance(
                    ModEffects.COTH.get(),
                    EFFECT_DURATION,
                    0,
                    false,
                    true,
                    true
            ));
        }
    }
}