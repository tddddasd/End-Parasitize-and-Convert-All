package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

import java.util.List;
import java.util.UUID;

public class ContaminatedWater extends Entity {

    /**
     * Natural end of life of this entity, in ticks: 200 ticks = 10 seconds.
     *
     * <p>This is the single, explicit removal rule of the entity. It replaces the old bare
     * {@code lifeTicks >= 200} literal, so the number is named and can be quoted anywhere.</p>
     */
    public static final int MAX_LIFETIME_TICKS = 200;

    /**
     * NBT key of {@link #lifeTicks}. The counter is persisted like a vanilla item's {@code Age}, so a
     * chunk unload or a world reload cannot restart the 10 second clock and keep the entity alive
     * forever.
     */
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
    protected void defineSynchedData() {
        
        this.entityData.define(DUMMY, false);
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;

        
        if (lifeTicks >= MAX_LIFETIME_TICKS) {
            // Natural end of life. The discard is deliberately run on BOTH sides: the server copy is
            // the authoritative one (it is the one that damages and that unloads with the world), but
            // the client copy is the one that is actually rendered - and it is also the only copy
            // that still ticks when the server-side chunk is loaded but not entity-ticking (render
            // distance > simulation distance), where the server copy would freeze mid-life and the
            // effect would stay on screen forever. Removing the client copy here is always safe: once
            // the server does remove its own copy, the removal packet is a no-op on the client.
            this.discard();
            return;
        }

        if (!this.level().isClientSide && lifeTicks % 10 == 0) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            DamageSources damageSources = serverLevel.damageSources();

            List<Entity> entities = serverLevel.getEntities(
                    this,
                    this.getBoundingBox(),
                    entity -> entity instanceof LivingEntity
            );

            for (Entity entity : entities) {
                LivingEntity livingEntity = (LivingEntity) entity;
                
                if (!IParasite.isParasiteByTagOrInterface(livingEntity) && livingEntity.isInWater()) {
                    applyDamageWithMinMechanic(livingEntity, damageSources);
                }
            }
        }
    }

    
    private void applyDamageWithMinMechanic(LivingEntity target, DamageSources damageSources) {
        Level level = target.level();
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
        DamageSource minimumSource = new DamageSource(holder);
        target.hurt(minimumSource, 0.1F);
        addParaCurseEffect(target);
        
    }


    
    private void addParaCurseEffect(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(
                ModEffects.PARA_CURSE.get(),
                400,  
                0,    
                false,
                true,
                true
        ));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        // Restore the remaining lifetime instead of restarting it at zero.
        this.lifeTicks = Math.max(0, compoundTag.getInt(TAG_LIFE_TICKS));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putInt(TAG_LIFE_TICKS, this.lifeTicks);
    }

    
    @Override
    public boolean isAttackable() {
        return false;
    }
}