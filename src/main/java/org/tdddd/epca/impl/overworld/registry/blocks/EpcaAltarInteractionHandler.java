package org.tdddd.epca.impl.overworld.registry.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.tdddd.eej.api.AltarInteractionHandler;
import org.tdddd.eej.api.AltarItemContainer;
import org.tdddd.eej.api.AltarStructure;
import org.tdddd.eej.impl.altar.AbstractAltarBlock;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;


public class EpcaAltarInteractionHandler implements AltarInteractionHandler {

    
    private static final int RITUAL_SCAN_RADIUS = 72;
    
    private static final double RITUAL_SPHERE_RADIUS = 1.5;

    @Override
    public InteractionResult onAltarUse(Level level, BlockPos pos, BlockState state,
                                        Player player, InteractionHand hand, ItemStack heldItem) {
        
        
        
        
        
        
        
        
        
        
        if (!player.isShiftKeyDown() && heldItem.isEmpty()) {
            SacrificePlan plan = evaluateSacrifice(level, pos);
            if (plan.isOk()) {
                performSacrifice(plan.serverLevel(), pos, player, plan.animals(), plan.villagers());
                return InteractionResult.SUCCESS;
            }
            
        }

        
        if (heldItem.is(ModItems.KILL_STICK.get())) {
            if (!(state.getBlock() instanceof AbstractAltarBlock altarBlock)) {
                return InteractionResult.PASS;
            }
            AltarStructure data = altarBlock.findAltarStructure(level, pos);
            if (data == null || data.allPositions.isEmpty()) {
                sendTo(player, Component.translatable("altar_debug.epca.not_structure"));
                return InteractionResult.SUCCESS;
            }
            sendTo(player, Component.translatable("altar_debug.epca.pedestal_count", data.pedestalCount));
            sendTo(player, Component.translatable("altar_debug.epca.pedestals_with_item", data.pedestalsWithItem));
            sendTo(player, Component.translatable("altar_debug.epca.total_points", data.totalPoints));
            sendTo(player, Component.translatable(data.isValid
                    ? "altar_debug.epca.status.valid" : "altar_debug.epca.status.invalid"));
            if (!data.isValid && data.invalidReason != null) {
                sendTo(player, Component.translatable("altar_debug.epca.reason", data.invalidReason));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    

    
    public enum SacrificeCheck {
        OK,
        NO_BASE,
        NO_GEM,
        PARASITE_PRESENT,
        NOT_ENOUGH_ANIMALS,
        NOT_ENOUGH_VILLAGERS,
        ALREADY_RUNNING;

        public String langKey() {
            return "ritual.epca." + name().toLowerCase(Locale.ROOT);
        }
    }

    
    private record SacrificePlan(SacrificeCheck result, @Nullable ServerLevel serverLevel,
                                 List<Animal> animals, List<LivingEntity> villagers) {
        static SacrificePlan failed(SacrificeCheck result) {
            return new SacrificePlan(result, null, List.of(), List.of());
        }

        boolean isOk() {
            return result == SacrificeCheck.OK;
        }
    }

    
    private SacrificePlan evaluateSacrifice(Level level, BlockPos pos) {
        if (level.isClientSide()) return SacrificePlan.failed(SacrificeCheck.NO_BASE);
        if (!(level instanceof ServerLevel serverLevel)) return SacrificePlan.failed(SacrificeCheck.NO_BASE);

        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        if (!isBeaconBaseBlock(belowState)) return SacrificePlan.failed(SacrificeCheck.NO_BASE);

        if (!(level.getBlockEntity(pos) instanceof AltarItemContainer pedestal)) {
            return SacrificePlan.failed(SacrificeCheck.NO_GEM);
        }
        ItemStack stored = pedestal.getItem();
        if (!isGemBlockItem(stored)) return SacrificePlan.failed(SacrificeCheck.NO_GEM);

        List<Animal> animals = new ArrayList<>();
        List<LivingEntity> villagers = new ArrayList<>();
        AABB sphere = new AABB(pos).inflate(RITUAL_SPHERE_RADIUS);
        List<Entity> entities = level.getEntities(null, sphere);
        boolean parasitePresent = false;
        for (Entity e : entities) {
            if (e.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    <= RITUAL_SPHERE_RADIUS * RITUAL_SPHERE_RADIUS) {
                if (IParasite.isParasiteNoLivingByTagOrInterface(e)) {
                    
                    parasitePresent = true;
                    break;
                }
                if (!e.isAlive()) continue;
                if (e instanceof Animal animal && !(e instanceof Monster)) {
                    animals.add(animal);
                } else if (isVillagerLike(e) && e instanceof LivingEntity living) {
                    villagers.add(living);
                }
            }
        }
        if (parasitePresent) return SacrificePlan.failed(SacrificeCheck.PARASITE_PRESENT);
        if (animals.size() < 2) return SacrificePlan.failed(SacrificeCheck.NOT_ENOUGH_ANIMALS);
        if (villagers.isEmpty()) return SacrificePlan.failed(SacrificeCheck.NOT_ENOUGH_VILLAGERS);

        
        if (BlockConversionManager.getInstance().hasSacrificeTask(serverLevel, pos)) {
            return SacrificePlan.failed(SacrificeCheck.ALREADY_RUNNING);
        }

        
        return new SacrificePlan(SacrificeCheck.OK, serverLevel, animals, villagers);
    }

    
    private void performSacrifice(ServerLevel level, BlockPos pos, Player player,
                                  List<Animal> animals, List<LivingEntity> villagers) {
        int animalRemoved = 0;
        for (Animal a : animals) {
            if (animalRemoved >= 2) break;
            sacrificeFeedback(level, a);
            a.remove(Entity.RemovalReason.DISCARDED);
            animalRemoved++;
        }
        if (!villagers.isEmpty()) {
            LivingEntity victim = villagers.get(0);
            sacrificeFeedback(level, victim);
            victim.remove(Entity.RemovalReason.DISCARDED);
        }

        List<BlockPos> positions = new ArrayList<>();
        int maxRadius = RITUAL_SCAN_RADIUS;
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

        
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 1.0;
        double cz = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 20, 0.5, 0.5, 0.5, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz, 10, 0.4, 0.3, 0.4, 0.01);
        level.playSound(null, pos, SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.7F, 0.6F);

        boolean queued = false;
        if (player instanceof ServerPlayer serverPlayer) {
            queued = BlockConversionManager.getInstance().addSacrificeTask(level, pos, player.getUUID(), positions,
                    () -> {
                        sendTo(serverPlayer, Component.translatable(SacrificeCheck.ALREADY_RUNNING.langKey()));
                        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(),
                                SoundSource.BLOCKS, 0.6F, 0.5F);
                    });
        }
        if (!queued) {
            
            reportFailure(player, SacrificeCheck.ALREADY_RUNNING);
            return;
        }

        sendTo(player, Component.translatable("ritual.epca.started", positions.size()));
        for (ServerPlayer nearby : nearbyPlayers(level, pos, 32.0)) {
            if (nearby != player) {
                sendTo(nearby, Component.translatable("ritual.epca.started_nearby"));
            }
        }
    }

    
    private static void sacrificeFeedback(ServerLevel level, LivingEntity victim) {
        double x = victim.getX();
        double y = victim.getY() + victim.getBbHeight() * 0.5;
        double z = victim.getZ();
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 12, 0.35, 0.45, 0.35, 0.02);
        level.sendParticles(ModParticles.BLEEDING.get(), x, y, z, 6, 0.3, 0.3, 0.3, 0.01);
        level.playSound(null, x, y, z, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 0.9F, 0.8F);
    }

    private static List<ServerPlayer> nearbyPlayers(ServerLevel level, BlockPos pos, double radius) {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer candidate : level.players()) {
            if (candidate.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    <= radius * radius) {
                result.add(candidate);
            }
        }
        return result;
    }

    
    private static void reportFailure(Player player, SacrificeCheck check) {
        sendTo(player, Component.translatable(check.langKey()));
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(),
                    SoundSource.PLAYERS, 0.5F, 0.6F);
        }
    }

    private static boolean isBeaconBaseBlock(BlockState state) {
        return state.is(Blocks.IRON_BLOCK) || state.is(Blocks.GOLD_BLOCK) ||
                state.is(Blocks.DIAMOND_BLOCK) || state.is(Blocks.EMERALD_BLOCK) ||
                state.is(Blocks.NETHERITE_BLOCK);
    }

    private static boolean isGemBlockItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.DIAMOND_BLOCK || item == Items.EMERALD_BLOCK || item == Items.AMETHYST_BLOCK;
    }

    /**
     * 26.1.2: {@code Player#displayClientMessage(Component, boolean)} was deleted; that action-bar
     * variant now lives on {@code ServerPlayer#sendSystemMessage(Component, boolean)}. On the client
     * this is a no-op, matching the old behaviour (these messages only came from the server-side
     * altar interaction path).
     */
    private static void sendTo(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
        }
    }

    private static boolean isVillagerLike(Entity entity) {
        return entity instanceof AbstractVillager || entity instanceof Pillager ||
                entity instanceof Witch || entity instanceof Vindicator ||
                entity instanceof Evoker;
    }
}
