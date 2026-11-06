package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

import java.util.List;
import java.util.UUID;

public class ContaminatedWater extends Entity {

    public static final int MAX_LIFETIME_TICKS = 200;

    private static final String TAG_LIFE_TICKS = "LifeTicks";

    private int lifeTicks = 0;
    private final UUID entityId;

    private static final EntityDataAccessor<Boolean> DUMMY = SynchedEntityData.defineId(ContaminatedWater.class, EntityDataSerializers.BOOLEAN);

    public ContaminatedWater(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.entityId = this.getUUID();

        this.setBoundingBox(this.getBoundingBox().inflate(2.5, 2.5, 2.5));

        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {

        entityData.define(DUMMY, false);
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;

        if (lifeTicks >= MAX_LIFETIME_TICKS) {

            this.discard();
            return;
        }

        if (!this.level().isClientSide() && lifeTicks % 10 == 0) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            DamageSources damageSources = serverLevel.damageSources();

            List<Entity> entities = serverLevel.getEntities(
                    this,
                    this.getBoundingBox(),
                    entity -> entity instanceof LivingEntity
            );

            for (Entity entity : entities) {
                LivingEntity livingEntity = (LivingEntity) entity;

                if (!ParasiteEvents.isParasiteByTagOrInterface(livingEntity) && livingEntity.isInWater()) {
                    applyDamageWithMinMechanic(livingEntity, damageSources);
                }
            }
        }
    }

    private void applyDamageWithMinMechanic(LivingEntity target, DamageSources damageSources) {
        Level level = target.level();
        Registry<DamageType> registry = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getOrThrow(ModDamageTypes.MINIMUM);
        DamageSource minimumSource = new DamageSource(holder);
        target.hurt(minimumSource, 0.1F);
        addParaCurseEffect(target);

    }

    private void addParaCurseEffect(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(
                ModEffects.PARA_CURSE,
                400,
                0,
                false,
                true,
                true
        ));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {

        this.lifeTicks = Math.max(0, input.getIntOr(TAG_LIFE_TICKS, 0));

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt(TAG_LIFE_TICKS, this.lifeTicks);

    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
