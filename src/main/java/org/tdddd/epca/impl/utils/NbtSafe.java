package org.tdddd.epca.impl.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.items.ItemStackHandler;
import org.tdddd.epca.impl.epca;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Defensive NBT readers for block entity data.
 *
 * <p>Block entity NBT does not necessarily come from EPCA's own {@code save}: schematic tools
 * (WorldEdit {@code //copy} + {@code //paste}, structure blocks, other mods' cloning) copy a
 * {@link CompoundTag} verbatim into a freshly created block entity. Such a tag can be missing
 * fields, carry a wrong type or contain a value that vanilla would happily construct but EPCA's
 * invariants cannot handle. Every accessor here therefore falls back to a default instead of
 * throwing, and warns exactly once per {@code key} so a broken schematic cannot flood the log.</p>
 */
public final class NbtSafe {

    private static final Set<String> WARNED = Collections.synchronizedSet(new HashSet<>());

    private NbtSafe() {
    }

    /** Reads a UUID, tolerating a missing key and a malformed (non 4-int) array. */
    @Nullable
    public static UUID getUuid(CompoundTag tag, String key) {
        if (tag == null || !tag.contains(key, Tag.TAG_INT_ARRAY)) {
            return null;
        }
        try {
            return tag.getUUID(key);
        } catch (RuntimeException exception) {
            warn(key, exception);
            return null;
        }
    }

    public static int getInt(CompoundTag tag, String key, int fallback) {
        if (tag == null || !tag.contains(key, Tag.TAG_ANY_NUMERIC)) {
            return fallback;
        }
        return tag.getInt(key);
    }

    public static long getLong(CompoundTag tag, String key, long fallback) {
        if (tag == null || !tag.contains(key, Tag.TAG_ANY_NUMERIC)) {
            return fallback;
        }
        return tag.getLong(key);
    }

    public static boolean getBoolean(CompoundTag tag, String key, boolean fallback) {
        if (tag == null || !tag.contains(key, Tag.TAG_BYTE)) {
            return fallback;
        }
        return tag.getBoolean(key);
    }

    public static String getString(CompoundTag tag, String key, String fallback) {
        if (tag == null || !tag.contains(key, Tag.TAG_STRING)) {
            return fallback;
        }
        return tag.getString(key);
    }

    /**
     * Loads an {@link ItemStackHandler} from a tag that may have been produced elsewhere.
     *
     * <p>{@code ItemStackHandler#deserializeNBT} resizes itself from the stored {@code Size} field,
     * so a tag with a negative or absurd size throws or exhausts memory. The size is therefore
     * forced back to the handler's own slot count before deserialising; out of range slot indices
     * are dropped by Forge itself.</p>
     */
    public static void loadItemHandler(@Nullable CompoundTag tag, ItemStackHandler handler) {
        if (tag == null || tag.isEmpty()) {
            // nothing stored: keep the freshly constructed (empty) inventory
            return;
        }
        CompoundTag safe = tag.copy();
        safe.putInt("Size", handler.getSlots());
        try {
            handler.deserializeNBT(safe);
        } catch (RuntimeException exception) {
            warn("Inventory", exception);
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                handler.setStackInSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }

    /** Logs the first failure for a key, silently ignores every later one. */
    public static void warn(String key, Throwable throwable) {
        if (WARNED.add(key)) {
            epca.LOGGER.warn("[epca] ignored malformed block entity NBT field '{}': {}",
                    key, throwable.toString());
        }
    }
}
