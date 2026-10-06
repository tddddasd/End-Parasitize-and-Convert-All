package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
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
 *  synced data
 *
 *
 * {@link BiomassEventHandler#computeParasiteBiomass(LivingEntity)}
 * <b></b>
 * </p>
 *
 * <h2> {@code discard()}  {@code hurt}/{@code kill}</h2>
 * <p>{@code Entity#discard()}  {@code remove(RemovalReason.DISCARDED)}
 * {@code setRemoved(...)}<b></b> {@code LivingEntity#die()}
 * {@code LivingDeathEvent}  {@code dropAllDeathLoot}</p>
 * <ul>
 *   <li>{@code BiomassEventHandler#onLivingDeath} </li>
 *   <li> override  {@code die()}  {@code this.onDeath(source)} </li>
 *   <li> /  /  /  3 </li>
 * </ul>
 * <p>
 * {@code BiomassEventHandler#onLivingAttack}  {@code RemovalReason.DISCARDED}
 * </p>
 *
 * <h2>1.20.1 -&gt; 26.1.2  API </h2>
 * <ul>
 *   <li><b></b>1.20.1  {@code Player#getEntityReach()}
 *       Forge  {@code IForgePlayer} 26.1.2
 *       {@code Attributes.ENTITY_INTERACTION_RANGE}{@code player.getAttributeValue(...)}
 *        _tmp_26src LivingEntity  getAttributeValue(Holder)
 *        / {@code OrganStatSummary}
 *       {@code KillStick} </li>
 *   <li><b></b>1.20.1  {@code LivingEntity#dropFromLootTable(DamageSource, boolean)}
 *        {@code protected} {@code LootParams}
 *       26.1.2  {@code LivingEntity#dropFromLootTable(ServerLevel, DamageSource, boolean,
 *       ResourceKey<LootTable>)}  <b>public</b>_tmp_26src LivingEntity.java  1592
 *        {@code getLootTable()}  {@code Optional<ResourceKey<LootTable>>} 1586
 *        ENTITY  luck
 *        {@code LootParams} 1.20.1
 *       {@code Server#getLootData()}26.1.2  {@code reloadableRegistries().getLootTable(...)}
 *       </li>
 *   <li><b></b>{@code Player#displayClientMessage(Component, boolean)}
 *        {@code ServerPlayer#sendSystemMessage(Component, boolean)}</li>
 * </ul>
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

        //   epca_kill_count
        int points = BiomassEventHandler.computeParasiteBiomass(target);

        //     = IParasite#BIOMASS_SPAWNED_KEY IParasite#isBiomassSpawned
        boolean biomassSpawned = target instanceof IParasite parasite && parasite.isBiomassSpawned();
        if (!biomassSpawned) {
            dropLootTableOnly(serverLevel, target, player);
        }

        //   die() LivingDeathEvent /  /
        target.discard();

        //   +  +
        serverLevel.sendParticles(ModParticles.BIOMASS.get(),
                target.getX(), target.getY() + 0.5D, target.getZ(),
                points, 0.5D, 0.5D, 0.5D, 0.05D);

        BiomassEventHandler.addPointsAndSync(player, points);
        player.sendSystemMessage(Component.literal("分解寄生体：+" + points + " 生物质点数"), true);
    }

    /**
     *  {@code null}
     *
     * <h2></h2>
     * <ul>
     *   <li><b></b> {@code Attributes.ENTITY_INTERACTION_RANGE}
     *       26.1.2  {@code forge:entity_reach}  3.0
     *        {@code <= 0} </li>
     *   <li> {@code ProjectileUtil#getEntityHitResult(Entity, Vec3, Vec3, AABB,
     *       Predicate, double)}_tmp_26src ProjectileUtil.java  97
     *       1.20.1  {@code getEyePosition()}
     *       {@code from + look * reach}
     *       {@code getBoundingBox().expandTowards(look * reach).inflate(1.0)}
     *        {@code !isSpectator() && isPickable()}
     *        {@code reach * reach}</li>
     *   <li><b></b>
     *        {@code ProjectileUtil#getHitResult}
     *       </li>
     * </ul>
     *
     * <h2></h2>
     * <ul>
     *   <li> {@code LivingEntity}</li>
     *   <li>{@code instanceof IParasite}</li>
     *   <li><b></b> {@code Player} {@code PlayerMixin}  {@code Player}
     *       {@code implements IParasite}</li>
     *   <li>{@code Entity#isAlive()}</li>
     *   <li>{@code level()} </li>
     * </ul>
     * <p><b></b>
     *
     * </p>
     */
    @Nullable
    public static LivingEntity pickParasite(ServerPlayer player) {
        // 26.1.2 Player#getEntityReach() -> Attributes.ENTITY_INTERACTION_RANGE
        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach <= 0.0D) return null;

        Vec3 from = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 to = from.add(look.x * reach, look.y * reach, look.z * reach);

        //  ProjectileUtil#getHitResult  clip
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
     *
     *
     * <p>26.1.2  public
     * {@code LivingEntity#dropFromLootTable(ServerLevel, DamageSource, boolean, ResourceKey<LootTable>)}
     * _tmp_26src LivingEntity.java  1592
     * {@code LootContextParamSets.ENTITY} THIS_ENTITY / ORIGIN / DAMAGE_SOURCE /
     * ATTACKING_ENTITY / DIRECT_ATTACKING_ENTITY LAST_DAMAGE_PLAYER + luck
     *  {@code spawnAtLocation}  1.20.1
     *  {@code playerKilled = true}
     * {@code LAST_DAMAGE_PLAYER} </p>
     */
    private static void dropLootTableOnly(ServerLevel serverLevel, LivingEntity target, Player decomposer) {
        DamageSource source = decomposer.damageSources().playerAttack(decomposer);
        // 1.20.1  LootParams getLootTable()  26.1.2  Optional
        target.getLootTable().ifPresent(key ->
                target.dropFromLootTable(serverLevel, source, true, key));
    }
}

