package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.client.entity.IMotionAligned;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

import java.util.Random;

public class BoneArrow extends AbstractArrow implements GeoEntity, IMotionAligned {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int age = 0;
    private final int MAX_AGE = 30 * 20; 
    private static final Random RANDOM = new Random(); 

    public BoneArrow(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
        this.setBaseDamage(8.0F); 
    }

    public BoneArrow(Level level, LivingEntity shooter) {
        // 26.1.2 AbstractArrow line 99-100: on the server, a non-null but EMPTY firedFromWeapon
        // throws IllegalArgumentException("Invalid weapon firing an arrow"). null means "no weapon".
        super(ModEntities.BONE_ARROW.get(), shooter, level, ItemStack.EMPTY, null);
        this.setNoGravity(false);
        this.setBaseDamage(8.0F); 
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (age >= MAX_AGE && !this.level().isClientSide()) {
            this.discard();
        }

        if (!this.onGround() && age > 1) {
            Vec3 dm = this.getDeltaMovement();
            double horizontal = dm.horizontalDistance();
            if (dm.lengthSqr() > 1.0E-6D) {
                this.setYRot((float)(Mth.atan2(dm.x, dm.z) * (180F / Math.PI)));
                this.setXRot((float)(Mth.atan2(dm.y, horizontal) * (180F / Math.PI)));
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        
        if (this.level().isClientSide()) {
            return;
        }
        LivingEntity shooter = this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null;
        var target = hitResult.getEntity();
        if (IParasite.isParasiteByTagOrInterface(shooter) && IParasite.isParasiteNoLivingByTagOrInterface(target)) {
            
            return;
        }
        boolean damageApplied = target.hurtOrSimulate(this.damageSources().arrow(this, shooter), 8.0F);
        if (damageApplied && target instanceof LivingEntity livingTarget) {
            applyCothEffect(livingTarget);

            if (target instanceof LivingEntity living && this.getPersistentData().getBooleanOr("InfestedFireArrow", false)) {
                living.setRemainingFireTicks(160);  
            }
        }
        this.discard();
    }
    private void applyCothEffect(LivingEntity target) {
        var existingEffect = target.getEffect(ModEffects.COTH);

        if (existingEffect != null) {
            
            int currentAmplifier = existingEffect.getAmplifier(); 
            int newAmplifier = currentAmplifier;
            if (RANDOM.nextFloat() < 0.7f) {
                
                newAmplifier = Math.min(currentAmplifier + 1, 2);
            }
            
            target.addEffect(new MobEffectInstance(ModEffects.COTH, 15 * 20, newAmplifier));
        } else {
            
            target.addEffect(new MobEffectInstance(ModEffects.COTH, 15 * 20, 0));
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
    /**
     * Deliberately empty: the bone arrow renders statically.
     *
     * <p>There is no {@code assets/epca/geckolib/animations/bone_arrow.animation.json} — not in the
     * 26.1.2 tree and not in the 1.20.1 baseline either — so a controller's {@code RawAnimation}
     * stages would all resolve to {@code null} and GeckoLib 5.5.2's
     * {@code AnimationTimeline.create} would call {@code List#getLast()} on the resulting empty
     * stage list (non-zero transition ticks), throwing {@code NoSuchElementException} while
     * extracting the render state. No controller means no timeline; the geo model and texture
     * still render through {@code EpcaGeoRenderer}/{@code EpcaGeoModel}.</p>
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // no controllers: the bone arrow is a static model
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
