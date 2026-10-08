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

public class InfestedSpiderWebProjectile extends ThrowableItemProjectile {
    public InfestedSpiderWebProjectile(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.INFESTED_SPIDER_WEB_PROJECTILE.get();
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
            // User requirement: when this web hits a creature that is NOT a parasite and NOT a nest
            // leader, the web is generated at THAT CREATURE'S FEET rather than wherever the
            // projectile happened to be at the moment of impact. For parasites and nest leaders the
            // previous behaviour is kept (the projectile's own position).
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
     * Places this projectile's web with the {@code spider} flag set.
     *
     * <p>The flag is applied through {@link InfestedSpiderWeb#markSpiderWeb}, which is a no-op when
     * the property is absent. Calling {@code setValue} directly on the property is what crashed the
     * server ("Cannot set property BooleanProperty{name=spider} as it does not exist in
     * Block{epca:infested_spider_web}") when the state did not carry it, so the guarded helper is
     * mandatory here - this runs from entity ticking.</p>
     */
    /**
     * Resolves where the web should go for a hit on {@code result}'s entity.
     *
     * <p>Classification is done with the mod's OWN predicate,
     * {@link IParasite#isParasiteByTagOrInterface(net.minecraft.world.entity.LivingEntity)} - nothing
     * new is invented here. Note that this single predicate already covers BOTH halves of the
     * requirement: for a {@code Player} it delegates to {@code NestLeaderManager.isNestLeader(uuid)},
     * and for every other entity it checks the {@code IParasite} interface or the {@code "Parasite"}
     * persistent-data flag. So "is not a parasite and not a nest leader" is exactly
     * {@code !isParasiteByTagOrInterface(...)}.</p>
     *
     * <p>Non-parasite / non-leader targets get their FEET block, with the block below as the fallback
     * when the feet block is occupied (the head's single-web rule). Because a web block is itself not
     * replaceable, an entity already standing in a web resolves to the block below it, so a second web
     * is never stacked on an existing one. If both candidates are blocked, the projectile's own
     * position is used so the shot still leaves a web rather than silently doing nothing.</p>
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
            BlockState web = ModBlocks.INFESTED_SPIDER_WEB.get().defaultBlockState();
            level.setBlock(pos, InfestedSpiderWeb.markSpiderWeb(web, true), 3);
        }
    }
}
