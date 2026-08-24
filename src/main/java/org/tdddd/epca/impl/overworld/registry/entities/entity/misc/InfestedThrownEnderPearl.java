package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEndermite;

public class InfestedThrownEnderPearl extends ThrownEnderpearl {
    public InfestedThrownEnderPearl(EntityType<? extends ThrownEnderpearl> type, Level level) {
        super(type, level);
    }

    
    public InfestedThrownEnderPearl(Level level, LivingEntity shooter) {
        super(level, shooter);
    }

    @Override
    protected void onHit(HitResult result) {
        
        if (!this.level().isClientSide) {
            
            for (int i = 0; i < 32; ++i) {
                this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY() + this.random.nextDouble() * 2.0D, this.getZ(),
                        this.random.nextGaussian(), 0.0D, this.random.nextGaussian());
            }
            
            Entity owner = this.getOwner();
            if (owner instanceof Player player) {
                
                if (!player.isSpectator()) {
                    player.teleportTo(this.getX(), this.getY(), this.getZ());
                    player.hurt(this.damageSources().fall(), 5.0F); 
                }
            }
            
            this.discard();
        }
        
        if (!this.level().isClientSide) {
            Vec3 hitPos = result.getLocation();
            InfestedEndermite endermite = new InfestedEndermite(ModEntities.INFESTED_ENDERMITE.get(), this.level());
            endermite.setPos(hitPos.x, hitPos.y, hitPos.z);
            this.level().addFreshEntity(endermite);
        }
    }
}
