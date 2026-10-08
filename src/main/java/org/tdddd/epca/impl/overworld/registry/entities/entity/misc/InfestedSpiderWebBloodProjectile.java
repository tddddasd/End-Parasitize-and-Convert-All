package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import net.minecraft.core.BlockPos;
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
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedSpiderWeb;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

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
            // User requirement: a non-parasite, non-nest-leader target gets the web at ITS FEET;
            // parasites and nest leaders keep the projectile-position behaviour.
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
                tryPlaceWeb(this.blockPosition());
                this.discard();
            }
        }
        super.onHitBlock(result);
    }

    /**
     * Places this projectile's blood web with the {@code spider} flag set.
     *
     * <p>Guarded through {@link InfestedSpiderWeb#markSpiderWeb}: a direct {@code setValue} on the
     * property throws when the state does not carry it, and this runs from entity ticking, so the
     * unguarded call is a hard server crash.</p>
     */
    /**
     * Resolves where the blood web should go for a hit on {@code result}'s entity.
     *
     * <p>Classification uses the mod's OWN predicate,
     * {@link IParasite#isParasiteByTagOrInterface(net.minecraft.world.entity.LivingEntity)}, which
     * already covers both halves of the requirement: for a {@code Player} it delegates to
     * {@code NestLeaderManager.isNestLeader(uuid)}, and otherwise it checks the {@code IParasite}
     * interface or the {@code "Parasite"} persistent-data flag.</p>
     *
     * <p>Feet block first, block below as fallback; an entity already standing in a web resolves to
     * the block below, so webs never stack. If both are blocked the projectile's own position is used.</p>
     */
    private BlockPos webTargetFor(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity living
                && !IParasite.isParasiteByTagOrInterface(living)) {
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
            level.setBlock(pos, InfestedSpiderWeb.markSpiderWeb(web, true), 3);
        }
    }
}
