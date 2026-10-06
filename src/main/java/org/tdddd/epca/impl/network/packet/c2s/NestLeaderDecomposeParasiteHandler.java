package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.events.BiomassEventHandler;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import javax.annotation.Nullable;

/**
 *
 *
 * <p><b></b><b></b>
 *  {@code NestLeaderOrganKeys#DECOMPOSE_PARASITE}</p>
 * <ol>
 *   <li></li>
 *   <li></li>
 *   <li><b></b> / </li>
 * </ol>
 *
 * <h2></h2>
 * <p>C2S  {@link NestLeaderDecomposeParasitePacket}
 * {@code ServerPlayer#getEyePosition()} + {@code getLookAngle()}
 * {@link #pickParasite}  id /
 * </p>
 *
 * <h2></h2>
 * <p><b></b>{@code BiomassManager}
 * {@code BiomassPoints}  {@code Player#getPersistentData()}
 * {@code BiomassManager.java}  6-18  synced data
 *
 *
 * {@link BiomassEventHandler#computeParasiteBiomass(LivingEntity)}
 * <b></b>
 * </p>
 *
 * <h2> {@code discard()}  {@code hurt}/{@code kill}</h2>
 * <p>{@code Entity#discard()}{@code Entity.java}  331-333
 * {@code remove(RemovalReason.DISCARDED)}  {@code setRemoved(...)} 3357
 * <b></b> {@code LivingEntity#die()} 1303
 * {@code ForgeHooks.onLivingDeath} 1304    {@code LivingDeathEvent}
 * {@code dropAllDeathLoot} 1327 </p>
 * <ul>
 *   <li>{@code BiomassEventHandler#onLivingDeath} </li>
 *   <li> override  {@code die()}  {@code this.onDeath(source)} </li>
 *   <li> /  /  /  3 </li>
 * </ul>
 * <p>
 * {@code BiomassEventHandler.java}  64  {@code target.remove(RemovalReason.DISCARDED)}
 * </p>
 *
 * <h2></h2>
 * <p> {@code LivingEntity#dropFromLootTable(DamageSource, boolean)}
 *  1.20.1  {@code protected}{@code LivingEntity.java}  1402
 *  {@code LivingEntity}
 * {@code dropAllDeathLoot} 1360  {@code dropCustomDeathLoot} +
 * {@code dropEquipment} + {@code dropExperience}
 *  {@code dropFromLootTable}  1402-1412
 * {@link LootParams} {@code LootContextParamSets.ENTITY}
 *  luck {@code Entity#spawnAtLocation(ItemStack)}
 * {@code Entity.java}  1815 public AccessTransformer
 *  {@code accesstransformer.cfg}</p>
 */
public final class NestLeaderDecomposeParasiteHandler {

    private NestLeaderDecomposeParasiteHandler() {
    }

    /**  {@link NestLeaderDecomposeParasitePacket#handle}  */
    public static void handleRequest(@Nullable ServerPlayer player) {
        if (player == null) return;
        // ""
        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        LivingEntity target = pickParasite(player);
        //  ->
        if (target == null) return;

        // 1)  epca_kill_count
        int points = BiomassEventHandler.computeParasiteBiomass(target);

        // 2)
        //     = IParasite#BIOMASS_SPAWNED_KEY IParasite#isBiomassSpawned
        boolean biomassSpawned = target instanceof IParasite parasite && parasite.isBiomassSpawned();
        if (!biomassSpawned) {
            dropLootTableOnly(serverLevel, target, player);
        }

        // 3)  die() LivingDeathEvent /  /
        target.discard();

        // 4)  +  +
        //    BiomassEventHandler.java  66-73  BiomassEventHandler#addPointsAndSync
        serverLevel.sendParticles(ModParticles.BIOMASS.get(),
                target.getX(), target.getY() + 0.5D, target.getZ(),
                points, 0.5D, 0.5D, 0.5D, 0.05D);

        BiomassEventHandler.addPointsAndSync(player, points);
        player.displayClientMessage(Component.literal("分解寄生体：+" + points + " 生物质点数"), true);
    }

    /**
     *  {@code null}
     *
     * <h2>/Forge </h2>
     * <ul>
     *   <li><b></b> Forge
     *       {@code Player#getEntityReach()}{@code IForgePlayer.java}  29-33
     *        3.0 +3 {@code ForgeMod.java}  169
     *       {@code NestLeaderOrganEffects.java}  113
     *       {@code OrganStatSummary.java}  716
     *
     *        0 {@code IForgePlayer}  26 </li>
     *   <li> {@code ProjectileUtil#getEntityHitResult(Entity, Vec3, Vec3, AABB,
     *       Predicate, double)}{@code ProjectileUtil.java}  52
     *        {@code GameRenderer#pick} 724-730
     *        {@code getEyePosition()} {@code from + look * reach}
     *        {@code getBoundingBox().expandTowards(look * reach).inflate(1.0)}
     *        {@code !isSpectator() && isPickable()}
     *        {@code reach * reach} {@code d0}
     *        54  70/79 </li>
     *   <li><b></b>
     *        {@code ProjectileUtil#getHitResult} 36-49
     *        38-41  clip 43
     *        private
     *       </li>
     * </ul>
     *
     * <h2></h2>
     * <ul>
     *   <li> {@code LivingEntity}</li>
     *   <li>{@code instanceof IParasite}</li>
     *   <li><b></b> {@code Player} {@code PlayerMixin}  {@code Player}
     *       {@code implements IParasite}{@code PlayerMixin.java}  20
     *       </li>
     *   <li>{@code Entity#isAlive()}{@code Entity.java}  1835 </li>
     *   <li>{@code level()} </li>
     * </ul>
     * <p><b></b>
     *
     * </p>
     */
    @Nullable
    public static LivingEntity pickParasite(ServerPlayer player) {
        double reach = player.getEntityReach();
        if (reach <= 0.0D) return null;

        Vec3 from = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 to = from.add(look.x * reach, look.y * reach, look.z * reach);

        //  ProjectileUtil#getHitResult  38-41  clip
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

        Entity picked = hit.getEntity();
        if (!(picked instanceof LivingEntity living)) return null;
        if (picked instanceof Player) return null;
        if (!(living instanceof IParasite)) return null;
        if (!living.isAlive()) return null;
        if (living.level() != player.level()) return null;
        return living;
    }

    /**
     *  {@code LivingEntity#dropFromLootTable}
     *
     *
     * <p> 1405 {@code THIS_ENTITY = }
     * {@code ORIGIN = }{@code DAMAGE_SOURCE = }
     * {@code KILLER_ENTITY / DIRECT_KILLER_ENTITY = }
     * {@code LAST_DAMAGE_PLAYER + withLuck}  1406-1408
     * </p>
     */
    private static void dropLootTableOnly(ServerLevel serverLevel, LivingEntity target, Player decomposer) {
        LootTable lootTable = serverLevel.getServer().getLootData().getLootTable(target.getLootTable());
        DamageSource source = decomposer.damageSources().playerAttack(decomposer);

        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, target)
                .withParameter(LootContextParams.ORIGIN, target.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.KILLER_ENTITY, decomposer)
                .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, decomposer)
                .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, decomposer)
                .withLuck(decomposer.getLuck())
                .create(LootContextParamSets.ENTITY);

        //  this::spawnAtLocation LivingEntity.java  1411
        lootTable.getRandomItems(params, target.getLootTableSeed(), target::spawnAtLocation);
    }
}

