package org.tdddd.epca.impl.overworld.raid;

import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;

import java.util.List;

@EventBusSubscriber(modid = epca.MODID)
public final class InfestedRaidBarrier {

    public static final int PULSE_TICKS = 10;

    public static final float MAGIC_DAMAGE = 4.0F;
    public static final float FIRE_DAMAGE = 2.0F;

    private static final List<net.neoforged.neoforge.registries.DeferredHolder<MobEffect, MobEffect>> STRIPPED =
            List.of(ModEffects.PARA_CURSE, ModEffects.CONTEMPT_INORGANIC, ModEffects.SYMBIOSIS_LIE);

    private static final String IMMUNE_UNTIL = "epca:barrier_immune_until";

    private static final int IMMUNE_TICKS = 200;

    private static boolean magicPhase;

    private InfestedRaidBarrier() { }

    public static void pulse(ServerLevel level) {
        InfestedRaidBarrierData data = InfestedRaidBarrierData.get(level);
        if (data.prune()) {
            org.tdddd.epca.impl.network.packet.s2c.RaidBarrierS2CPacket.sync(level, data);
        }
        List<InfestedRaidBarrierData.Barrier> active = data.activeIn(level);
        if (active.isEmpty()) return;

        magicPhase = !magicPhase;
        for (InfestedRaidBarrierData.Barrier barrier : active) {
            applyPulse(level, barrier);
        }
    }

    private static void applyPulse(ServerLevel level, InfestedRaidBarrierData.Barrier barrier) {
        BlockPos center = barrier.center();
        double radius = barrier.radius();
        AABB box = AABB.ofSize(Vec3.atCenterOf(center), radius * 2.0D, radius * 2.0D, radius * 2.0D);
        double radiusSq = radius * radius;

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!entity.isAlive()) continue;
            if (entity.position().distanceToSqr(Vec3.atCenterOf(center)) > radiusSq) continue;
            handleInsideLiving(level, entity, center);
        }

        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
            if (!isRemovable(entity)) continue;
            if (entity.position().distanceToSqr(Vec3.atCenterOf(center)) > radiusSq) continue;
            entity.discard();
        }

        AABB shell = AABB.ofSize(Vec3.atCenterOf(center), (radius + 8.0D) * 2.0D, (radius + 8.0D) * 2.0D,
                (radius + 8.0D) * 2.0D);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, shell)) {
            if (entity instanceof Player) continue;
            if (!isBlockedKind(entity)) continue;
            pushOut(entity, center, radius);
        }
    }

    private static void handleInsideLiving(ServerLevel level, LivingEntity entity, BlockPos center) {
        if (entity instanceof Player player) {
            if (HiveLeaderManager.isHiveLeader(player.getUUID())) {
                damage(level, player, center);
                return;
            }

            if (player instanceof ServerPlayer serverPlayer) bless(level, serverPlayer);
            return;
        }

        if (InfestedPurification.isInfested(entity.getType())) {
            InfestedPurification.purify(entity);
            return;
        }

        if (ParasiteEvents.isParasiteEntity(entity) || ParasiteEvents.isParasiteByTagOrInterface(entity)) {
            damage(level, entity, center);
            if (entity instanceof Mob mob) pushOut(mob, center, InfestedRaidBarrierData.RADIUS);
            return;
        }

        if (entity instanceof Mob mob) {
            stripAndShield(level, mob);
        }
    }

    private static void damage(ServerLevel level, LivingEntity entity, BlockPos center) {
        DamageSource source = magicPhase
                ? level.damageSources().magic()
                : level.damageSources().onFire();
        entity.hurt(source, magicPhase ? MAGIC_DAMAGE : FIRE_DAMAGE);
    }

    private static void stripAndShield(ServerLevel level, LivingEntity entity) {
        for (var holder : STRIPPED) {
            entity.removeEffect(holder);
        }
        entity.getPersistentData().putLong(IMMUNE_UNTIL, level.getGameTime() + IMMUNE_TICKS);

        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true, true));
    }

    private static void bless(ServerLevel level, ServerPlayer player) {
        stripAndShield(level, player);

        player.addEffect(new MobEffectInstance(ModEffects.SOUL_PROTECTION, 20 * 60 * 30, 4, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 60 * 20, 1, false, true, true));

        MobEffectInstance hero = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        if (hero == null || hero.getDuration() < 20 * 60 * 40) {
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 20 * 60 * 40, 0, false, true, true));
        }
    }

    private static boolean isRemovable(Entity entity) {
        return entity.getType() == ModEntities.BIOMASS_EGG.get()
                || entity.getType() == ModEntities.BONE_ARROW.get()
                || entity.getType() == ModEntities.BONE_FRAGMENT.get()
                || entity.getType() == ModEntities.ACID_BULLET.get()
                || entity.getType() == ModEntities.CONTAMINATED_WATER.get();
    }

    private static boolean isBlockedKind(LivingEntity entity) {
        if (ParasiteEvents.isParasiteEntity(entity) || ParasiteEvents.isParasiteByTagOrInterface(entity)) return true;
        return entity instanceof Enemy;
    }

    private static void pushOut(LivingEntity entity, BlockPos center, double radius) {
        Vec3 c = Vec3.atCenterOf(center);
        Vec3 offset = entity.position().subtract(c);
        double dist = offset.length();
        if (dist > radius) return;
        Vec3 dir = dist < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : offset.scale(1.0D / dist);
        Vec3 target = c.add(dir.scale(radius + 1.5D));
        entity.teleportTo(target.x, target.y, target.z);
        entity.setDeltaMovement(dir.x * 0.35D, 0.1D, dir.z * 0.35D);
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) return;
        MobEffect effect = instance.getEffect().value();
        boolean watched = effect == ModEffects.PARA_CURSE.value()
                || effect == ModEffects.CONTEMPT_INORGANIC.value()
                || effect == ModEffects.SYMBIOSIS_LIE.value();
        if (!watched) return;
        LivingEntity entity = event.getEntity();
        long until = entity.getPersistentData().getLong(IMMUNE_UNTIL).orElse(0L);
        if (entity.level().getGameTime() < until) {
            entity.removeEffect(instance.getEffect());
        }
    }
}
