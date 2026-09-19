package org.tdddd.epca.impl.overworld.registry.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.tdddd.eej.api.AltarInteractionHandler;
import org.tdddd.eej.api.AltarItemContainer;
import org.tdddd.eej.api.AltarStructure;
import org.tdddd.eej.impl.altar.AbstractAltarBlock;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/**
 * EPCA 侧的祭坛交互：献祭仪式、小型物品展示框过滤器、调试用击杀棒。
 * <p>
 * 祭坛方块本体已经分离到前置模组 eej 中，这里通过 eej 的
 * {@link org.tdddd.eej.api.AltarInteractionRegistry} 注册处理器接入祭坛右键交互，
 * 因此 eej 不需要知道 EPCA 的任何内容。
 *
 * <h2>触发方式（I21 / 已确认的设计决策）</h2>
 * 仪式分两步：<b>先把宝石块放到祭台上</b>（普通右键放置，与任何物品一样），
 * 然后<b>空手右键祭台</b>发起请求：
 * <ul>
 *   <li><b>空手 + 非潜行右键</b> = 请求仪式。判定条件仍然是原来的那一组，
 *       其中「宝石在祭台上」由 {@code AltarItemContainer#getItem()} 读取，
 *       因此看的是<b>祭台里存着什么</b>，与玩家手上拿什么完全无关。
 *       <b>只有全部条件成立时才真正发动仪式</b>；任何一条不成立都返回
 *       {@link InteractionResult#PASS} 放行给 eej 的祭坛框架，
 *       由 {@code AbstractAltarBlock#useWithoutItem} 继续执行「主祭台合成」。</li>
 *   <li><b>潜行 + 空手</b> = 从祭台取回存放的物品（I20，恢复改动前的原始手感）。
 *       eej 侧 {@code useWithoutItem} 也会 dispatch 一次，但本处理器在潜行空手时直接返回
 *       {@link InteractionResult#PASS} 放行，让 {@code useWithoutItem} 的取回分支继续执行。</li>
 *   <li><b>手持任何物品</b> = 放置物品 / 放置展示框（不变）；手持调试用击杀棒时只走结构调试输出，
 *       不尝试仪式。也就是说仪式请求不再依赖手里的宝石块。</li>
 * </ul>
 *
 * 判定与执行被拆开：{@link #evaluateSacrifice} 只做无副作用的判定（不发消息、不喷粒子、不入队），
 * 只有判定全部通过时 {@link #onAltarUse} 才调用 {@link #performSacrifice}。
 * 判定不通过时<b>不</b>报告任何「献祭失败」信息：空手右键本来就有「祭坛合成」这一语义，
 * 用失败提示吞掉这次点击会让合成永远做不了；{@link SacrificeCheck} 的原因只用于区分判定分支。
 *
 * <h2>表现层反馈（I18）</h2>
 * 被献祭的生物在消失前会各喷一次灵魂粒子并播放音效；仪式开始 / 进行中 / 完成 / 取消
 * 都有音效与动作栏提示（进行中的节奏反馈在 {@link BlockConversionManager} 里）。
 */
public class EpcaAltarInteractionHandler implements AltarInteractionHandler {

    /** 仪式检测半径：与 {@code BlockConversionManager} 的转换半径 72 保持一致。 */
    private static final int RITUAL_SCAN_RADIUS = 72;
    /** 仪式判定球半径（与原实现一致：以方块中心为球心 1.5 格）。 */
    private static final double RITUAL_SPHERE_RADIUS = 1.5;

    @Override
    public InteractionResult onAltarUse(Level level, BlockPos pos, BlockState state,
                                        Player player, InteractionHand hand, ItemStack heldItem) {
        // 0. 仪式请求 = 非潜行 + 空手右键。此时祭台上应该已经放着宝石块（由 evaluateSacrifice
        //    通过 AltarItemContainer#getItem() 读取 —— 「宝石在祭台上」是仪式的硬条件之一，
        //    与玩家手里拿着什么无关；手持物品时的普通右键仍然是「放置物品」）。
        //    I20：潜行 + 空手不算仪式请求 —— 那是「从祭台取回物品」的原始手感（eej 侧
        //    AbstractAltarBlock#useWithoutItem 的取回分支），这里必须放行（返回 PASS），
        //    否则空手潜行右键会被仪式请求吞掉，玩家再也拿不回祭台上存放的东西。
        //    判定与执行分离：evaluateSacrifice 只判定、无副作用；只有它判定全部通过时才执行仪式。
        //    任何一条前置条件不成立都**不**提示失败、**不**入队，直接落到下面的 PASS，
        //    由 AltarInteractionRegistry#dispatch → AbstractAltarBlock#useWithoutItem 继续走
        //    祭坛合成（框架在自己的取回/合成逻辑之前 dispatch，所以 PASS 确实会到达合成路径）。
        if (!player.isShiftKeyDown() && heldItem.isEmpty()) {
            SacrificePlan plan = evaluateSacrifice(level, pos);
            if (plan.isOk()) {
                performSacrifice(plan.serverLevel(), pos, player, plan.animals(), plan.villagers());
                return InteractionResult.SUCCESS;
            }
            // 前置条件不满足：不报告失败、不入队，放行给框架的合成路径。
        }

        // 1. 击杀棒：输出当前祭坛结构的信息（调试）
        if (heldItem.is(ModItems.KILL_STICK.get())) {
            if (!(state.getBlock() instanceof AbstractAltarBlock altarBlock)) {
                return InteractionResult.PASS;
            }
            AltarStructure data = altarBlock.findAltarStructure(level, pos);
            if (data == null || data.allPositions.isEmpty()) {
                sendTo(player, Component.translatable("altar_debug.epca.not_structure"));
                return InteractionResult.SUCCESS;
            }
            sendTo(player, Component.translatable("altar_debug.epca.pedestal_count", data.pedestalCount));
            sendTo(player, Component.translatable("altar_debug.epca.pedestals_with_item", data.pedestalsWithItem));
            sendTo(player, Component.translatable("altar_debug.epca.total_points", data.totalPoints));
            sendTo(player, Component.translatable(data.isValid
                    ? "altar_debug.epca.status.valid" : "altar_debug.epca.status.invalid"));
            if (!data.isValid && data.invalidReason != null) {
                sendTo(player, Component.translatable("altar_debug.epca.reason", data.invalidReason));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // ═══════════════════ 献祭仪式 ═══════════════════

    /**
     * 献祭触发的判定结果（I17：把原来的 {@code boolean} 换成可区分的原因）。
     *
     * <p>常量本身与判定顺序一字未改；现在它只用于<b>区分判定分支</b>，不再直接呈现给玩家：
     * 原先的 {@code reportable} 标记与「该报哪条失败」的谓词（{@code reportable(ItemStack)}）
     * 是「右键失败提示」这条已删除路径的一部分，因此随之删除；原有前置条件
     * （底座方块 / 祭台上的宝石块 / 无寄生体 / ≥2 动物 / ≥1 村民 / 不重复开仪式）完全不变。
     * {@link #langKey()} 仍被 {@link #performSacrifice} 的兜底分支使用。
     */
    public enum SacrificeCheck {
        OK,
        NO_BASE,
        NO_GEM,
        PARASITE_PRESENT,
        NOT_ENOUGH_ANIMALS,
        NOT_ENOUGH_VILLAGERS,
        ALREADY_RUNNING;

        public String langKey() {
            return "ritual.epca." + name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * 一次仪式判定的产物：判定结果、判定过程中收集到的祭品，以及判定通过时的服务端世界。
     *
     * <p>判定与执行因此被彻底拆开：{@link #evaluateSacrifice} 只读世界，不产生任何副作用
     * （无消息、无粒子、不入队），只有 {@code result == OK} 时调用方才拿着同一份祭品列表去执行
     * {@link #performSacrifice}——不会二次扫描实体，也不会出现「判定通过但执行时条件已经变了」。
     * 判定失败时 {@code serverLevel} 为 {@code null}、祭品列表为空。
     */
    private record SacrificePlan(SacrificeCheck result, @Nullable ServerLevel serverLevel,
                                 List<Animal> animals, List<LivingEntity> villagers) {
        static SacrificePlan failed(SacrificeCheck result) {
            return new SacrificePlan(result, null, List.of(), List.of());
        }

        boolean isOk() {
            return result == SacrificeCheck.OK;
        }
    }

    /**
     * 判定献祭仪式的全部前置条件（<b>无副作用</b>：不发消息、不喷粒子、不入队、不改动世界）。
     *
     * <p>与移植前的差别：返回值从 {@code boolean} 变成 {@link SacrificePlan}（判定结果 + 祭品），
     * 并且到此为止只判定、不执行——原来这里的最后两步（{@code performSacrifice} 与
     * 「{@code OK}」）被移到调用方，只有全部条件通过才会发生。判定条件本身一字未改，
     * 包括「该祭坛是否已有仪式在跑」（I19b）。
     */
    private SacrificePlan evaluateSacrifice(Level level, BlockPos pos) {
        if (level.isClientSide()) return SacrificePlan.failed(SacrificeCheck.NO_BASE);
        if (!(level instanceof ServerLevel serverLevel)) return SacrificePlan.failed(SacrificeCheck.NO_BASE);

        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        if (!isBeaconBaseBlock(belowState)) return SacrificePlan.failed(SacrificeCheck.NO_BASE);

        if (!(level.getBlockEntity(pos) instanceof AltarItemContainer pedestal)) {
            return SacrificePlan.failed(SacrificeCheck.NO_GEM);
        }
        ItemStack stored = pedestal.getItem();
        if (!isGemBlockItem(stored)) return SacrificePlan.failed(SacrificeCheck.NO_GEM);

        List<Animal> animals = new ArrayList<>();
        List<LivingEntity> villagers = new ArrayList<>();
        AABB sphere = new AABB(pos).inflate(RITUAL_SPHERE_RADIUS);
        List<Entity> entities = level.getEntities(null, sphere);
        boolean parasitePresent = false;
        for (Entity e : entities) {
            if (e.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    <= RITUAL_SPHERE_RADIUS * RITUAL_SPHERE_RADIUS) {
                if (IParasite.isParasiteNoLivingByTagOrInterface(e)) {
                    // 与原实现一致：只要球内有寄生体就整体否决。区别只是现在能给出原因。
                    parasitePresent = true;
                    break;
                }
                if (!e.isAlive()) continue;
                if (e instanceof Animal animal && !(e instanceof Monster)) {
                    animals.add(animal);
                } else if (isVillagerLike(e) && e instanceof LivingEntity living) {
                    villagers.add(living);
                }
            }
        }
        if (parasitePresent) return SacrificePlan.failed(SacrificeCheck.PARASITE_PRESENT);
        if (animals.size() < 2) return SacrificePlan.failed(SacrificeCheck.NOT_ENOUGH_ANIMALS);
        if (villagers.isEmpty()) return SacrificePlan.failed(SacrificeCheck.NOT_ENOUGH_VILLAGERS);

        // I19b：同一个祭坛不允许叠加第二个仪式（原来会再排一条队列，两个仪式互相抢方块）
        if (BlockConversionManager.getInstance().hasSacrificeTask(serverLevel, pos)) {
            return SacrificePlan.failed(SacrificeCheck.ALREADY_RUNNING);
        }

        // 全部前置条件成立：把同一份祭品列表交给执行阶段（这里到此为止没有任何世界改动）。
        return new SacrificePlan(SacrificeCheck.OK, serverLevel, animals, villagers);
    }

    /**
     * 执行仪式：移除生物、给出献祭反馈、快照半径内的方块、清空祭台、排入转换队列。
     *
     * <p>移除方式仍是 {@link Entity.RemovalReason#DISCARDED}（没有死亡动画），
     * 但每只生物在消失前都会有自己的粒子与音效，所以不再是「静默消失」。
     */
    private void performSacrifice(ServerLevel level, BlockPos pos, Player player,
                                  List<Animal> animals, List<LivingEntity> villagers) {
        int animalRemoved = 0;
        for (Animal a : animals) {
            if (animalRemoved >= 2) break;
            sacrificeFeedback(level, a);
            a.remove(Entity.RemovalReason.DISCARDED);
            animalRemoved++;
        }
        if (!villagers.isEmpty()) {
            LivingEntity victim = villagers.get(0);
            sacrificeFeedback(level, victim);
            victim.remove(Entity.RemovalReason.DISCARDED);
        }

        List<BlockPos> positions = new ArrayList<>();
        int maxRadius = RITUAL_SCAN_RADIUS;
        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dy = -maxRadius; dy <= maxRadius; dy++) {
                for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                    BlockPos targetPos = pos.offset(dx, dy, dz);
                    double dist = Math.sqrt(pos.distSqr(targetPos));
                    if (dist > maxRadius) continue;
                    BlockState state = level.getBlockState(targetPos);
                    if (state.isAir()) continue;
                    positions.add(targetPos);
                }
            }
        }

        positions.sort(Comparator.comparingDouble(p -> p.distSqr(pos)));

        if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal) {
            pedestal.clearItem();
        }

        // 仪式起手式：让「开始了」这件事立刻可见可听（原来是完全静默的 10~70 秒）
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 1.0;
        double cz = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 20, 0.5, 0.5, 0.5, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz, 10, 0.4, 0.3, 0.4, 0.01);
        level.playSound(null, pos, SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.7F, 0.6F);

        boolean queued = false;
        if (player instanceof ServerPlayer serverPlayer) {
            queued = BlockConversionManager.getInstance().addSacrificeTask(level, pos, player.getUUID(), positions,
                    () -> {
                        sendTo(serverPlayer, Component.translatable(SacrificeCheck.ALREADY_RUNNING.langKey()));
                        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(),
                                SoundSource.BLOCKS, 0.6F, 0.5F);
                    });
        }
        if (!queued) {
            // 极端情况（玩家不是 ServerPlayer / 覆盖层拒绝）：方块快照尚未产生任何副作用，直接返回。
            reportFailure(player, SacrificeCheck.ALREADY_RUNNING);
            return;
        }

        sendTo(player, Component.translatable("ritual.epca.started", positions.size()));
        for (ServerPlayer nearby : nearbyPlayers(level, pos, 32.0)) {
            if (nearby != player) {
                sendTo(nearby, Component.translatable("ritual.epca.started_nearby"));
            }
        }
    }

    /** 单个被献祭生物的反馈：灵魂粒子 + 一声闷响。 */
    private static void sacrificeFeedback(ServerLevel level, LivingEntity victim) {
        double x = victim.getX();
        double y = victim.getY() + victim.getBbHeight() * 0.5;
        double z = victim.getZ();
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 12, 0.35, 0.45, 0.35, 0.02);
        level.sendParticles(ModParticles.BLEEDING.get(), x, y, z, 6, 0.3, 0.3, 0.3, 0.01);
        level.playSound(null, x, y, z, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 0.9F, 0.8F);
    }

    private static List<ServerPlayer> nearbyPlayers(ServerLevel level, BlockPos pos, double radius) {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer candidate : level.players()) {
            if (candidate.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    <= radius * radius) {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * 把失败原因发给玩家（I17）。用动作栏，与祭坛合成失败的反馈风格一致。
     *
     * <p>现在只剩一个调用点：{@link #performSacrifice} 里「队列拒绝」的兜底分支
     * （玩家不是 {@code ServerPlayer}，或在判定与入队之间祭坛被别人抢先占用）。
     * 判定不通过的前置条件不再走这里——那条路径改为返回 {@code PASS} 交给祭坛合成。
     */
    private static void reportFailure(Player player, SacrificeCheck check) {
        sendTo(player, Component.translatable(check.langKey()));
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(),
                    SoundSource.PLAYERS, 0.5F, 0.6F);
        }
    }

    private static boolean isBeaconBaseBlock(BlockState state) {
        return state.is(Blocks.IRON_BLOCK) || state.is(Blocks.GOLD_BLOCK) ||
                state.is(Blocks.DIAMOND_BLOCK) || state.is(Blocks.EMERALD_BLOCK) ||
                state.is(Blocks.NETHERITE_BLOCK);
    }

    private static boolean isGemBlockItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.DIAMOND_BLOCK || item == Items.EMERALD_BLOCK || item == Items.AMETHYST_BLOCK;
    }

    /**
     * 26.1.2: {@code Player#displayClientMessage(Component, boolean)} was deleted; that action-bar
     * variant now lives on {@code ServerPlayer#sendSystemMessage(Component, boolean)}. On the client
     * this is a no-op, matching the old behaviour (these messages only came from the server-side
     * altar interaction path).
     */
    private static void sendTo(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
        }
    }

    private static boolean isVillagerLike(Entity entity) {
        return entity instanceof AbstractVillager || entity instanceof Pillager ||
                entity instanceof Witch || entity instanceof Vindicator ||
                entity instanceof Evoker;
    }
}
