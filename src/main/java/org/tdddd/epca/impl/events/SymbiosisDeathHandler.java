package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager;
import org.tdddd.epca.impl.overworld.registry.effects.buff.HyperplasiaEffect;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

/**
 * The Symbiosis Lie effect system plus the Hyperplasia effect-ending recovery (1.20.1 parity - the
 * 1.20.1 build keeps both in this same class, so the two trees stay diffable).
 *
 * <ul>
 *   <li>{@link #onLivingTick} - Hyperplasia rollback sweep: an entity whose MAX_HEALTH was inflated by
 *       Hyperplasia but which no longer carries the effect gets the whole bonus removed and its health
 *       clamped. Runs once per second, exactly like the 1.20.1 {@code LivingTickEvent} handler.</li>
 *   <li>{@link #onEffectExpired} - the same rollback, applied immediately when the duration runs out
 *       (26.1.2 has an expiry event where 1.20.1 only had the tick sweep).</li>
 *   <li>{@link #onHurt} - Symbiosis Lie retaliation: a same-kind victim that the affected mob damages
 *       has a 25% chance to take a quarter of that damage again as minimum damage.</li>
 *   <li>{@link #onDeath} - Symbiosis Lie burst: explosion, explosion sound, horizontal conversion
 *       particle, gib/dismember burst, block infestation around the corpse and the parasite spawns.</li>
 * </ul>
 */
@EventBusSubscriber(modid = epca.MODID)
public class SymbiosisDeathHandler {
    /** Dismemberment burst variant id used by the conversion particle family (1.20.1 "horizontal"). */
    private static final int CONVERSION_VARIANT_HORIZONTAL = 5;
    /** Colour handed to the conversion particle (same olive green as 1.20.1). */
    private static final int CONVERSION_COLOR = 0x3F5A2A;

    // ────────── Hyperplasia: effect-ending recovery ──────────

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide()) return;
        if (entity.tickCount % 20 != 0) return;
        // Cheap gate first: only entities that ever received the bonus need any work.
        if (!HyperplasiaEffect.hasHealthMark(entity)) return;
        // Still hyperplastic -> nothing to roll back.
        if (entity.hasEffect(ModEffects.HYPERPLASIA)) return;
        HyperplasiaEffect.restoreHealth(entity);
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) return;
        if (!(instance.getEffect().value() instanceof HyperplasiaEffect)) return;

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        HyperplasiaEffect.restoreHealth(entity);
    }

    // ────────── Symbiosis Lie: retaliation ──────────

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker)) return;
        if (attacker == target || !attacker.hasEffect(ModEffects.SYMBIOSIS_LIE)) return;
        if (source.is(ModDamageTypes.MINIMUM)) return;
        // Only a same-kind victim that was actually hit (melee or projectile) can be retaliated against.
        if (target.getClass() != attacker.getClass()) return;
        if (attacker.level().isClientSide()) return;
        if (attacker.level().getRandom().nextFloat() < 0.25F) {
            Holder<DamageType> holder = attacker.level().registryAccess()
                    .lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(ModDamageTypes.MINIMUM);
            target.hurt(new DamageSource(holder), Math.max(0.5F, event.getAmount() * 0.25F));
        }
    }

    // ────────── Symbiosis Lie: death burst ──────────

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(ModEffects.SYMBIOSIS_LIE)) return;

        float strength = Math.max(1.0F, Math.min(4.0F,
                (entity.getBbWidth() + entity.getBbHeight()) * 0.75F));
        level.explode(entity, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                strength, Level.ExplosionInteraction.NONE);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                strength >= 2.5F ? ModSoundEvents.BIG_EXPLOSION.get() : ModSoundEvents.SMALL_EXPLOSION.get(),
                SoundSource.HOSTILE, 1.0F, 1.0F);
        // The horizontal conversion burst is the "corresponding particle" of this burst in both trees.
        ModParticles.spawnConversion(level,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                CONVERSION_VARIANT_HORIZONTAL, CONVERSION_COLOR);

        // 26.1.2: the MobDismemberment reflection call of 1.20.1 is replaced by the in-mod gib packet.
        org.tdddd.epca.impl.network.packet.s2c.DismemberS2CPacket.send(entity);

        // Randomly infest blocks inside a 1.5x body-size cube around the death position.
        double radiusX = Math.max(1.0D, entity.getBbWidth() * 0.75D);
        double radiusY = Math.max(1.0D, entity.getBbHeight() * 0.75D);
        for (int i = 0; i < 24; i++) {
            double bx = entity.getX() + (level.getRandom().nextDouble() * 2.0D - 1.0D) * radiusX;
            double by = entity.getY() + (level.getRandom().nextDouble() * 2.0D - 1.0D) * radiusY;
            double bz = entity.getZ() + (level.getRandom().nextDouble() * 2.0D - 1.0D) * radiusX;
            BlockPos pos = BlockPos.containing(bx, by, bz);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            BlockConversionManager.getInstance().convertBlockUsingGeneralConfig(level, pos, state);
        }

        // Parasite spawns: 1-2 curbugs, a fins in water / a ripper on land, and an optional mozzie.
        int curbugs = 1 + level.getRandom().nextInt(2);
        for (int i = 0; i < curbugs; i++) {
            spawn(level, ModEntities.CURBUG.get(), entity.getX(), entity.getY(), entity.getZ());
        }
        if (entity.isInWater()) {
            spawn(level, ModEntities.FINS.get(), entity.getX(), entity.getY(), entity.getZ());
        } else {
            spawn(level, ModEntities.RIPPER.get(), entity.getX(), entity.getY(), entity.getZ());
        }
        if (level.getRandom().nextFloat() < 0.25F) {
            spawn(level, ModEntities.MOZZIE.get(), entity.getX(), entity.getY(), entity.getZ());
        }
    }

    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, double x, double y, double z) {
        // 26.1.2: EntityType#create needs an explicit spawn reason, and Entity#moveTo was renamed snapTo.
        Mob mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) return;
        mob.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(mob);
    }
}
