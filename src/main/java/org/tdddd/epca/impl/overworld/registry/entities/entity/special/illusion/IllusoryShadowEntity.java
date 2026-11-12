package org.tdddd.epca.impl.overworld.registry.entities.entity.special.illusion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Purely cosmetic "phantom" that the {@link IllusoryButterflyEntity} overlaps with itself during the
 * past / future phases. It has no model or texture of its own: the client renderer draws a coloured
 * wireframe cage sized 1.1x the butterfly, tinted purple-grey for the past and white for the future.
 *
 * <p>The entity is server driven: the butterfly spawns it, keeps its position locked to its own
 * position every tick and removes it as soon as the phase changes.</p>
 */
public class IllusoryShadowEntity extends Entity {

    /** 0 = none, 1 = past (purple grey), 2 = future (white). Kept in sync so the renderer can pick a look. */
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(IllusoryShadowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(IllusoryShadowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(IllusoryShadowEntity.class, EntityDataSerializers.INT);

    public static final int COLOR_PAST = 0x8E82A8;
    public static final int COLOR_FUTURE = 0xFFFFFF;

    public static final int KIND_PAST = 1;
    public static final int KIND_FUTURE = 2;

    public IllusoryShadowEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_COLOR, COLOR_FUTURE);
        this.entityData.define(DATA_SCALE, 1.1F);
        this.entityData.define(DATA_KIND, KIND_FUTURE);
    }

    public int color() {
        return this.entityData.get(DATA_COLOR);
    }

    public float scale() {
        return this.entityData.get(DATA_SCALE);
    }

    public int kind() {
        return this.entityData.get(DATA_KIND);
    }

    public void configure(int kind, int color, float scale) {
        this.entityData.set(DATA_KIND, kind);
        this.entityData.set(DATA_COLOR, color);
        this.entityData.set(DATA_SCALE, scale);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("ShadowColor")) {
            this.entityData.set(DATA_COLOR, tag.getInt("ShadowColor"));
        }
        if (tag.contains("ShadowScale")) {
            this.entityData.set(DATA_SCALE, tag.getFloat("ShadowScale"));
        }
        if (tag.contains("ShadowKind")) {
            this.entityData.set(DATA_KIND, tag.getInt("ShadowKind"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ShadowColor", this.color());
        tag.putFloat("ShadowScale", this.scale());
        tag.putInt("ShadowKind", this.kind());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
