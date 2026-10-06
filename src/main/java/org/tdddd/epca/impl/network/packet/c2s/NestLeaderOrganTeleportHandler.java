package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganSavedData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;

import javax.annotation.Nullable;

/**
 * SPEC  B
 *
 * <h2></h2>
 * <p> 33  {@value #MIN_PEARLS}
 * <b></b>
 * {@value #MAX_DISTANCE}  {@value #COOLDOWN_TICKS}
 * tick= 20 </p>
 *
 * <h2></h2>
 * <p>C2S  {@link NestLeaderOrganTeleportPacket}
 * {@code ServerPlayer#getLookAngle()}""
 *  /  / </p>
 *
 * <h2></h2>
 * <ol>
 *   <li> {@code ServerPlayer} </li>
 *   <li>{@link NestLeaderManager#isNestLeader(java.util.UUID)}   </li>
 *   <li>{@link NestLeaderOrganSavedData#readOrCreate}
 *       {@link OrganStatSummary#countIn(OrganSlotGroup, ResourceLocation)}
 *        {@link OrganSlotGroup#HEAD_INNER}{@code OrganSlotGroup.HEAD_INNER}
 *       &lt; {@value #MIN_PEARLS}  </li>
 *   <li>{@link #cooldownRemainingTicks(ServerPlayer, long)} &gt; 0 </li>
 *   <li>{@link #findSafeDestination(ServerPlayer)}
 *       <b></b></li>
 *   <li>   {@code ServerPlayer#teleportTo(double, double, double)}
 *       </li>
 * </ol>
 *
 * <h2></h2>
 * <p>{@code ServerPlayer#getPersistentData()}
 * {@value #PERSISTENT_KEY}  {@value #TAG_COOLDOWN_END}
 * {@code level.getGameTime()}<b></b>
 * {@code NestLeaderOrganSavedData}{@code nestleader_organs.dat} UUID
 * <b></b>
 * <b></b>
 *
 * / {@code NestLeaderOrganDamageHandler#clear}
 *  {@code getGameTime()} </p>
 *
 * <h2>{@link #findSafeDestination(ServerPlayer)}</h2>
 * <p><b></b><b></b>
 * </p>
 * <ol>
 *   <li> {@code from = player.getEyePosition()} {@code dir = getLookAngle().normalize()}</li>
 *   <li> 0.5  1.0  {@value #MAX_DISTANCE}
 *       {@code i = 2 .. }{@value #MAX_DISTANCE}2 {@code distance = i * 0.5}</li>
 *   <li> {@code (Mth.floor(x), Mth.floor(y), Mth.floor(z))}
 *        {@link #isSafeLanding} </li>
 *   <li>   {@code null}</li>
 * </ol>
 *
 * <h3>{@link #isSafeLanding} </h3>
 * <ul>
 *   <li>{@code Level#isLoaded(BlockPos)}1.20.1 Level.java  578
 *       </li>
 *   <li>{@code pos}  {@code pos.above()}
 *       <b></b>{@code !BlockState#blocksMotion()}BlockBehaviour.java  531
 *       <b></b>{@code BlockState#isPathfindable(level, pos, PathComputationType.LAND)}
 *        797 ""
 *       ///""</li>
 *   <li>{@code BlockState#getFluidState()}
 *        855 SPEC ""</li>
 *   <li>{@code pos.below()}<b></b>""
 *       <b></b>{@code BlockState#isFaceSturdy(level, below, Direction.UP)}
 *        879 </li>
 *   <li>{@code Level#isInWorldBounds}Level.java  161
 *       ""</li>
 * </ul>
 * <p>2  +  1.8
 *
 *  {@code FallLocation} / </p>
 *
 * <h3></h3>
 * <p><b></b> {@link #handleRequest}
 * "" 20 </p>
 */
public final class NestLeaderOrganTeleportHandler {

    /** SPEC 24  */
    public static final int MAX_DISTANCE = 24;
    /** SPEC 20 20 tick/ */
    public static final int COOLDOWN_TICKS = 400;
    /** SPEC 2  */
    public static final int MIN_PEARLS = 2;

    /** 0.5  */
    private static final double STEP = 0.5D;

    /**  idiom */
    public static final String PERSISTENT_KEY = "NestLeaderOrganTeleport";
    /**  {@code level.getGameTime()} */
    public static final String TAG_COOLDOWN_END = "cooldown_end";

    /**
     *  id {@code ModItems#INFESTED_ENDER_PEARL}
     *
     * <p> id  {@code new ItemStack(ModItems.INFESTED_ENDER_PEARL.get())}
     * {@link OrganStatSummary#countIn(OrganSlotGroup, ResourceLocation)}  id
     * </p>
     */
    public static final ResourceLocation PEARL_ID =
            new ResourceLocation("epca", "infested_ender_pearl");

    private NestLeaderOrganTeleportHandler() {
    }

    /**  {@link NestLeaderOrganTeleportPacket#handle}  */
    public static void handleRequest(@Nullable ServerPlayer player) {
        if (player == null) return;
        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;

        // ""
        OrganStatSummary summary = OrganStatSummary.compute(
                NestLeaderOrganSavedData.readOrCreate(player));
        if (summary.countIn(OrganSlotGroup.HEAD_INNER, PEARL_ID) < MIN_PEARLS) {
            return;
        }

        long now = player.level().getGameTime();
        if (cooldownRemainingTicks(player, now) > 0) {
            return;
        }

        BlockPos destination = findSafeDestination(player);
        // ""
        if (destination == null) {
            return;
        }

        // 1.20.1 ServerPlayer#teleportTo(double,double,double)  1191
        // connection.teleport(x, y, z, getYRot(), getXRot(), RelativeMovement.ROTATION)
        player.teleportTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D);
        player.fallDistance = 0.0F;

        CompoundTag tag = player.getPersistentData().getCompound(PERSISTENT_KEY);
        tag.putLong(TAG_COOLDOWN_END, now + COOLDOWN_TICKS);
        player.getPersistentData().put(PERSISTENT_KEY, tag);
    }

    /**  tick0 =  */
    public static int cooldownRemainingTicks(ServerPlayer player, long now) {
        CompoundTag tag = player.getPersistentData().getCompound(PERSISTENT_KEY);
        if (!tag.contains(TAG_COOLDOWN_END)) return 0;
        long remaining = tag.getLong(TAG_COOLDOWN_END) - now;
        return remaining <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    /**  /  {@code NestLeaderOrganDamageHandler#clear}  */
    public static void clear(ServerPlayer player) {
        if (player == null) return;
        player.getPersistentData().remove(PERSISTENT_KEY);
    }

    /**
     * <b></b> {@value #MAX_DISTANCE}
     *
     * <p><b></b> {@code null}
     * " 3 " 8
     * ""</p>
     */
    @Nullable
    public static BlockPos findSafeDestination(ServerPlayer player) {
        Vec3 from = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        if (direction == Vec3.ZERO) {
            //  0""
            return null;
        }

        int steps = (int) Math.round(MAX_DISTANCE / STEP);
        for (int i = 2; i <= steps; i++) {
            double distance = i * STEP;
            double x = from.x + direction.x * distance;
            double y = from.y + direction.y * distance;
            double z = from.z + direction.z * distance;
            BlockPos pos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            if (isSafeLanding(player, pos)) {
                return pos;
            }
        }
        return null;
    }

    /**
     * {@code pos} {@link #isSafeLanding}
     *
     * <p> {@code pos} {@code pos.above()}</p>
     */
    public static boolean isSafeLanding(ServerPlayer player, BlockPos pos) {
        if (pos == null) return false;
        // Level#isLoaded  578
        if (!player.level().isLoaded(pos)) return false;

        BlockPos head = pos.above();
        BlockPos below = pos.below();
        if (!player.level().isInWorldBounds(below)) return false;

        // ""
        if (!isPassableAt(player, pos)) return false;
        if (!isPassableAt(player, head)) return false;

        BlockState support = player.level().getBlockState(below);
        if (!support.blocksMotion()) return false;
        if (!support.getFluidState().isEmpty()) return false;
        return support.isFaceSturdy(player.level(), below, Direction.UP);
    }

    /**
     * ""
     *
     * <p>{@code blocksMotion()} ""
     * {@code isPathfindable(LAND)}  /  /  /
     * ""{@code getFluidState().isEmpty()} </p>
     */
    private static boolean isPassableAt(ServerPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        if (state.blocksMotion()) return false;
        if (!state.getFluidState().isEmpty()) return false;
        return state.isPathfindable(player.level(), pos, PathComputationType.LAND);
    }
}

