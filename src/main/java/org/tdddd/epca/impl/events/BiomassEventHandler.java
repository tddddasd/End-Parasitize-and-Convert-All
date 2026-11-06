package org.tdddd.epca.impl.events;

import org.tdddd.epca.impl.events.ParasiteEvents;
import org.tdddd.epca.impl.overworld.registry.ModTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.BiomassSyncPacket;
import org.tdddd.epca.impl.overworld.data.BiomassManager;
import org.tdddd.epca.impl.overworld.data.EntityBiomassCountManager;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.entities.*;
import org.tdddd.epca.impl.epca;

import java.util.Random;

@EventBusSubscriber(modid = epca.MODID)
public class BiomassEventHandler {
    private static final long COOLDOWN_TICKS = 200;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof Player player && HiveLeaderManager.isHiveLeader(player.getUUID())) {
            LivingEntity killed = event.getEntity();
            if (!ParasiteEvents.isParasiteEntity(killed)) {
                addPointsAndSync(player, 1);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;
        if (!player.getMainHandItem().isEmpty()) return;
        if (!player.isShiftKeyDown()) return;

        LivingEntity target = event.getEntity();
        if (!ParasiteEvents.isParasiteEntity(target) || target instanceof Player) return;

        Level level = player.level();
        if (level.isClientSide()) return;

        long lastInteract = player.getPersistentData().getLong("LastParasiteInteract").orElse(0L);
        long now = level.getGameTime();
        if (now - lastInteract < COOLDOWN_TICKS) {
            return;
        }

        int basePoints = computeParasiteBiomass(target);

        target.remove(Entity.RemovalReason.DISCARDED);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.BIOMASS.get(),
                    target.getX(), target.getY() + 0.5, target.getZ(),
                    basePoints, 0.5, 0.5, 0.5, 0.05);
        }

        addPointsAndSync(player, basePoints);
        player.sendSystemMessage(Component.literal("获得 " + basePoints + " 生物质点数"));

        player.getPersistentData().putLong("LastParasiteInteract", now);
    }

    public static int computeParasiteBiomass(LivingEntity target) {
        int basePoints = 1;
        int biomassCount = EntityBiomassCountManager.getCurrentBiomassCount(target);
        Random rand = new Random();

        if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).is(org.tdddd.epca.impl.overworld.registry.ModTags.INFESTED)) {
            basePoints = 2 + rand.nextInt(3) + biomassCount;
        } else if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).is(org.tdddd.epca.impl.overworld.registry.ModTags.ONESENT)) {
            basePoints = 1 + rand.nextInt(2) + biomassCount;
        } else if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).is(org.tdddd.epca.impl.overworld.registry.ModTags.POVERTY)) {
            basePoints = 2 + rand.nextInt(2) + biomassCount;
        } else if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).is(org.tdddd.epca.impl.overworld.registry.ModTags.RESHAPE)) {
            basePoints = 12 + 4 + rand.nextInt(3) + biomassCount;
        } else if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).is(org.tdddd.epca.impl.overworld.registry.ModTags.LINK)) {
            basePoints = 4 + biomassCount;
        }
        return basePoints;
    }

    public static void addPointsAndSync(Player player, int amount) {

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        BiomassManager.addBiomassPoints(player, amount);
        int total = BiomassManager.getBiomassPoints(player);
        ModNetwork.sendToPlayer(serverPlayer,
                new BiomassSyncPacket(true, total));
    }

    public static void syncBiomass(Player player) {
        if (player instanceof ServerPlayer sp) {
            boolean isLeader = HiveLeaderManager.isHiveLeader(player.getUUID());
            int points = isLeader ? BiomassManager.getBiomassPoints(player) : 0;
            ModNetwork.sendToPlayer(sp, new BiomassSyncPacket(isLeader, points));
        }
    }
}
