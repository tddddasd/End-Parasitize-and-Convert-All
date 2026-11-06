package org.tdddd.epca.impl.network.packet.c2s;

import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.events.BiomassEventHandler;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModParticles;

import javax.annotation.Nullable;

public final class HiveLeaderDecomposeParasiteHandler {

    private HiveLeaderDecomposeParasiteHandler() {
    }

    public static void handleRequest(@Nullable ServerPlayer player) {
        if (player == null) return;

        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        LivingEntity target = pickParasite(player);

        if (target == null) return;

        int points = BiomassEventHandler.computeParasiteBiomass(target);

        boolean biomassSpawned = ParasiteEvents.isParasiteEntity(target) && ParasiteEvents.isBiomassSpawned(target);
        if (!biomassSpawned) {
            dropLootTableOnly(serverLevel, target, player);
        }

        target.discard();

        serverLevel.sendParticles(ModParticles.BIOMASS.get(),
                target.getX(), target.getY() + 0.5D, target.getZ(),
                points, 0.5D, 0.5D, 0.5D, 0.05D);

        BiomassEventHandler.addPointsAndSync(player, points);
        player.sendSystemMessage(Component.literal("分解寄生体：+" + points + " 生物质点数"), true);
    }

    @Nullable
    public static LivingEntity pickParasite(ServerPlayer player) {

        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach <= 0.0D) return null;

        Vec3 from = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 to = from.add(look.x * reach, look.y * reach, look.z * reach);

        BlockHitResult blockHit = player.level().clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            to = blockHit.getLocation();
        }

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, from, to, searchBox,
                entity -> !entity.isSpectator() && entity.isPickable(),
                reach * reach);
        if (hit == null) return null;

        Entity picked = hit.getEntity();
        if (!(picked instanceof LivingEntity living)) return null;
        if (picked instanceof Player) return null;
        if (!ParasiteEvents.isParasiteEntity(living)) return null;
        if (!living.isAlive()) return null;
        if (living.level() != player.level()) return null;
        return living;
    }

    private static void dropLootTableOnly(ServerLevel serverLevel, LivingEntity target, Player decomposer) {
        DamageSource source = decomposer.damageSources().playerAttack(decomposer);

        target.getLootTable().ifPresent(key ->
                target.dropFromLootTable(serverLevel, source, true, key));
    }
}
