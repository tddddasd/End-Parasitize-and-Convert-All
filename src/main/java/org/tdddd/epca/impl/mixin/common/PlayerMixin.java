package org.tdddd.epca.impl.mixin.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;

import java.util.UUID;

@Mixin(Player.class)
public abstract class PlayerMixin implements IParasite {

    @Unique
    private boolean epca$isNestLeader() {
        return NestLeaderManager.isNestLeader(((Player)(Object)this).getUUID());
    }

    @Override
    public void onKillEntity(LivingEntity killedEntity) {
        if (!epca$isNestLeader()) return;
        IParasite.super.onKillEntity(killedEntity);
        ((Player)(Object)this).addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
    }

    @Override
    public void onDeath(DamageSource source) {
        if (!epca$isNestLeader()) return;
        IParasite.super.onDeath(source);
    }

    @Override
    public void onAttacked(LivingEntity attacker) {
        if (!epca$isNestLeader()) return;
        IParasite.super.onAttacked(attacker);
    }

    @Override
    public float onHurt(DamageSource source, float amount) {
        if (!epca$isNestLeader()) return amount;
        return IParasite.super.onHurt(source, amount);
    }

    @Override
    public boolean hasDamageAdaptationConfig() {
        if (!epca$isNestLeader()) return false;
        return IParasite.super.hasDamageAdaptationConfig();
    }

    @Override
    public DamageAdaptationConfig getDamageAdaptationConfig() {
        if (!epca$isNestLeader()) return null;
        return IParasite.super.getDamageAdaptationConfig();
    }

    @Override
    public boolean isDamageAdaptationInvulnerable() {
        if (!epca$isNestLeader()) return false;
        return IParasite.super.isDamageAdaptationInvulnerable();
    }

    @Override
    public boolean isFriendlyParasite(LivingEntity entity) {
        if (entity == (Object)this) return epca$isNestLeader();
        if (entity instanceof Player) {
            return NestLeaderManager.isNestLeader(entity.getUUID());
        }
        if (entity instanceof IParasite) {
            UUID follow = ((IParasite) entity).getFollowTarget();
            if (follow != null && follow.equals(((Player)(Object)this).getUUID())) return true;
        }
        return IParasite.super.isFriendlyParasite(entity);
    }

    @Override
    public boolean shouldIgnoreTarget(LivingEntity target) {
        if (!epca$isNestLeader()) return false;
        return IParasite.super.shouldIgnoreTarget(target);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return epca$isNestLeader();
    }

    @Override
    public void setParasite(boolean isParasite) {
    }

    @Override
    public CompoundTag getVariantData() {
        if (!epca$isNestLeader()) return new CompoundTag();
        return IParasite.super.getVariantData();
    }

    @Override
    public void setVariantData(CompoundTag data) {
        if (!epca$isNestLeader()) return;
        IParasite.super.setVariantData(data);
    }

    @Override
    public UUID getForcedTargetUuid() {
        if (!epca$isNestLeader()) return null;
        return IParasite.super.getForcedTargetUuid();
    }

    @Override
    public void setForcedTargetUuid(UUID uuid) {
        if (!epca$isNestLeader()) return;
        IParasite.super.setForcedTargetUuid(uuid);
    }

    @Override
    public LivingEntity getForcedTarget(ServerLevel level) {
        if (!epca$isNestLeader()) return null;
        return IParasite.super.getForcedTarget(level);
    }

    @Override
    public void setForcedTarget(LivingEntity target, ServerLevel level) {
        if (!epca$isNestLeader()) return;
        IParasite.super.setForcedTarget(target, level);
    }

    @Override
    public long getLastForcedSwitchTick() {
        if (!epca$isNestLeader()) return 0;
        return IParasite.super.getLastForcedSwitchTick();
    }

    @Override
    public void setLastForcedSwitchTick(long tick) {
        if (!epca$isNestLeader()) return;
        IParasite.super.setLastForcedSwitchTick(tick);
    }

    @Override
    public boolean isForcedTargetSwitchOnCooldown(Level level) {
        if (!epca$isNestLeader()) return true;
        return IParasite.super.isForcedTargetSwitchOnCooldown(level);
    }

    @Override
    public boolean trySwitchForcedTargetOnAttacked(LivingEntity attacker) {
        if (!epca$isNestLeader()) return false;
        return IParasite.super.trySwitchForcedTargetOnAttacked(attacker);
    }
}