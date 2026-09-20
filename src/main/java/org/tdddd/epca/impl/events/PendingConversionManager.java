package org.tdddd.epca.impl.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.ColorEffectPacket;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.CothEffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


@EventBusSubscriber(modid = epca.MODID)
public final class PendingConversionManager {

    private static final List<Pending> PENDING = new ArrayList<>();

    private static final float MIN_HEALTH_FRACTION = 0.05F;

    private PendingConversionManager() {
    }

    private static final class Pending {
        final LivingEntity entity;
        final CothEffect.ConversionPlan plan;
        int remainingTicks;
        final int totalTicks;
        
        final boolean wasInvulnerable;
        final boolean wasNoAi;

        Pending(LivingEntity entity, CothEffect.ConversionPlan plan, int totalTicks,
                boolean wasInvulnerable, boolean wasNoAi) {
            this.entity = entity;
            this.plan = plan;
            this.remainingTicks = totalTicks;
            this.totalTicks = totalTicks;
            this.wasInvulnerable = wasInvulnerable;
            this.wasNoAi = wasNoAi;
        }
    }

    
    public static void schedule(LivingEntity entity, CothEffect.ConversionPlan plan, int delayTicks) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (plan == null || delayTicks <= 0) {
            if (plan != null) {
                CothEffect.executePlan(entity, plan);
            }
            return;
        }

        for (Pending existing : PENDING) {
            if (existing.entity == entity) {
                return;
            }
        }

        Pending pending = new Pending(entity, plan, delayTicks,
                entity.isInvulnerable(), isNoAi(entity));
        holdAlive(entity, pending.wasInvulnerable);

        ModNetwork.sendToAllTracking(entity,
                new ColorEffectPacket(entity, CothEffect.COLOR_TYPE_CONVERSION, delayTicks));

        PENDING.add(pending);
    }

    private static boolean isNoAi(LivingEntity entity) {
        if (entity instanceof Mob mob) {
            return mob.isNoAi();
        }
        return false;
    }

    private static void setNoAi(LivingEntity entity, boolean noAi) {
        if (entity instanceof Mob mob) {
            mob.setNoAi(noAi);
        }
    }

    
    
    private static void holdAlive(LivingEntity entity, boolean wasInvulnerable) {
        setNoAi(entity, true);
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
        }
        if (entity.isOnFire()) {
            if (!wasInvulnerable) {
                entity.setInvulnerable(false);
            }
            return;
        }
        if (!entity.isInvulnerable()) {
            entity.setInvulnerable(true);
        }
    }
    
    private static void releaseAfterPending(LivingEntity entity, boolean wasInvulnerable, boolean wasNoAi) {
        setNoAi(entity, wasNoAi);
        if (!wasInvulnerable) {
            entity.setInvulnerable(false);
        }
    }

    
    
    public static boolean isPending(LivingEntity entity) {
        if (entity == null || PENDING.isEmpty()) {
            return false;
        }
        for (Pending p : PENDING) {
            if (p.entity == entity) {
                return true;
            }
        }
        return false;
    }

    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }

        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            LivingEntity e = p.entity;

            if (e == null || !e.isAlive() || e.isRemoved()) {
                it.remove();
                continue;
            }

            
            holdAlive(e, p.wasInvulnerable);
            if (!e.isOnFire()) {
                float min = e.getMaxHealth() * MIN_HEALTH_FRACTION;
                if (e.getHealth() < min) {
                    e.setHealth(min);
                }
            }

            if (--p.remainingTicks <= 0) {
                it.remove();
                if (CothEffect.planIsEmpty(p.plan)) {
                    releaseAfterPending(e, p.wasInvulnerable, p.wasNoAi);
                } else {
                    CothEffect.executePlan(e, p.plan);
                }
            }
        }
    }

    
    public static void clear() {
        PENDING.clear();
    }
}
