package org.tdddd.epca.impl.overworld.data.organ;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.ModTags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HiveLeaderOrganData {

    public static final String PERSISTENT_KEY = "HiveLeaderOrgans";

    private static final String TAG_ITEMS = "Items";
    private static final String TAG_UNLOCKED = "Unlocked";

    private static final byte[] NO_UNLOCKED_BYTES = new byte[0];

    private static final String TAG_HEAD_INNER_DEFAULTS = "HeadInnerDefaultsApplied";

    private static final String TAG_TORSO_INNER_DEFAULTS = "TorsoInnerDefaultsApplied";

    public static final int[] DEFAULT_TORSO_INNER_FLESH_LOCALS = {
            0, 1, 3, 5, 7, 8, 9, 10, 12, 13, 14, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};

    public static final int[] DEFAULT_TORSO_INNER_BONE_LOCALS = {2, 6, 11, 15};

    public static final int[] DEFAULT_TORSO_INNER_HEART_LOCALS = {4};

    public static final int DEFAULT_TORSO_INNER_FLESH = DEFAULT_TORSO_INNER_FLESH_LOCALS.length;

    public static final int DEFAULT_TORSO_INNER_HEARTS = DEFAULT_TORSO_INNER_HEART_LOCALS.length;

    public static final int DEFAULT_TORSO_INNER_BONES = DEFAULT_TORSO_INNER_BONE_LOCALS.length;

    private static final int LEGACY_TORSO_INNER_FLESH_LOCAL_COUNT = 22;

    private static final int LEGACY_TORSO_INNER_HEART_LOCAL = LEGACY_TORSO_INNER_FLESH_LOCAL_COUNT;

    private static final int LEGACY_TORSO_INNER_BONE_FIRST_LOCAL = LEGACY_TORSO_INNER_HEART_LOCAL + 1;

    private static final int LEGACY_TORSO_INNER_BONE_COUNT = 4;

    public static final int[] DEFAULT_HEAD_INNER_FLESH_LOCALS = {0, 1, 2, 3, 4, 5, 7};

    private final int size;
    private final ItemStack[] items;
    private final boolean[] unlocked;

    private boolean headInnerDefaultsApplied;

    private boolean torsoInnerDefaultsApplied;

    public HiveLeaderOrganData(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("器官槽位数量必须为正: " + size);
        }
        this.size = size;
        this.items = new ItemStack[size];
        this.unlocked = new boolean[size];
        for (int i = 0; i < size; i++) {
            this.items[i] = ItemStack.EMPTY;
        }
    }

    public static HiveLeaderOrganData createEmpty() {
        return new HiveLeaderOrganData(OrganSlotGroup.totalSlots());
    }

    public static HiveLeaderOrganData createDefault() {
        HiveLeaderOrganData data = createEmpty();

        for (OrganSlotGroup group : OrganSlotGroup.ALL) {
            if (group.kind().defaultUnlocked()) {
                for (int i = 0; i < group.size(); i++) {
                    data.unlocked[group.globalIndex(i)] = true;
                }
            }
        }

        data.fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_FLESH_LOCALS,
                ModItems.INFESTED_FLESH.get());
        data.fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_BONE_LOCALS,
                ModItems.INFESTED_BONE.get());
        data.fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_HEART_LOCALS,
                ModItems.DISEASED_HEART.get());

        for (int local : DEFAULT_HEAD_INNER_FLESH_LOCALS) {
            if (local < 0 || local >= OrganSlotGroup.HEAD_INNER.size()) continue;
            data.items[OrganSlotGroup.HEAD_INNER.globalIndex(local)] =
                    newStack(ModItems.INFESTED_FLESH.get());
        }

        data.headInnerDefaultsApplied = true;

        data.torsoInnerDefaultsApplied = true;

        return data;
    }

    public boolean applyHeadInnerDefaultsOnce() {
        if (this.headInnerDefaultsApplied) return false;
        this.headInnerDefaultsApplied = true;
        if (!isGroupEmpty(OrganSlotGroup.HEAD_INNER)) {
            return true;
        }
        for (int local : DEFAULT_HEAD_INNER_FLESH_LOCALS) {
            if (local < 0 || local >= OrganSlotGroup.HEAD_INNER.size()) continue;
            this.items[OrganSlotGroup.HEAD_INNER.globalIndex(local)] =
                    newStack(ModItems.INFESTED_FLESH.get());
        }
        return true;
    }

    private boolean isGroupEmpty(OrganSlotGroup group) {
        for (int i = 0; i < group.size(); i++) {
            if (!this.items[group.globalIndex(i)].isEmpty()) return false;
        }
        return true;
    }

    public boolean applyTorsoInnerDefaultsOnce() {
        if (this.torsoInnerDefaultsApplied) return false;
        this.torsoInnerDefaultsApplied = true;
        if (!isLegacyTorsoInnerDefault()) {
            return true;
        }

        fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_FLESH_LOCALS,
                ModItems.INFESTED_FLESH.get());
        fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_BONE_LOCALS,
                ModItems.INFESTED_BONE.get());
        fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_HEART_LOCALS,
                ModItems.DISEASED_HEART.get());
        return true;
    }

    private boolean isLegacyTorsoInnerDefault() {
        OrganSlotGroup group = OrganSlotGroup.TORSO_INNER;
        if (group.size() != LEGACY_TORSO_INNER_BONE_FIRST_LOCAL + LEGACY_TORSO_INNER_BONE_COUNT) {

            return false;
        }
        for (int local = 0; local < LEGACY_TORSO_INNER_FLESH_LOCAL_COUNT; local++) {
            if (!isSingleItem(group.globalIndex(local), ModItems.INFESTED_FLESH.get())) return false;
        }
        if (!isSingleItem(group.globalIndex(LEGACY_TORSO_INNER_HEART_LOCAL),
                ModItems.DISEASED_HEART.get())) {
            return false;
        }
        for (int local = LEGACY_TORSO_INNER_BONE_FIRST_LOCAL;
             local < LEGACY_TORSO_INNER_BONE_FIRST_LOCAL + LEGACY_TORSO_INNER_BONE_COUNT; local++) {
            if (!isSingleItem(group.globalIndex(local), ModItems.INFESTED_BONE.get())) return false;
        }
        return true;
    }

    private boolean isSingleItem(int globalIndex, net.minecraft.world.item.Item item) {
        ItemStack stack = this.items[globalIndex];
        return item != null && !stack.isEmpty() && stack.getCount() == 1 && stack.is(item);
    }

    private void fillLocals(OrganSlotGroup group, int[] locals, net.minecraft.world.item.Item item) {
        for (int local : locals) {
            if (local < 0 || local >= group.size()) continue;
            this.items[group.globalIndex(local)] = newStack(item);
        }
    }

    private static ItemStack newStack(net.minecraft.world.item.Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public int size() {
        return size;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("器官槽位下标越界: " + index + " (size=" + size + ")");
        }
    }

    public ItemStack getItem(int index) {
        checkIndex(index);
        return items[index];
    }

    public void setItem(int index, ItemStack stack) {
        checkIndex(index);
        items[index] = (stack == null) ? ItemStack.EMPTY : stack;
    }

    public void clearItem(int index) {
        setItem(index, ItemStack.EMPTY);
    }

    public boolean isUnlocked(int index) {
        checkIndex(index);
        return unlocked[index];
    }

    public boolean setUnlocked(int index, boolean value) {
        checkIndex(index);
        if (unlocked[index] == value) return false;
        unlocked[index] = value;
        return true;
    }

    public OrganSlotGroup groupOf(int index) {
        checkIndex(index);
        return OrganSlotGroup.byGlobalIndex(index);
    }

    public static boolean acceptsItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(ModTags.ORGAN_PART);
    }

    public static boolean isOrganPart(ItemStack stack) {
        return acceptsItem(stack);
    }

    public List<Integer> lockedPurpleSlots() {
        List<Integer> locked = new ArrayList<>();
        for (int index : OrganSlotGroup.purpleSlotIndices()) {
            if (!unlocked[index]) locked.add(index);
        }
        return Collections.unmodifiableList(locked);
    }

    public int unlockedPurpleCount() {
        int count = 0;
        for (int index : OrganSlotGroup.purpleSlotIndices()) {
            if (unlocked[index]) count++;
        }
        return count;
    }

    public void save(CompoundTag tag) {
        ListTag itemList = new ListTag();
        for (int i = 0; i < size; i++) {

            itemList.add(encodeStack(items[i]));
        }
        tag.put(TAG_ITEMS, itemList);

        byte[] unlockedBytes = new byte[size];
        for (int i = 0; i < size; i++) {
            unlockedBytes[i] = (byte) (unlocked[i] ? 1 : 0);
        }
        tag.putByteArray(TAG_UNLOCKED, unlockedBytes);

        if (headInnerDefaultsApplied) {
            tag.putBoolean(TAG_HEAD_INNER_DEFAULTS, true);
        }

        if (torsoInnerDefaultsApplied) {
            tag.putBoolean(TAG_TORSO_INNER_DEFAULTS, true);
        }
    }

    public void load(CompoundTag tag) {
        ListTag itemList = tag.getListOrEmpty(TAG_ITEMS);
        for (int i = 0; i < size; i++) {
            if (i < itemList.size()) {

                items[i] = ItemStack.CODEC.parse(NbtOps.INSTANCE, itemList.getCompoundOrEmpty(i))
                        .result().orElse(ItemStack.EMPTY);
            } else {
                items[i] = ItemStack.EMPTY;
            }
        }

        byte[] unlockedBytes = tag.getByteArray(TAG_UNLOCKED).orElse(NO_UNLOCKED_BYTES);
        for (int i = 0; i < size; i++) {
            if (i < unlockedBytes.length) {
                unlocked[i] = unlockedBytes[i] != 0;
            }
        }

        if (tag.contains(TAG_HEAD_INNER_DEFAULTS)) {
            headInnerDefaultsApplied = tag.getBooleanOr(TAG_HEAD_INNER_DEFAULTS, false);
        }
        if (tag.contains(TAG_TORSO_INNER_DEFAULTS)) {
            torsoInnerDefaultsApplied = tag.getBooleanOr(TAG_TORSO_INNER_DEFAULTS, false);
        }
    }

    private static CompoundTag encodeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        Tag encoded = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElse(null);
        return encoded instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    public static HiveLeaderOrganData fromNbt(CompoundTag tag) {
        HiveLeaderOrganData data = createEmpty();
        data.load(tag);
        return data;
    }

    public static HiveLeaderOrganData fromNbtOrDefaults(CompoundTag tag) {
        HiveLeaderOrganData data = createDefault();
        data.load(tag);
        return data;
    }

    @Override
    public String toString() {
        return "HiveLeaderOrganData[size=" + size
                + ", purpleUnlocked=" + unlockedPurpleCount()
                + "/" + OrganSlotGroup.purpleSlotIndices().length + "]";
    }
}
