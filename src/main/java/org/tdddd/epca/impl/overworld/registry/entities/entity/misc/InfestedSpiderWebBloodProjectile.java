package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedCaveSpiderWeb;

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
    protected void defineSynchedData() {
        super.defineSynchedData();
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
        if (!this.level().isClientSide) {
            tryPlaceWeb(this.blockPosition());
            this.discard();
        }
        super.onHitEntity(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            BlockState hitState = this.level().getBlockState(result.getBlockPos());
            if (hitState.getFluidState().isEmpty()) {
                tryPlaceWeb(this.blockPosition());
                this.discard();
            }
        }
        super.onHitBlock(result);
    }

    private void tryPlaceWeb(BlockPos pos) {
        Level level = this.level();
        BlockState state = level.getBlockState(pos);
        if (state.canBeReplaced()) {
            level.setBlock(pos,
                    ModBlocks.INFESTED_SPIDER_WEB_BLOOD.get().defaultBlockState()
                            .setValue(InfestedCaveSpiderWeb.SPIDER, true),
                    3);
        }
    }
}
