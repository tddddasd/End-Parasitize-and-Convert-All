package org.tdddd.epca.impl.overworld.registry.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.tdddd.eej.api.AltarInteractionHandler;
import org.tdddd.eej.api.AltarItemContainer;
import org.tdddd.eej.api.AltarStructure;
import org.tdddd.eej.impl.altar.AbstractAltarBlock;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class EpcaAltarInteractionHandler implements AltarInteractionHandler {

    @Override
    public InteractionResult onAltarUse(Level level, BlockPos pos, BlockState state,
                                        Player player, InteractionHand hand, ItemStack heldItem) {
        
        if (tryPerformSacrifice(level, pos, player)) {
            return InteractionResult.SUCCESS;
        }

        
        if (heldItem.is(ModItems.KILL_STICK.get())) {
            if (!(state.getBlock() instanceof AbstractAltarBlock altarBlock)) {
                return InteractionResult.PASS;
            }
            AltarStructure data = altarBlock.findAltarStructure(level, pos);
            if (data == null || data.allPositions.isEmpty()) {
                player.displayClientMessage(Component.literal("当前方块不属于任何有效祭坛结构"), false);
                return InteractionResult.SUCCESS;
            }
            String status = data.isValid ? "有效" : "无效";
            player.displayClientMessage(Component.literal("总祭台数: " + data.pedestalCount), false);
            player.displayClientMessage(Component.literal("有物品的祭台数: " + data.pedestalsWithItem), false);
            player.displayClientMessage(Component.literal("总点数: " + data.totalPoints), false);
            player.displayClientMessage(Component.literal("状态: " + status), false);
            if (!data.isValid && data.invalidReason != null) {
                player.displayClientMessage(Component.literal("原因: " + data.invalidReason), false);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    

    private boolean tryPerformSacrifice(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return false;
        if (!(level instanceof ServerLevel serverLevel)) return false;

        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        if (!isBeaconBaseBlock(belowState)) return false;

        if (!(level.getBlockEntity(pos) instanceof AltarItemContainer pedestal)) return false;
        ItemStack stored = pedestal.getItem();
        if (!isGemBlockItem(stored)) return false;

        List<Animal> animals = new ArrayList<>();
        List<LivingEntity> villagers = new ArrayList<>();
        double radius = 1.5;
        List<Entity> entities = level.getEntities(null, new AABB(pos).inflate(radius));
        for (Entity e : entities) {
            if (e.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= radius * radius) {
                if (IParasite.isParasiteNoLivingByTagOrInterface(e)) {
                    return false;
                }
                if (!e.isAlive()) continue;
                if (e instanceof Animal && !(e instanceof Monster)) {
                    animals.add((Animal) e);
                } else if (isVillagerLike(e) && e instanceof LivingEntity) {
                    villagers.add((LivingEntity) e);
                }
            }
        }
        if (animals.size() < 2 || villagers.size() < 1) return false;

        performSacrifice(serverLevel, pos, player, animals, villagers);
        return true;
    }

    private void performSacrifice(ServerLevel level, BlockPos pos, Player player,
                                  List<Animal> animals, List<LivingEntity> villagers) {
        int animalRemoved = 0;
        for (Animal a : animals) {
            if (animalRemoved >= 2) break;
            a.remove(Entity.RemovalReason.DISCARDED);
            animalRemoved++;
        }
        if (!villagers.isEmpty()) {
            villagers.get(0).remove(Entity.RemovalReason.DISCARDED);
        }

        List<BlockPos> positions = new ArrayList<>();
        int maxRadius = 72;
        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dy = -maxRadius; dy <= maxRadius; dy++) {
                for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                    BlockPos targetPos = pos.offset(dx, dy, dz);
                    double dist = Math.sqrt(pos.distSqr(targetPos));
                    if (dist > maxRadius) continue;
                    BlockState state = level.getBlockState(targetPos);
                    if (state.isAir()) continue;
                    positions.add(targetPos);
                }
            }
        }

        positions.sort(Comparator.comparingDouble(p -> p.distSqr(pos)));

        if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal) {
            pedestal.clearItem();
        }

        BlockConversionManager.getInstance().addSacrificeTask(level, pos, player.getUUID(), positions);
    }

    private boolean isBeaconBaseBlock(BlockState state) {
        return state.is(Blocks.IRON_BLOCK) || state.is(Blocks.GOLD_BLOCK) ||
                state.is(Blocks.DIAMOND_BLOCK) || state.is(Blocks.EMERALD_BLOCK) ||
                state.is(Blocks.NETHERITE_BLOCK);
    }

    private boolean isGemBlockItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.DIAMOND_BLOCK || item == Items.EMERALD_BLOCK || item == Items.AMETHYST_BLOCK;
    }

    private boolean isVillagerLike(Entity entity) {
        return entity instanceof AbstractVillager || entity instanceof Pillager ||
                entity instanceof Witch || entity instanceof Vindicator ||
                entity instanceof Evoker;
    }
}
