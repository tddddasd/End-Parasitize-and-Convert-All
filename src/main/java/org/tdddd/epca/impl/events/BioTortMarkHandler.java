package org.tdddd.epca.impl.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.BioTortSkillConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *  1 "" V
 *
 * <h2></h2>
 * <p>"ImpalingV" Impaling  <b></b> 1
 * {@code LangDataCN}  159  {@code effect.epca.needler}
 * "" {@code LangDataEN}  "Needler"
 * {@link ModEffects#NEEDLER} {@code NeedlerEffect}
 * " V"= {@code epca:needler}  amplifier 4 = amplifier + 1
 *  {@link BioTortSkillConstants#NEEDLER_DURATION_TICKS} tick1 </p>
 *
 * <p> {@code NeedlerEffect}  tick
 *  4 ""
 * {@code removeEffect(this)} ""
 *  1
 * {@code data/yawning_neko_api/entity_immunity_effects/*.json}  {@code epca:needler}
 * </p>
 *
 * <h2> LivingHurtEvent</h2>
 * <p>"" {@link LivingHurtEvent}
 *  {@code NestLeaderHandler}
 * {@code source.getEntity()} </p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BioTortMarkHandler {

    /** UUID -> {@code expiry <= now} */
    private static final Map<UUID, Long> MARKED_CREATURES = new HashMap<>();

    private BioTortMarkHandler() {
    }

    /**  {@code BioTortIncarnation}  */
    public static void markCreature(UUID creatureId, long now, int durationTicks) {
        MARKED_CREATURES.put(creatureId, now + Math.max(1, durationTicks));
    }

    /**  */
    public static void pruneExpired(long now) {
        MARKED_CREATURES.values().removeIf(expiry -> expiry <= now);
    }

    /**  */
    public static boolean isMarkedCreature(UUID creatureId, long now) {
        Long expiry = MARKED_CREATURES.get(creatureId);
        return expiry != null && expiry > now;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        //  BioTortIncarnation
        if (!(attacker instanceof LivingEntity livingAttacker) || attacker instanceof Player) {
            return;
        }
        Level level = livingAttacker.level();
        if (level.isClientSide) {
            return;
        }
        if (!isMarkedCreature(livingAttacker.getUUID(), level.getGameTime())) {
            return;
        }
        livingAttacker.addEffect(new MobEffectInstance(
                ModEffects.NEEDLER.get(),
                BioTortSkillConstants.NEEDLER_DURATION_TICKS,
                BioTortSkillConstants.NEEDLER_AMPLIFIER,
                false,
                true));
        epca.LOGGER.debug("[epca-bio-tort] 被标记的生物 {} 发动攻击，施加穿刺 V", livingAttacker.getName().getString());
    }
}

