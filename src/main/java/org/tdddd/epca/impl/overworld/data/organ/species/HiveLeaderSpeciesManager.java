package org.tdddd.epca.impl.overworld.data.organ.species;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.events.ParasiteEvents;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.BiomassSyncPacket;
import org.tdddd.epca.impl.network.packet.s2c.SyncHiveLeaderSpeciesPacket;
import org.tdddd.epca.impl.overworld.data.BiomassManager;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganSavedData;
import org.tdddd.epca.impl.overworld.data.organ.OrganModelDisplay;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeRules;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.ParaCurseEffect;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Curbug;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Ripper;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 领巢者「种阶特化」的全部服务端逻辑。
 *
 * <h2>判定与回退</h2>
 * <pre>
 *   当前 = 重塑种 且 (粗制+重塑) 器官 ≥ 18  → 保持重塑种
 *   否则                                   → 自动判定（粗制种 → 初探种 → 虫染种 → 默认虫染种）
 * </pre>
 * 重塑种只能靠「(粗制+重塑) 器官 ≥ 18 时累计 25 击杀 + 消耗 50 生物质」进化，不会自动判定。
 */
public final class HiveLeaderSpeciesManager {

    // ===================== A. 基础增益参数 =====================

    /** 夜视：每 80 tick（4 秒）施加一次，持续 400 tick（20 秒）。 */
    public static final int NIGHT_VISION_PERIOD_TICKS = 80;
    public static final int NIGHT_VISION_DURATION_TICKS = 400;

    // ===================== B/C. 攻击附加参数 =====================

    /** 恐慌（FEAR）持续 15 秒。 */
    public static final int FEAR_DURATION_TICKS = 300;
    /** 虫咒缚身持续 30 秒。 */
    public static final int PARA_CURSE_DURATION_TICKS = 600;
    /** 流血持续 10 秒。 */
    public static final int BLEED_DURATION_TICKS = 200;
    /** 病毒持续 10 秒。 */
    public static final int VIRAL_DURATION_TICKS = 200;
    /** 接触施咒持续 60 秒。 */
    public static final int CONTACT_CURSE_DURATION_TICKS = 1200;

    /** 虫咒缚身最高 III 级（amplifier 2）。 */
    public static final int PARA_CURSE_MAX_LEVEL = 3;
    /** 流血最高 V 级（amplifier 4）。 */
    public static final int BLEED_MAX_LEVEL = 5;
    /** 病毒最高 255 级（amplifier 254）。 */
    public static final int VIRAL_MAX_LEVEL = 255;

    /** 虫染种：70% 虫咒缚身 / 5% 流血。 */
    public static final float INFESTED_PARA_CURSE_CHANCE = 0.70F;
    public static final float INFESTED_BLEED_CHANCE = 0.05F;

    /** 初探种：10% 流血 / 10% 病毒。 */
    public static final float ONESENT_BLEED_CHANCE = 0.10F;
    public static final float ONESENT_VIRAL_CHANCE = 0.10F;

    /** 重塑种：60% 虫咒缚身 / 5% 流血。 */
    public static final float RESHAPE_PARA_CURSE_CHANCE = 0.60F;
    public static final float RESHAPE_BLEED_CHANCE = 0.05F;

    /** 初探种：鳍兽的鳍 ≥5 时攻击附加 10 秒深潜 V（amplifier 4）。 */
    public static final int FINS_REQUIRED = 5;
    public static final int DEEP_SNEAK_DURATION_TICKS = 200;
    public static final int DEEP_SNEAK_AMPLIFIER = 4;

    /** 「鳍兽的鳍」的物品 id（按物品精确计数）。 */
    private static final Identifier FINS_FIN_ID = Identifier.fromNamespaceAndPath("epca", "fins_fin");

    /** 初探种：水中持续获得急迫/效率 I + 迅捷 I。 */
    public static final int WATER_BUFF_DURATION_TICKS = 60;
    public static final int WATER_BUFF_REFRESH_TICKS = 20;

    /** 初探种：碰撞箱接触施咒的检测间隔（tick）与膨胀量。 */
    public static final int CONTACT_CHECK_INTERVAL_TICKS = 5;
    public static final double CONTACT_INFLATE = 0.15D;

    // ===================== 粗制种 =====================

    /** 粗制种：每次获得生物质立刻回复 1 生命。 */
    public static final float POVERTY_BIOMASS_HEAL = 1.0F;

    /** 粗制种死亡「假死爆体」：半径 4 格、6 点伤害、原地 1 只诅虫。 */
    public static final double DEATH_BURST_RADIUS = 4.0D;
    public static final float DEATH_BURST_DAMAGE = 6.0F;

    // ===================== 重塑种 =====================

    /** 进化门槛：击杀数 / 生物质消耗。 */
    public static final int RESHAPE_EVOLVE_KILLS = 25;
    public static final int RESHAPE_EVOLVE_BIOMASS_COST = 50;

    /** 重塑种：击杀后 50% 概率获得 6 秒增生 I。 */
    public static final float RESHAPE_HYPERPLASIA_CHANCE = 0.50F;
    public static final int RESHAPE_HYPERPLASIA_DURATION_TICKS = 120;

    /** 重塑种：模型显示时的摔落免疫 / 跳跃 +3 格 / 步高 +1。 */
    public static final int RESHAPE_JUMP_AMPLIFIER = 3;
    public static final int RESHAPE_JUMP_DURATION_TICKS = 80;
    public static final int RESHAPE_JUMP_REFRESH_TICKS = 40;
    public static final double RESHAPE_STEP_HEIGHT_BONUS = 1.0D;

    /** 重塑种按键：3 生物质 → 15 秒力量 I / 迅捷 I / 饱和 I。 */
    public static final int RESHAPE_BUFF_BIOMASS_COST = 3;
    public static final int RESHAPE_BUFF_DURATION_TICKS = 300;

    // ===================== 初探种按键 =====================

    /** 初探种按键：15 生物质把准星裂兽变成虫巢节点。 */
    public static final int CONVERT_RIPPER_BIOMASS_COST = 15;

    /** 主动按键的防连点冷却（1 秒，只防刷包，不限制正常使用）。 */
    public static final int ABILITY_COOLDOWN_TICKS = 20;

    /** 攻击最小伤害的每目标冷却：1 秒 = 20 tick。 */
    public static final int MIN_DAMAGE_INTERVAL_TICKS = 20;

    public static final int ACTION_CONVERT_RIPPER = 0;
    public static final int ACTION_RESHAPE_BUFF = 1;

    // ===================== 属性修饰符 =====================

    private static final String MODIFIER_PATH_PREFIX = "species_";

    private static final Identifier ATTACK_MODIFIER_ID = modifierId("attack_damage");
    private static final Identifier HEALTH_MODIFIER_ID = modifierId("max_health");
    private static final Identifier ARMOR_MODIFIER_ID = modifierId("armor");
    private static final Identifier STEP_HEIGHT_MODIFIER_ID = modifierId("step_height");

    private static final Map<UUID, HiveLeaderSpeciesState> STATES = new HashMap<>();
    private static final Set<UUID> MODIFIERS_APPLIED = new HashSet<>();

    private HiveLeaderSpeciesManager() {
    }

    private static Identifier modifierId(String key) {
        return Identifier.fromNamespaceAndPath("epca", MODIFIER_PATH_PREFIX + key);
    }

    // ===================== 状态 =====================

    public static HiveLeaderSpeciesState state(Player player) {
        if (player == null) return HiveLeaderSpeciesState.load(null);
        return STATES.computeIfAbsent(player.getUUID(), ignored -> HiveLeaderSpeciesState.load(player));
    }

    public static void discard(Player player) {
        if (player == null) return;
        HiveLeaderSpeciesState cached = STATES.remove(player.getUUID());
        if (cached != null && cached.isDirty()) {
            cached.save(player);
        }
    }

    /** 玩家不再是领巢者 / 退出时：移除本系统加上的属性修饰符。 */
    public static void notLeader(Player player) {
        if (player == null || player.level().isClientSide()) return;
        if (MODIFIERS_APPLIED.remove(player.getUUID())) {
            clearModifiers(player);
            discard(player);
        }
    }

    public static void clear(Player player) {
        if (player == null) return;
        clearModifiers(player);
        discard(player);
    }

    public static void clearModifiers(Player player) {
        if (player == null) return;
        MODIFIERS_APPLIED.remove(player.getUUID());
        removeModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_ID);
        removeModifier(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER_ID);
        removeModifier(player, Attributes.ARMOR, ARMOR_MODIFIER_ID);
        removeModifier(player, Attributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER_ID);
        clampHealth(player);
    }

    // ===================== 每 tick =====================

    /**
     * 领巢者每 tick 调用（服务端）。{@code summary} 复用 {@code HiveLeaderOrganEffects#tick}
     * 已经算好的器官读数，避免重复遍历 35 个器官槽。
     */
    public static void tick(Player player, @Nullable OrganStatSummary summary) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (player.level().isClientSide()) return;
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;

        HiveLeaderSpeciesState state = state(player);
        OrganSpeciesCounts counts = OrganSpeciesCounts.of(summary);

        boolean speciesChanged = resolveSpecies(serverPlayer, state, counts);

        boolean modelVisible = OrganModelDisplay.isVisible(player);
        applyAttributes(serverPlayer, state.species(), modelVisible);

        tickNightVision(serverPlayer, state);
        tickSpecializationPassive(serverPlayer, state, summary, modelVisible);

        if (speciesChanged) {
            sendSpeciesMessage(serverPlayer, Component.literal("特化种阶：" + state.species().displayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        syncIfChanged(serverPlayer, state, counts, modelVisible);
        if (state.isDirty() && serverPlayer.tickCount % 20 == 0) {
            state.save(serverPlayer);
        }
    }

    /**
     * 特化判定。返回 true 表示特化发生了变化。
     */
    private static boolean resolveSpecies(ServerPlayer player, HiveLeaderSpeciesState state,
                                          OrganSpeciesCounts counts) {
        HiveLeaderSpecies current = state.species();
        HiveLeaderSpecies next;
        if (current == HiveLeaderSpecies.RESHAPE
                && counts.povertyOrReshape() >= OrganSpeciesCounts.REQUIRED_ORGAN_COUNT) {
            // 重塑种是投资进化来的，只要 (粗制+重塑) 器官仍然 ≥18 就保持，不自动降级。
            next = HiveLeaderSpecies.RESHAPE;
        } else {
            next = counts.autoSpecies();
        }
        if (next == current) return false;
        state.setSpecies(next);
        return true;
    }

    private static void tickNightVision(ServerPlayer player, HiveLeaderSpeciesState state) {
        int countdown = state.nightVisionCooldown() - 1;
        if (countdown <= 0) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION_TICKS, 0, false, true, true));
            countdown = NIGHT_VISION_PERIOD_TICKS;
        }
        state.setNightVisionCooldown(countdown);
    }

    private static void tickSpecializationPassive(ServerPlayer player, HiveLeaderSpeciesState state,
                                                  @Nullable OrganStatSummary summary,
                                                  boolean modelVisible) {
        switch (state.species()) {
            case ONESENT -> {
                tickOnesentContact(player);
                tickOnesentWaterBuffs(player, summary);
            }
            case RESHAPE -> tickReshapePassive(player, modelVisible);
            default -> {
            }
        }
    }

    // ===================== 属性修饰符 =====================

    /**
     * 把所有种阶属性修饰符收敛到「当前种阶应有的值」：
     * 先按固定 Identifier 移除旧值，再按需添加，因此切换种阶不会叠加。
     */
    private static void applyAttributes(ServerPlayer player, HiveLeaderSpecies species, boolean modelVisible) {
        double stepHeight = species == HiveLeaderSpecies.RESHAPE && modelVisible
                ? RESHAPE_STEP_HEIGHT_BONUS : 0.0D;

        applyModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_ID, species.attackDamageBonus());
        applyModifier(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER_ID, species.maxHealthBonus());
        applyModifier(player, Attributes.ARMOR, ARMOR_MODIFIER_ID, species.armorBonus());
        applyModifier(player, Attributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER_ID, stepHeight);

        MODIFIERS_APPLIED.add(player.getUUID());
        clampHealth(player);
    }

    private static void applyModifier(Player player, Holder<Attribute> attribute, Identifier id, double value) {
        if (attribute == null || id == null) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        AttributeModifier existing = instance.getModifier(id);
        if (Math.abs(value) < 1.0E-6D) {
            if (existing != null) instance.removeModifier(id);
            return;
        }
        if (existing != null && Math.abs(existing.amount() - value) < 1.0E-6D) {
            return;
        }
        if (existing != null) instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(
                id, value, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeModifier(Player player, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
    }

    private static void clampHealth(Player player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ===================== 初探种：接触 / 水下 =====================

    private static void tickOnesentContact(ServerPlayer player) {
        if (player.tickCount % CONTACT_CHECK_INTERVAL_TICKS != 0) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        AABB box = player.getBoundingBox().inflate(CONTACT_INFLATE);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != null && entity != player && entity.isAlive()
                        && !(entity instanceof Player)
                        && !ParasiteEvents.isParasiteEntity(entity))) {
            applyFixedParaCurse(other, CONTACT_CURSE_DURATION_TICKS, 0);
        }
    }

    /**
     * 固定等级施加虫咒缚身（只刷新时长，不随触发次数升级），用于「接触施咒」这类 60 秒 I 级效果。
     */
    private static void applyFixedParaCurse(LivingEntity target, int durationTicks, int amplifier) {
        if (target == null) return;
        if (!ParaCurseEffect.canApplyInLevel(target.level())) return;

        MobEffectInstance existing = target.getEffect(ModEffects.PARA_CURSE);
        int level = Math.max(amplifier, existing == null ? 0 : existing.getAmplifier());
        if (existing != null && existing.getDuration() >= durationTicks
                && existing.getAmplifier() == level) {
            return;
        }
        target.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE, durationTicks, level, false, false, true));
    }

    private static void tickOnesentWaterBuffs(ServerPlayer player, @Nullable OrganStatSummary summary) {
        if (player.tickCount % WATER_BUFF_REFRESH_TICKS != 0) return;
        if (!player.isInWater()) return;
        if (countFins(summary) < FINS_REQUIRED) return;

        player.addEffect(new MobEffectInstance(MobEffects.HASTE,
                WATER_BUFF_DURATION_TICKS, 0, true, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED,
                WATER_BUFF_DURATION_TICKS, 0, true, false, false));
    }

    /** 鳍兽的鳍数量（精确按物品统计，不按种阶计数）。 */
    private static int countFins(@Nullable OrganStatSummary summary) {
        if (summary == null) return 0;
        return summary.countOf(FINS_FIN_ID);
    }

    // ===================== 重塑种：跳跃 / 击杀增生 =====================

    private static void tickReshapePassive(ServerPlayer player, boolean modelVisible) {
        if (!modelVisible) return;
        if (player.tickCount % RESHAPE_JUMP_REFRESH_TICKS != 0) return;
        // 26 用隐藏的跳跃提升（amplifier 3 ≈ +3 格）实现，与原 1.20.1 语义一致。
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,
                RESHAPE_JUMP_DURATION_TICKS, RESHAPE_JUMP_AMPLIFIER, true, false, false));
    }

    // ===================== 攻击附加效果 =====================

    /**
     * 领巢者攻击命中时调用（服务端、LivingIncomingDamageEvent）。要求开启器官模型显示。
     */
    public static void onAttack(ServerPlayer player, LivingEntity target, @Nullable OrganStatSummary summary) {
        if (player == null || target == null || target == player) return;
        if (player.level().isClientSide()) return;
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;
        if (!OrganModelDisplay.isVisible(player)) return;

        HiveLeaderSpecies species = state(player).species();
        switch (species) {
            case INFESTED -> {
                applyFear(target, 0);
                if (player.getRandom().nextFloat() < INFESTED_PARA_CURSE_CHANCE) {
                    applyStackedParaCurse(target, PARA_CURSE_DURATION_TICKS);
                }
                if (player.getRandom().nextFloat() < INFESTED_BLEED_CHANCE) {
                    applyStacked(target, ModEffects.BLEEDING, BLEED_DURATION_TICKS, BLEED_MAX_LEVEL);
                }
            }
            case ONESENT -> {
                applyFear(target, 0);
                applyStackedParaCurse(target, PARA_CURSE_DURATION_TICKS);
                if (player.getRandom().nextFloat() < ONESENT_BLEED_CHANCE) {
                    applyStacked(target, ModEffects.BLEEDING, BLEED_DURATION_TICKS, BLEED_MAX_LEVEL);
                }
                if (player.getRandom().nextFloat() < ONESENT_VIRAL_CHANCE) {
                    applyStacked(target, ModEffects.VIRAL, VIRAL_DURATION_TICKS, VIRAL_MAX_LEVEL);
                }
                if (summary != null && countFins(summary) >= FINS_REQUIRED) {
                    target.addEffect(new MobEffectInstance(ModEffects.DEEP_SNEAK,
                            DEEP_SNEAK_DURATION_TICKS, DEEP_SNEAK_AMPLIFIER, false, true, true));
                }
            }
            case POVERTY -> {
                // 粗制种没有攻击附加效果，也没有最小伤害。
            }
            case RESHAPE -> {
                applyFear(target, 1);
                if (player.getRandom().nextFloat() < RESHAPE_PARA_CURSE_CHANCE) {
                    applyStackedParaCurse(target, PARA_CURSE_DURATION_TICKS);
                }
                if (player.getRandom().nextFloat() < RESHAPE_BLEED_CHANCE) {
                    applyStacked(target, ModEffects.BLEEDING, BLEED_DURATION_TICKS, BLEED_MAX_LEVEL);
                }
            }
        }
    }

    private static void applyFear(LivingEntity target, int amplifier) {
        MobEffectInstance existing = target.getEffect(ModEffects.FEAR);
        int next = Math.max(amplifier, existing == null ? 0 : existing.getAmplifier());
        if (existing != null && existing.getDuration() >= FEAR_DURATION_TICKS && existing.getAmplifier() == next) {
            return;
        }
        target.addEffect(new MobEffectInstance(ModEffects.FEAR, FEAR_DURATION_TICKS, next, false, true, true));
    }

    /**
     * 虫咒缚身：每次触发提升一级（最高 III），到顶后刷新时长。
     * 通过 {@link ParaCurseEffect#applyParaCurseEffect} 应用，保留原有的难度/进化点逻辑。
     */
    private static void applyStackedParaCurse(LivingEntity target, int durationTicks) {
        if (target == null) return;

        int maxAmplifier = PARA_CURSE_MAX_LEVEL - 1;
        MobEffectInstance existing = target.getEffect(ModEffects.PARA_CURSE);
        int next = existing == null ? 0 : Math.min(existing.getAmplifier() + 1, maxAmplifier);
        if (existing != null && existing.getAmplifier() > maxAmplifier) {
            // 已经因为其它来源超过本上限：不降级，只刷新时长。
            next = existing.getAmplifier();
        }

        if (existing != null && next <= existing.getAmplifier()) {
            if (!ParaCurseEffect.canApplyInLevel(target.level())) return;
            target.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE, durationTicks,
                    Math.max(existing.getAmplifier(), next), false, false, true));
            return;
        }
        ParaCurseEffect.applyParaCurseEffect(target, durationTicks, next);
    }

    /**
     * 通用「叠层 + 顶到上限后刷新时长」效果施加。
     *
     * @param maxLevel 最高等级（1 表示只刷新 I 级）
     */
    private static void applyStacked(LivingEntity target, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                     int durationTicks, int maxLevel) {
        if (target == null || effect == null) return;
        int maxAmplifier = Math.max(0, maxLevel - 1);
        MobEffectInstance existing = target.getEffect(effect);
        int next = existing == null ? 0 : Math.min(existing.getAmplifier() + 1, maxAmplifier);
        if (existing != null && existing.getAmplifier() > maxAmplifier) {
            next = existing.getAmplifier();
        }
        boolean sameLevelLongEnough = existing != null
                && existing.getAmplifier() == next
                && existing.getDuration() >= durationTicks;
        if (sameLevelLongEnough) return;
        target.addEffect(new MobEffectInstance(effect, durationTicks, next, false, true, true));
    }

    // ===================== 最小伤害 =====================

    /** 当前特化对应的攻击最小伤害（0 表示没有最小伤害）。 */
    public static double minimumDamage(Player player) {
        if (!(player instanceof ServerPlayer)) return 0.0D;
        return state(player).species().minimumDamage();
    }

    /**
     * 最小伤害的「同一目标 1 秒只触发一次」冷却检查。通过返回 true 时会把该目标记为刚触发。
     */
    public static boolean minimumDamageOffCooldown(Player player, LivingEntity target) {
        if (!(player instanceof ServerPlayer serverPlayer) || target == null) return false;
        HiveLeaderSpeciesState state = state(serverPlayer);
        long now = serverPlayer.level().getGameTime();
        Long last = state.lastMinDamageTick(target.getUUID());
        if (last != null && now - last < MIN_DAMAGE_INTERVAL_TICKS) {
            return false;
        }
        state.markMinDamage(target.getUUID(), now);
        return true;
    }

    // ===================== 受击 / 死亡 / 击杀 =====================

    /**
     * 是否应该完全免疫这次伤害：
     * 所有领巢者免疫墙内窒息与水中窒息；重塑种在模型显示时免疫摔落伤害。
     */
    public static boolean shouldCancelDamage(Player player, DamageSource source) {
        if (!(player instanceof ServerPlayer serverPlayer) || source == null) return false;
        if (!HiveLeaderManager.isHiveLeader(serverPlayer.getUUID())) return false;

        if (source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.IN_WALL)) {
            return true;
        }

        return state(serverPlayer).species() == HiveLeaderSpecies.RESHAPE
                && OrganModelDisplay.isVisible(serverPlayer)
                && source.is(DamageTypes.FALL);
    }

    /** 重塑种自带的伤害适应性（唯一保留的适应性来源；器官适应性已移除）。 */
    public static float modifyIncomingDamage(Player player, DamageSource source, float amount) {
        if (amount <= 0.0F || source == null) return amount;
        if (!(player instanceof ServerPlayer serverPlayer)) return amount;
        if (!HiveLeaderManager.isHiveLeader(serverPlayer.getUUID())) return amount;

        HiveLeaderSpeciesState state = state(serverPlayer);
        // 非重塑种：没有任何适应性（进度数据保留但完全不参与结算）。
        if (state.species() != HiveLeaderSpecies.RESHAPE) return amount;

        return HiveLeaderReshapeAdaptation.adaptAndReduce(serverPlayer, state, source, amount);
    }

    /**
     * 领巢者击杀生物时调用：累计进化击杀数，满足条件则消耗生物质进化为重塑种。
     * 只统计非寄生体（与自己人开战不算进度）。
     */
    public static void onKill(Player killer, LivingEntity killed) {
        if (!(killer instanceof ServerPlayer player)) return;
        if (killed == null || killed == player) return;
        if (player.level().isClientSide()) return;
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;
        if (ParasiteEvents.isParasiteEntity(killed)) return;

        HiveLeaderSpeciesState state = state(player);
        if (state.species() == HiveLeaderSpecies.RESHAPE) {
            // 重塑种：每次击杀 50% 概率获得 6 秒增生 I。
            if (player.getRandom().nextFloat() < RESHAPE_HYPERPLASIA_CHANCE) {
                player.addEffect(new MobEffectInstance(ModEffects.HYPERPLASIA,
                        RESHAPE_HYPERPLASIA_DURATION_TICKS, 0, false, true, true));
            }
            return;
        }

        OrganSpeciesCounts counts = currentCounts(player);
        if (counts.povertyOrReshape() < OrganSpeciesCounts.REQUIRED_ORGAN_COUNT) {
            return;
        }

        state.setLastKillTick(player.level().getGameTime());
        int kills = state.addKill();

        if (kills < RESHAPE_EVOLVE_KILLS) {
            if (kills % 5 == 0) {
                player.sendSystemMessage(Component.literal(
                        "进化进度：" + kills + "/" + RESHAPE_EVOLVE_KILLS + " 击杀"), true);
            }
            return;
        }

        tryEvolveToReshape(player, state);
    }

    private static void tryEvolveToReshape(ServerPlayer player, HiveLeaderSpeciesState state) {
        int points = BiomassManager.getBiomassPoints(player);
        if (points < RESHAPE_EVOLVE_BIOMASS_COST) {
            if (state.kills() == RESHAPE_EVOLVE_KILLS || state.kills() % 5 == 0) {
                player.sendSystemMessage(Component.literal(
                                "生物质不足：进化为重塑种需要 " + RESHAPE_EVOLVE_BIOMASS_COST
                                        + "，当前 " + points)
                        .withStyle(ChatFormatting.RED), false);
            }
            return;
        }

        int remaining = points - RESHAPE_EVOLVE_BIOMASS_COST;
        BiomassManager.setBiomassPoints(player, remaining);
        ModNetwork.sendToPlayer(player, new BiomassSyncPacket(true, remaining));

        state.setSpecies(HiveLeaderSpecies.RESHAPE);
        state.setKills(0);
        state.save(player);

        player.sendSystemMessage(Component.literal(
                        "进化完成：重塑种（消耗 " + RESHAPE_EVOLVE_BIOMASS_COST + " 生物质）")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.BIOMASS.get(),
                    player.getX(), player.getY() + 0.5D, player.getZ(),
                    24, 0.6D, 0.9D, 0.6D, 0.05D);
        }
        sync(player);
    }

    /** 粗制种：每次获得生物质立刻回复 1 生命。 */
    public static void onBiomassGained(Player player, int amount) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (amount <= 0) return;
        if (!HiveLeaderManager.isHiveLeader(serverPlayer.getUUID())) return;
        if (state(serverPlayer).species() != HiveLeaderSpecies.POVERTY) return;
        if (serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
            serverPlayer.heal(POVERTY_BIOMASS_HEAL);
        }
    }

    /** 粗制种死亡：假死爆体（6 伤害 + 原地 1 只诅虫）。 */
    public static void onLeaderDeath(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return;
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;
        if (state(player).species() != HiveLeaderSpecies.POVERTY) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        AABB box = player.getBoundingBox().inflate(DEATH_BURST_RADIUS);
        DamageSource burst = level.damageSources().explosion(player, player);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != null && entity != player && entity.isAlive()
                        && !(entity instanceof Player)
                        && !ParasiteEvents.isParasiteEntity(entity))) {
            other.hurt(burst, DEATH_BURST_DAMAGE);
        }

        level.sendParticles(ModParticles.BIOMASS.get(),
                player.getX(), player.getY() + 0.5D, player.getZ(),
                30, 1.0D, 0.6D, 1.0D, 0.08D);

        Curbug curbug = ModEntities.CURBUG.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (curbug != null) {
            curbug.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
            level.addFreshEntity(curbug);
        }
    }

    // ===================== 主动按键 =====================

    public static void handleAbility(ServerPlayer player, int action) {
        if (player == null) return;
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;
        if (player.level().isClientSide()) return;

        switch (action) {
            case ACTION_CONVERT_RIPPER -> convertRipper(player);
            case ACTION_RESHAPE_BUFF -> applyReshapeBuff(player);
            default -> {
            }
        }
    }

    private static boolean abilityOffCooldown(ServerPlayer player, String key) {
        HiveLeaderSpeciesState state = state(player);
        long now = player.level().getGameTime();
        if (now < state.abilityCooldownEnd(key)) return false;
        state.setAbilityCooldownEnd(key, now + ABILITY_COOLDOWN_TICKS);
        return true;
    }

    /** 初探种：15 生物质把准星指向的裂兽变成虫巢节点。 */
    private static void convertRipper(ServerPlayer player) {
        if (state(player).species() != HiveLeaderSpecies.ONESENT) {
            player.sendSystemMessage(Component.literal("只有初探种可以使用裂兽转化")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) return;

        Ripper ripper = pickRipper(player);
        if (ripper == null) {
            player.sendSystemMessage(Component.literal("准星没有指向裂兽")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        if (!abilityOffCooldown(player, "convert_ripper")) return;

        int points = BiomassManager.getBiomassPoints(player);
        if (points < CONVERT_RIPPER_BIOMASS_COST) {
            player.sendSystemMessage(Component.literal(
                            "生物质不足：转化裂兽需要 " + CONVERT_RIPPER_BIOMASS_COST + "，当前 " + points)
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        BlockPos pos = ripper.blockPosition();
        if (!HiveNodeRules.canCreateNaturalNode(level, pos)) {
            player.sendSystemMessage(Component.literal("该区块的虫巢节点已达上限")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        int remaining = points - CONVERT_RIPPER_BIOMASS_COST;
        BiomassManager.setBiomassPoints(player, remaining);
        ModNetwork.sendToPlayer(player, new BiomassSyncPacket(true, remaining));

        ripper.discard();
        level.setBlock(pos, ModBlocks.HIVE_NODE.get().defaultBlockState(), 3);
        HiveNodeRules.markNatural(level, pos);
        level.sendParticles(ModParticles.BIOMASS.get(),
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                16, 0.4D, 0.4D, 0.4D, 0.05D);

        player.sendSystemMessage(Component.literal(
                        "裂兽 → 虫巢节点（消耗 " + CONVERT_RIPPER_BIOMASS_COST + " 生物质）")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    /** 重塑种：3 生物质 → 15 秒力量 I / 迅捷 I / 饱和 I。 */
    private static void applyReshapeBuff(ServerPlayer player) {
        if (state(player).species() != HiveLeaderSpecies.RESHAPE) {
            player.sendSystemMessage(Component.literal("只有重塑种可以使用该增益")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        if (!abilityOffCooldown(player, "reshape_buff")) return;

        int points = BiomassManager.getBiomassPoints(player);
        if (points < RESHAPE_BUFF_BIOMASS_COST) {
            player.sendSystemMessage(Component.literal(
                            "生物质不足：该增益需要 " + RESHAPE_BUFF_BIOMASS_COST + "，当前 " + points)
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        int remaining = points - RESHAPE_BUFF_BIOMASS_COST;
        BiomassManager.setBiomassPoints(player, remaining);
        ModNetwork.sendToPlayer(player, new BiomassSyncPacket(true, remaining));

        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,
                RESHAPE_BUFF_DURATION_TICKS, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED,
                RESHAPE_BUFF_DURATION_TICKS, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION,
                RESHAPE_BUFF_DURATION_TICKS, 0, false, true, true));

        player.sendSystemMessage(Component.literal(
                        "重塑增益：力量 I / 迅捷 I / 饱和 I（消耗 " + RESHAPE_BUFF_BIOMASS_COST + " 生物质）")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    @Nullable
    private static Ripper pickRipper(ServerPlayer player) {
        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach <= 0.0D) return null;

        Vec3 from = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 to = from.add(look.x * reach, look.y * reach, look.z * reach);

        BlockHitResult blockHit = player.level().clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            to = blockHit.getLocation();
        }

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, from, to, searchBox,
                entity -> !entity.isSpectator() && entity.isPickable(),
                reach * reach);
        if (hit == null) return null;

        if (!(hit.getEntity() instanceof Ripper ripper)) return null;
        if (!ripper.isAlive() || ripper.level() != player.level()) return null;
        return ripper;
    }

    // ===================== 同步 =====================

    /** 主动把当前特化状态发给客户端（登录 / 进化 / 调试时用）。 */
    public static void sync(ServerPlayer player) {
        if (player == null) return;
        HiveLeaderSpeciesState state = state(player);
        OrganSpeciesCounts counts = currentCounts(player);
        boolean modelVisible = OrganModelDisplay.isVisible(player);
        ModNetwork.sendToPlayer(player, new SyncHiveLeaderSpeciesPacket(
                state.species().id(), state.kills(), counts, modelVisible,
                HiveLeaderReshapeAdaptation.adaptableEntries(player, state)));
        state.setLastSyncedSignature(signature(state, counts, modelVisible));
    }

    /**
     * 登录 / 维度切换后立刻按器官构成重算特化（不等下一 tick），并同步客户端。
     */
    public static void refresh(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return;
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;

        OrganStatSummary summary = OrganStatSummary.compute(
                HiveLeaderOrganSavedData.readOrCreate(player));
        OrganSpeciesCounts counts = OrganSpeciesCounts.of(summary);
        HiveLeaderSpeciesState state = state(player);

        // 登录 / 换维度：把黑名单（火焰 / kill / 虚空）与非法 key 的历史适应数据清掉。
        HiveLeaderReshapeAdaptation.sanitize(player, state);

        if (resolveSpecies(player, state, counts)) {
            sendSpeciesMessage(player, Component.literal("特化种阶：" + state.species().displayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        boolean modelVisible = OrganModelDisplay.isVisible(player);
        applyAttributes(player, state.species(), modelVisible);
        tickNightVision(player, state);
        sync(player);
        state.save(player);
    }

    private static void syncIfChanged(ServerPlayer player, HiveLeaderSpeciesState state,
                                      OrganSpeciesCounts counts, boolean modelVisible) {
        String signature = signature(state, counts, modelVisible);
        if (signature.equals(state.lastSyncedSignature())) return;
        if (player.tickCount % 10 != 0 && !state.isDirty()) return;
        ModNetwork.sendToPlayer(player, new SyncHiveLeaderSpeciesPacket(
                state.species().id(), state.kills(), counts, modelVisible,
                HiveLeaderReshapeAdaptation.adaptableEntries(player, state)));
        state.setLastSyncedSignature(signature);
    }

    private static String signature(HiveLeaderSpeciesState state, OrganSpeciesCounts counts,
                                    boolean modelVisible) {
        return state.species().id() + '|' + state.kills() + '|' + counts.describe() + '|' + modelVisible
                + '|' + state.adaptationVersion();
    }

    public static OrganSpeciesCounts currentCounts(Player player) {
        if (player == null) return OrganSpeciesCounts.EMPTY;
        return OrganSpeciesCounts.of(OrganStatSummary.compute(
                HiveLeaderOrganSavedData.readOrCreate(player)));
    }

    private static void sendSpeciesMessage(ServerPlayer player, Component message) {
        player.sendSystemMessage(message, false);
    }

    // ===================== 调试 / 读数 =====================

    public static String describe(Player player) {
        if (player == null) return "无";
        HiveLeaderSpeciesState state = state(player);
        OrganSpeciesCounts counts = currentCounts(player);
        return state.species().displayName() + "（击杀 " + state.kills() + "，"
                + counts.describe() + "，最小伤害 " + state.species().minimumDamage()
                + "，伤害适应 " + HiveLeaderReshapeAdaptation.describe(player, state) + "）";
    }
}
