package org.tdddd.epca.impl.overworld.registry.entities.entity.special.illusion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Event driven "change log" recorder for the {@link IllusoryButterflyEntity} past phase.
 *
 * <p>The rewind covers a <b>sphere of 64 blocks</b> (four chunks) around the butterfly, which is roughly
 * 1.1 million positions. Snapshotting that volume every tick is impossible, so nothing is sampled at all:
 * the recorder only listens to the world events that actually change something and appends one entry per
 * change to the butterfly's log. A tick in which nobody built or broke anything costs zero work.</p>
 *
 * <p>Only changes that happen while a butterfly is inside its record window
 * ({@code tickCount in [2300, 3200)}), in the very same {@link ServerLevel} and within
 * {@link #RECORD_RADIUS} blocks of it are kept. The butterfly itself owns the log
 * ({@link IllusoryButterflyEntity#isRecordWindow()}) so a reload wipes it together with the entity and a
 * fight can never rewind a change that belongs to another dimension or another butterfly.</p>
 *
 * <p>What is recorded:</p>
 * <ul>
 *     <li>{@link BlockEvent.BreakEvent} — blocks destroyed by a player (old state only, the replacement is
 *         air; the rewind only ever writes the old state back).</li>
 *     <li>{@link BlockEvent.EntityPlaceEvent} — blocks placed by a player or by any entity
 *         ({@code BlockSnapshot#getReplacedBlock()} is the state that was there before the placement), and
 *         every position of an {@link BlockEvent.EntityMultiPlaceEvent} (beds, doors, ...).</li>
 *     <li>{@link EntityJoinLevelEvent} — entities that really spawned (chunk reloads are skipped through
 *         {@link EntityJoinLevelEvent#loadedFromDisk()}), so the rewind can remove them again.</li>
 *     <li>{@link LivingDeathEvent} — entities that died, with everything needed to bring them back.</li>
 *     <li>{@link EntityLeaveLevelEvent} — entities that left / despawned. The rewind re-adds them only when
 *         the entity is missing <em>and</em> its chunk is loaded, so a chunk unload can never duplicate a
 *         mob.</li>
 * </ul>
 *
 * <p>Not covered on purpose: explosions, pistons, fluid spread, fire and every other change that has no
 * Forge event (or only a {@code NeighborNotifyEvent}) are not part of the rewind; the vanilla
 * {@code EntityPlaceEvent} list above is what the stage actually needs. The tail phase black hole destroys
 * blocks through {@code ServerLevel#destroyBlock}, which fires none of these events, and it runs long after
 * the record window (6300 ticks &gt; 3200) so it must not be undone either.</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IllusionRewindRecorder {

    /** "半径 4 个区块" == 64 blocks, measured as a sphere around the butterfly. */
    public static final double RECORD_RADIUS = 64.0D;
    public static final double RECORD_RADIUS_SQR = RECORD_RADIUS * RECORD_RADIUS;

    /**
     * Butterflies that are currently inside their record window. Normally empty or a single element; the
     * copy-on-write list keeps the (very hot) block events allocation free and safe against a rebuild while
     * an event handler is iterating it.
     */
    private static final List<IllusoryButterflyEntity> RECORDING = new CopyOnWriteArrayList<>();

    private IllusionRewindRecorder() {
    }

    /** Arms the recorder for one butterfly. Idempotent; called on the record phase entry and after a reload. */
    public static void begin(IllusoryButterflyEntity butterfly) {
        if (!RECORDING.contains(butterfly)) {
            RECORDING.add(butterfly);
        }
    }

    /** Disarms the recorder for one butterfly. Idempotent. */
    public static void end(IllusoryButterflyEntity butterfly) {
        RECORDING.remove(butterfly);
    }

    /** Drops every registration (server shutdown / world unload) so no entity reference outlives a world. */
    public static void clear() {
        RECORDING.clear();
    }

    public static boolean isRecording(IllusoryButterflyEntity butterfly) {
        return RECORDING.contains(butterfly);
    }

    // ------------------------------------------------------------------------------------------------
    // block changes
    // ------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (RECORDING.isEmpty()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState destroyed = event.getState();
        for (IllusoryButterflyEntity butterfly : RECORDING) {
            butterfly.onBlockBroken(level, pos, destroyed);
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (RECORDING.isEmpty()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        // the event is fired after the placement went through, so the live state is the new one and the
        // snapshot holds the state that was replaced (this is also the only reliable "old state" source for
        // the entity placement path, where getPlacedBlock() is the replaced block instead of the placed one)
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiPlace) {
            for (BlockSnapshot snapshot : multiPlace.getReplacedBlockSnapshots()) {
                BlockPos pos = snapshot.getPos();
                BlockState oldState = snapshot.getReplacedBlock();
                BlockState newState = level.getBlockState(pos);
                for (IllusoryButterflyEntity butterfly : RECORDING) {
                    butterfly.onBlockPlaced(level, pos, oldState, newState);
                }
            }
            return;
        }
        BlockPos pos = event.getPos();
        BlockState oldState = event.getBlockSnapshot().getReplacedBlock();
        // EntityPlaceEvent fires after the placement, so the live state is the placed one. Do not use
        // getPlacedBlock() here: Forge 47.2.0 returns the *replaced* state for every non player entity,
        // which would make oldState == newState and silently drop the record. oldState must stay the
        // pre-placement snapshot (getReplacedBlock), never the live level state.
        BlockState newState = level.getBlockState(pos);
        for (IllusoryButterflyEntity butterfly : RECORDING) {
            butterfly.onBlockPlaced(level, pos, oldState, newState);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // entity changes
    // ------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (RECORDING.isEmpty()) {
            return;
        }
        if (event.loadedFromDisk()) {
            // a chunk (re)load is not a spawn: the entity already existed before the window and must not be
            // discarded by the rewind
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        for (IllusoryButterflyEntity butterfly : RECORDING) {
            butterfly.onEntitySpawned(level, entity);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (RECORDING.isEmpty()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        for (IllusoryButterflyEntity butterfly : RECORDING) {
            butterfly.onEntityDied(level, entity);
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (RECORDING.isEmpty()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        for (IllusoryButterflyEntity butterfly : RECORDING) {
            butterfly.onEntityLeft(level, entity);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // lifecycle: never keep an entity of a world that is gone
    // ------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (RECORDING.isEmpty()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        RECORDING.removeIf(butterfly -> butterfly.level() == level);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        clear();
    }
}
