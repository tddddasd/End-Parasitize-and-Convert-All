package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import net.minecraft.core.BlockPos;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSpider;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedCaveSpiderWeb;

public class InfestedCaveSpiderWebProjectile extends ThrowableItemProjectile {
    public InfestedCaveSpiderWebProjectile(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.INFESTED_CAVE_SPIDER_WEB_PROJECTILE.get();
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

            // 先回传命中结果（命中目标 / 命中其它实体），再销毁自身
            notifyShooter(result.getEntity());
            tryPlaceWeb(webTargetFor(result));
            this.discard();
        }
        super.onHitEntity(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            BlockState hitState = this.level().getBlockState(result.getBlockPos());
            if (hitState.getFluidState().isEmpty()) {
                // 命中方块 = 没有打中目标，按一次未命中回传
                notifyShooter(null);
                tryPlaceWeb(this.blockPosition());
                this.discard();
            }
        }
        super.onHitBlock(result);
    }

    /**
     * 把结算结果回传给发射者（虫染蜘蛛），用于「连续远程未命中 → 近战攀爬」判定。
     *
     * @param hit 被击中的实体；命中方块/未命中时为 {@code null}
     */
    private void notifyShooter(Entity hit) {
        if (this.getOwner() instanceof InfestedSpider spider) {
            spider.onWebProjectileResolved(hit);
        }
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
            BlockState web = ModBlocks.INFESTED_CAVE_SPIDER_WEB.get().defaultBlockState();
            level.setBlock(pos, InfestedCaveSpiderWeb.markSpiderWeb(web, true), 3);
        }
    }
}
