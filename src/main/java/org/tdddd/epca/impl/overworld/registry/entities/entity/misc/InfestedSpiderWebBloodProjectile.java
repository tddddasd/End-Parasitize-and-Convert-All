package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedSpiderWebBlood;

public class InfestedSpiderWebBloodProjectile extends ThrowableItemProjectile {
    public InfestedSpiderWebBloodProjectile(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.INFESTED_SPIDER_WEB_BLOOD_PROJECTILE.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
    }

    @Override
    public boolean isNoGravity() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide()) {

            tryPlaceWeb(webTargetFor(result));
            this.discard();
        }
        super.onHitEntity(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide()) {
            BlockState hitState = this.level().getBlockState(result.getBlockPos());
            if (hitState.getFluidState().isEmpty()) {
                tryPlaceWeb(this.blockPosition());
                this.discard();
            }
        }
        super.onHitBlock(result);
    }

    private BlockPos webTargetFor(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity living
                && !ParasiteEvents.isParasiteByTagOrInterface(living)) {
            BlockPos feet = living.blockPosition();
            if (this.level().getBlockState(feet).canBeReplaced()) {
                return feet;
            }
            BlockPos below = feet.below();
            if (this.level().getBlockState(below).canBeReplaced()) {
                return below;
            }
        }
        return this.blockPosition();
    }

    private void tryPlaceWeb(BlockPos pos) {
        Level level = this.level();
        BlockState state = level.getBlockState(pos);
        if (state.canBeReplaced()) {
            BlockState web = ModBlocks.INFESTED_SPIDER_WEB_BLOOD.get().defaultBlockState();
            level.setBlock(pos, InfestedSpiderWebBlood.markSpiderWeb(web, true), 3);
        }
    }
}
