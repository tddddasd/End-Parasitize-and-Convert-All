package org.tdddd.epca.impl.overworld.registry.blocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.utils.NbtSafe;

import java.util.function.Supplier;

/**
 * Block entity shared by the three infested web blocks (spider / cave spider / blood).
 *
 * <p>It only stores the expiry timestamp (a game time), nothing else.</p>
 *
 * <p>Why this exists: the expiry used to live in {@code IntegerProperty("placed_at", 0, 2047)} on the
 * block state, so every web block contributed 2048 states (4096 with {@code spider}). Anything that
 * enumerates all block states -- WorldEdit 7.2.15 does it on {@code ServerStartedEvent} -- stalled for
 * minutes building the state map. Keeping the stamp on a block entity leaves the block with two states.</p>
 *
 * <p>Semantics, unchanged from the old {@code PLACED_NEVER} value:</p>
 * <ul>
 *   <li>{@code expiryTime == 0} ({@link #NO_EXPIRY}) means "not set": the web never decays by itself.</li>
 *   <li>Otherwise the web decays once {@code level.getGameTime() >= expiryTime}.</li>
 *   <li>A missing block entity (old save, schematic, structure block, WorldEdit paste) is treated as
 *       "not set" as well and never throws.</li>
 * </ul>
 */
public class InfestedWebBlockEntity extends BlockEntity {
    /** NBT key of the expiry stamp. */
    public static final String TAG_EXPIRY_TIME = "ExpiryTime";

    /** Sentinel for "never decays". */
    public static final long NO_EXPIRY = 0L;

    private long expiryTime = NO_EXPIRY;

    public InfestedWebBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Block entity factory for {@code ModBlocks.INFESTED_SPIDER_WEB}. */
    public static InfestedWebBlockEntity spider(BlockPos pos, BlockState state) {
        return new InfestedWebBlockEntity(ModBlockEntities.INFESTED_SPIDER_WEB.get(), pos, state);
    }

    /** Block entity factory for {@code ModBlocks.INFESTED_CAVE_SPIDER_WEB}. */
    public static InfestedWebBlockEntity caveSpider(BlockPos pos, BlockState state) {
        return new InfestedWebBlockEntity(ModBlockEntities.INFESTED_CAVE_SPIDER_WEB.get(), pos, state);
    }

    /** Block entity factory for {@code ModBlocks.INFESTED_SPIDER_WEB_BLOOD}. */
    public static InfestedWebBlockEntity blood(BlockPos pos, BlockState state) {
        return new InfestedWebBlockEntity(ModBlockEntities.INFESTED_SPIDER_WEB_BLOOD.get(), pos, state);
    }

    public long getExpiryTime() {
        return this.expiryTime;
    }

    public boolean hasExpiry() {
        return this.expiryTime != NO_EXPIRY;
    }

    public void setExpiryTime(long expiryTime) {
        if (this.expiryTime == expiryTime) {
            return;
        }
        this.expiryTime = expiryTime;
        this.setChanged();
    }

    /**
     * Stamps {@code now + decayTicks} on the web at {@code pos}, unless it already carries a stamp.
     *
     * <p>1.20.1 calls {@code Block#onPlace} <em>before</em> {@code LevelChunk} creates the block entity,
     * so on the first placement there is nothing to write to yet; that case creates the entity here and
     * {@code LevelChunk} then reuses it instead of creating a second one. If the entity cannot be created
     * the web simply stays unstamped (never decays) rather than throwing.</p>
     */
    public static void stampExpiry(Level level, BlockPos pos, int decayTicks, Supplier<BlockEntity> factory) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            blockEntity = factory.get();
            if (blockEntity == null) {
                return;
            }
            level.setBlockEntity(blockEntity);
        }
        if (blockEntity instanceof InfestedWebBlockEntity web && !web.hasExpiry()) {
            web.setExpiryTime(level.getGameTime() + (long) decayTicks);
        }
    }

    /** True when the web at {@code pos} carries an expiry that has been reached. */
    public static boolean isExpired(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof InfestedWebBlockEntity web
                && web.hasExpiry()
                && level.getGameTime() >= web.getExpiryTime();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // defensive: schematic / structure / older data may be missing the key or store another type
        this.expiryTime = NbtSafe.getLong(tag, TAG_EXPIRY_TIME, NO_EXPIRY);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.expiryTime != NO_EXPIRY) {
            tag.putLong(TAG_EXPIRY_TIME, this.expiryTime);
        }
    }
}
