package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

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
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
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
            // User requirement: when this web hits a creature that is NOT a parasite and NOT a nest
            // leader, the web is generated at THAT CREATURE'S FEET rather than wherever the projectile
            // happened to be at the moment of impact. Parasites and nest leaders keep the previous
            // behaviour (the projectile's own position). The block-hit path below is unchanged.
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

    /**
     * Resolves where the web should go for a hit on {@code result}'s entity.
     *
     * <p>Classification uses the mod's OWN predicate,
     * IParasite#isParasiteByTagOrInterface, which already covers BOTH halves of the requirement: for a
     * Player it delegates to NestLeaderManager#isNestLeader(uuid), and for every other entity it
     * checks the IParasite interface or the "Parasite" persistent-data flag. So "not a parasite and
     * not a nest leader" is exactly the negation of that one call.</p>
     *
     * <p>Feet block first, block below as the fallback when the feet block is occupied (the head's
     * single-web rule). A web block is itself not replaceable, so an entity already standing in a web
     * resolves to the block below it and a second web is never stacked on an existing one. If both
     * candidates are blocked, the projectile's own position is used so the shot still leaves a web.</p>
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
            BlockState web = ModBlocks.INFESTED_CAVE_SPIDER_WEB.get().defaultBlockState();
            level.setBlock(pos, InfestedCaveSpiderWeb.markSpiderWeb(web, true), 3);
        }
    }
}
