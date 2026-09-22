package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;
import org.tdddd.epca.impl.client.entity.EpcaAnimations;
import org.tdddd.epca.impl.client.entity.IMotionAligned;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.capability.LifetimeCapability;
import com.geckolib.animatable.GeoEntity;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingChickenHead;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

public class BiomassEgg extends AbstractArrow implements GeoEntity, IMotionAligned {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int age = 0;
    private final int MAX_AGE = 15 * 20; 

    public BiomassEgg(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.setNoGravity(false); 
    }

    public BiomassEgg(Level level, LivingEntity shooter) {
        // 26.1.2 AbstractArrow line 99-100: a non-null but EMPTY firedFromWeapon throws
        // IllegalArgumentException("Invalid weapon firing an arrow") on the server. null = no weapon.
        super(ModEntities.BIOMASS_EGG.get(), shooter, level, ItemStack.EMPTY, null); 
        this.setNoGravity(false); 
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (age >= MAX_AGE && !this.level().isClientSide()) {
            this.discard();
        }
        if (!this.onGround()) {
            Vec3 deltaMovement = this.getDeltaMovement();
            double length = deltaMovement.length();
            if (length > 1.0E-6D) {
                double horizontalLength = deltaMovement.horizontalDistance();
                this.setYRot((float)(Mth.atan2(deltaMovement.x, deltaMovement.z) * (180F / Math.PI)));
                this.setXRot((float)(Mth.atan2(deltaMovement.y, horizontalLength) * (180F / Math.PI)));
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.random.nextFloat() < 0.125f) { 
            this.spawnWalkingChickenHeads();
        }
        this.discard();
        super.onHitBlock(result);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) result.getEntity();
            
            if (!IParasite.isParasiteByTagOrInterface(target)) {
                target.hurtOrSimulate(target.damageSources().indirectMagic(this, this.getOwner()), 0.5f);
                target.addEffect(new MobEffectInstance(
                        ModEffects.COTH,
                        300,
                        0,
                        false,
                        true
                ));
            }
        }
        if (this.random.nextFloat() < 0.125f) { 
            this.spawnWalkingChickenHeads();
        }
        this.discard();
    }

    private void setLifeTime(LivingEntity entity, int ticks) {
        entity.getData(LifetimeCapability.LIFETIME).setRemainingTicks(ticks);
    }
    private void spawnWalkingChickenHeads() {
        if (!this.level().isClientSide()) {
            
            int count = 1 + random.nextInt(2); 

            for (int i = 0; i < count; i++) {
                WalkingChickenHead walkingChickenHead = new WalkingChickenHead(ModEntities.WALKING_CHICKEN_HEAD.get(), this.level());
                walkingChickenHead.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                setLifeTime(walkingChickenHead, 1200);
                walkingChickenHead.addEffect(new MobEffectInstance(
                        ModEffects.RAGE, 
                        1200,  
                        1       
                ));
                this.level().addFreshEntity(walkingChickenHead);
            }
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        
        return ItemStack.EMPTY;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity) {
        return super.getAddEntityPacket(serverEntity);
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", EpcaAnimations.GEO_TRANSITION_TICKS, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationTest<BiomassEgg> event) {
        event.setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
