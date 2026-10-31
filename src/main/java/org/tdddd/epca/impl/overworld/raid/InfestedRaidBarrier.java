package org.tdddd.epca.impl.overworld.raid;

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
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.List;

/**
 * The raid-victory barrier: a 64 block sphere that purifies parasites, shields the living and blocks
 * parasite reinforcements for one real hour.
 *
 * <p>Everything happens on a 10 tick (0.5 s) pulse so the numbers in the spec line up exactly
 * ("每 0.5 秒 4 魔法伤害 / 2 火焰伤害，交替").</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID)
public final class InfestedRaidBarrier {
    /** Pulse period in ticks (0.5 s). */
    public static final int PULSE_TICKS = 10;
    /** Damage of the alternating pulses. */
    public static final float MAGIC_DAMAGE = 4.0F;
    public static final float FIRE_DAMAGE = 2.0F;

    /**
     * Effects that are stripped from every non-parasite inside the barrier.
     *
     * <p>Deliberately stores the {@link RegistryObject}s, not their values: this class is an
     * {@code @Mod.EventBusSubscriber}, so Forge loads it (running the static initialisers) while the
     * registries are still being populated. Calling {@code .get()} here threw
     * {@code NullPointerException: Registry Object not present: epca:para_curse} and aborted mod
     * construction, so the values are resolved per use instead.</p>
     */
    private static final List<RegistryObject<MobEffect>> STRIPPED = List.of(
            ModEffects.PARA_CURSE,
            ModEffects.CONTEMPT_INORGANIC,
            ModEffects.SYMBIOSIS_LIE);

    /** Persistent-data key: game time until which the barrier immunity lasts. */
    private static final String IMMUNE_UNTIL = "epca:barrier_immune_until";
    /** How long the immunity lasts after the last pulse touched the entity. */
    private static final int IMMUNE_TICKS = 200;

    private static boolean magicPhase;

    private InfestedRaidBarrier() { }

    /** Called from the raid manager on a 10 tick cadence for every level that has barriers. */
    public static void pulse(ServerLevel level) {
        InfestedRaidBarrierData data = InfestedRaidBarrierData.get(level);
        if (data.prune()) {
            org.tdddd.epca.impl.network.packet.s2c.RaidBarrierS2CPacket.sendAll(level, data);
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

        // 1) Living entities inside.
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!entity.isAlive()) continue;
            if (entity.position().distanceToSqr(Vec3.atCenterOf(center)) > radiusSq) continue;
            handleInsideLiving(level, entity, center);
        }

        // 2) Projectiles and other removable parasite clutter inside.
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
            if (!isRemovable(entity)) continue;
            if (entity.position().distanceToSqr(Vec3.atCenterOf(center)) > radiusSq) continue;
            entity.discard();
        }

        // 3) Keep parasites and hostile mobs out of the sphere.
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
            // Only server players can receive the barrier buffs.
            if (player instanceof ServerPlayer serverPlayer) bless(level, serverPlayer);
            return;
        }

        // Infested species -> back to the creature it was converted from.
        if (InfestedPurification.isInfested(entity.getType())) {
            InfestedPurification.purify(entity);
            return;
        }

        // Parasites that are not part of the infested family get burned out of the barrier.
        if (entity instanceof IParasite || IParasite.isParasiteByTagOrInterface(entity)) {
            damage(level, entity, center);
            if (entity instanceof Mob mob) pushOut(mob, center, InfestedRaidBarrierData.RADIUS);
            return;
        }

        if (entity instanceof Mob mob) {
            stripAndShield(level, mob);
        }
    }

    /** Alternating magic / fire damage, exactly one of the two per pulse. */
    private static void damage(ServerLevel level, LivingEntity entity, BlockPos center) {
        DamageSource source = magicPhase
                ? level.damageSources().magic()
                : level.damageSources().onFire();
        entity.hurt(source, magicPhase ? MAGIC_DAMAGE : FIRE_DAMAGE);
    }

    private static void stripAndShield(ServerLevel level, LivingEntity entity) {
        for (RegistryObject<MobEffect> effect : STRIPPED) {
            entity.removeEffect(effect.get());
        }
        entity.getPersistentData().putLong(IMMUNE_UNTIL, level.getGameTime() + IMMUNE_TICKS);
        // Continuous Regeneration I, 5 s, refreshed every pulse.
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true, true));
    }

    /** Non-nest-leader player blessing: cleansing, immunity, and the vanilla-raid style rewards. */
    private static void bless(ServerLevel level, ServerPlayer player) {
        stripAndShield(level, player);
        // 30 min Soul Protection V, 20 min Strength II (refreshed while the player stays inside).
        player.addEffect(new MobEffectInstance(ModEffects.SOUL_PROTECTION.get(), 20 * 60 * 30, 4, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60 * 20, 1, false, true, true));
        // Village hero, same shape as the vanilla raid reward (40 min, level I).
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

    /** Parasites and hostile mobs are kept outside; infested creatures are purified instead. */
    private static boolean isBlockedKind(LivingEntity entity) {
        if (entity instanceof IParasite || IParasite.isParasiteByTagOrInterface(entity)) return true;
        return entity instanceof Enemy;
    }

    /** Teleports/flings an entity back to the outside of the shell. */
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

    /**
     * Keeps the three cleansing targets off the entity while the immunity window is up: the effect is
     * applied and immediately removed.
     */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) return;
        MobEffect effect = instance.getEffect();
        boolean watched = effect == ModEffects.PARA_CURSE.get()
                || effect == ModEffects.CONTEMPT_INORGANIC.get()
                || effect == ModEffects.SYMBIOSIS_LIE.get();
        if (!watched) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().getGameTime() < entity.getPersistentData().getLong(IMMUNE_UNTIL)) {
            entity.removeEffect(effect);
        }
    }
}
