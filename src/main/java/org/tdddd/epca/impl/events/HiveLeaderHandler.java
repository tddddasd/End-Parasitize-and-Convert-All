package org.tdddd.epca.impl.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganSavedData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HiveLeaderHandler {

    private static final ResourceLocation ENDER_BLADE_SCRAP_ID =
            new ResourceLocation("epca", "ender_blade_scrap");

    private static final float ENDER_BLADE_CHANCE = 0.7F;

    private static final int ENDER_EROSION_DURATION_TICKS = 60;

    private static final int ENDER_EROSION_AMPLIFIER = 1;

    private static final ResourceLocation BECKON_MEMBRANE_ID =
            new ResourceLocation("epca", "beckon_membrane");

    private static final int PARA_CURSE_DURATION_TICKS = 600;

    private static final int PARA_CURSE_AMPLIFIER = 1;

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        Player player = event.getEntity();
        if (HiveLeaderManager.isHiveLeader(player.getUUID())) {
            event.setDisplayname(Component.literal(event.getDisplayname().getString())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        BiomassEventHandler.syncBiomass(player);
        // 领巢者名单：客户端缓存是空集合起步的，服务端只在名单变动时广播，登录必须补发一份，
        // 否则“上一次游戏就已经是领巢者”的玩家本会话里所有领巢者渲染全部被身份守卫挡掉。
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            HiveLeaderManager.syncTo(serverPlayer);
        }
    }

    /** 换维度也补发一次：客户端在换世界/换维度时会清掉部分缓存，补发让它立刻重新收敛。 */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            HiveLeaderManager.syncTo(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;

        LivingEntity target = event.getEntity();
        if (target == null || target == player) return;

        if (player.level().isClientSide()) return;

        OrganStatSummary summary = OrganStatSummary.compute(
                HiveLeaderOrganSavedData.readOrCreate(player));

        applyEnderBladeProc(player, target, summary);
        applyBeckonMembraneProc(player, target, summary);

        // 种阶特化的攻击附加效果（要求开启器官模型显示，内部自行判定）。
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            org.tdddd.epca.impl.overworld.data.organ.species.HiveLeaderSpeciesManager
                    .onAttack(serverPlayer, target, summary);
        }
    }

    private static void applyEnderBladeProc(Player player, LivingEntity target, OrganStatSummary summary) {
        if (countInArms(summary, ENDER_BLADE_SCRAP_ID) <= 0) return;
        if (player.level().random.nextFloat() >= ENDER_BLADE_CHANCE) return;
        target.addEffect(new MobEffectInstance(
                ModEffects.ENDER_EROSION.get(), ENDER_EROSION_DURATION_TICKS, ENDER_EROSION_AMPLIFIER));
    }

    private static void applyBeckonMembraneProc(Player player, LivingEntity target, OrganStatSummary summary) {
        if (countInOuter(summary, BECKON_MEMBRANE_ID) <= 0) return;
        target.addEffect(new MobEffectInstance(
                ModEffects.PARA_CURSE.get(), PARA_CURSE_DURATION_TICKS, PARA_CURSE_AMPLIFIER));
    }

    private static int countInOuter(OrganStatSummary summary, ResourceLocation itemId) {
        int total = 0;
        for (OrganSlotGroup group : OrganSlotGroup.OUTER_GROUPS) {
            total += summary.countIn(group, itemId);
        }
        return total;
    }

    private static int countInArms(OrganStatSummary summary, ResourceLocation itemId) {
        int total = 0;
        for (OrganSlotGroup group : OrganSlotGroup.ARM_GROUPS) {
            total += summary.countIn(group, itemId);
        }
        return total;
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;
        MobEffect effect = event.getEffectInstance().getEffect();
        if (effect == MobEffects.POISON || effect == MobEffects.HUNGER || effect == MobEffects.CONFUSION ||
                effect == ModEffects.PARA_CURSE.get() || effect == ModEffects.VIRAL.get() || effect == ModEffects.FEAR.get() ||
                effect == ModEffects.SOLIDIFY.get() || effect == ModEffects.CORROSIVE.get() || effect == ModEffects.CONTEMPT_INORGANIC.get() ||
                effect == ModEffects.SOUL_BREAKING_CURSE.get()) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onLeaderKill(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        org.tdddd.epca.impl.overworld.data.HiveLeaderPlayerHooks.onKill(player, event.getEntity());
    }

    @SubscribeEvent
    public static void onLeaderHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        event.setAmount(org.tdddd.epca.impl.overworld.data.HiveLeaderPlayerHooks
                .onHurt(player, event.getSource(), event.getAmount()));
    }
}
