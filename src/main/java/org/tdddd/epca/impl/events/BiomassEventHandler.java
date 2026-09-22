package org.tdddd.epca.impl.events;

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
import org.tdddd.epca.impl.overworld.data.EntityKillCountManager;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
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
        if (source.getEntity() instanceof Player player && NestLeaderManager.isNestLeader(player.getUUID())) {
            LivingEntity killed = event.getEntity();
            if (!(killed instanceof IParasite)) {
                addPointsAndSync(player, 1);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;
        if (!player.getMainHandItem().isEmpty()) return;
        if (!player.isShiftKeyDown()) return;

        LivingEntity target = event.getEntity();
        if (!(target instanceof IParasite) || target instanceof Player) return;

        Level level = player.level();
        if (level.isClientSide()) return;

        long lastInteract = player.getPersistentData().getLong("LastParasiteInteract").orElse(0L);
        long now = level.getGameTime();
        if (now - lastInteract < COOLDOWN_TICKS) {
            return;
        }

        int basePoints = 1;
        int killCount = EntityKillCountManager.getCurrentKillCount(target);
        Random rand = new Random();

        if (target instanceof IInfested) {
            basePoints = 2 + rand.nextInt(3) + killCount;
        } else if (target instanceof IOnesent) {
            basePoints = 1 + rand.nextInt(2) + killCount;
        } else if (target instanceof IPoverty) {
            basePoints = 2 + rand.nextInt(2) + killCount;
        } else if (target instanceof IReshape) {
            basePoints = 12 + 4 + rand.nextInt(3) + killCount;
        } else if (target instanceof ILink) {
            basePoints = 4 + killCount;
        }

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

    private static void addPointsAndSync(Player player, int amount) {
        
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
            boolean isLeader = NestLeaderManager.isNestLeader(player.getUUID());
            int points = isLeader ? BiomassManager.getBiomassPoints(player) : 0;
            ModNetwork.sendToPlayer(sp, new BiomassSyncPacket(isLeader, points));
        }
    }
}
