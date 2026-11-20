package org.tdddd.epca.impl.overworld.data.organ;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

    private static final String TAG_HEAD_INNER_DEFAULTS = "HeadInnerDefaultsApplied";

    private static final String TAG_TORSO_INNER_DEFAULTS = "TorsoInnerDefaultsApplied";

    /**
     * 2026-11 布局改造前的总槽位数：16 个紫色外器官槽 + 27 躯干内部 + 9 头部内部 = 52。
     * 旧存档按 {@link #LEGACY_INDEX_MAP} 重映射到新布局（35 格）。
     */
    public static final int LEGACY_TOTAL_SLOTS = 52;

    /**
     * 旧下标 → 新下标。旧布局与新布局的前 16 格顺序完全一致（头/躯干紫槽 + 四肢紫槽，
     * 四肢紫槽在新布局里原地变成 1×3 内器官槽），躯干内部 27→15、头部内部 9→4 取前若干格。
     */
    private static final int[] LEGACY_INDEX_MAP = buildLegacyIndexMap();

    private static int[] buildLegacyIndexMap() {
        int[] map = new int[LEGACY_TOTAL_SLOTS];
        java.util.Arrays.fill(map, -1);
        for (int i = 0; i < 16; i++) {
            map[i] = i;
        }
        for (int i = 0; i < 15; i++) {
            map[16 + i] = 16 + i;
        }
        for (int i = 0; i < 4; i++) {
            map[43 + i] = 31 + i;
        }
        return map;
    }

    /**
     * 躯干内部默认填充（15 格，局部下标 0..14，从 0 开始计数）：
     * <b>第 3 个槽（局部下标 2）放 1 个病变心脏</b>，其余 14 格全部放虫染肉。
     *
     * <p>旧的「11 血肉 + 3 骨 + 1 心脏（心脏在 local 4）」配置已全部移除。</p>
     */
    public static final int[] DEFAULT_TORSO_INNER_HEART_LOCALS = {2};

    public static final int[] DEFAULT_TORSO_INNER_FLESH_LOCALS = {
            0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};

    public static final int DEFAULT_TORSO_INNER_FLESH = DEFAULT_TORSO_INNER_FLESH_LOCALS.length;

    public static final int DEFAULT_TORSO_INNER_HEARTS = DEFAULT_TORSO_INNER_HEART_LOCALS.length;

    /** 现在默认填充不再放骨头 / 其它器官。 */
    public static final int DEFAULT_TORSO_INNER_BONES = 0;

    /**
     * 上一版默认填充（11 虫染肉 + 3 虫染骨 + 1 病变心脏，心脏在 local 4）。
     * 只有「旧存档里恰好还是这套默认值」时，首次读取才会迁移成新默认；
     * 玩家自行改过的器官数据一律不动。
     */
    private static final int PREVIOUS_TORSO_INNER_SLOT_COUNT = 15;

    private static final int PREVIOUS_TORSO_INNER_HEART_LOCAL = 4;

    private static final int[] PREVIOUS_TORSO_INNER_BONE_LOCALS = {2, 6, 11};

    private static final int[] PREVIOUS_TORSO_INNER_FLESH_LOCALS = {
            0, 1, 3, 5, 7, 8, 9, 10, 12, 13, 14};

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

        data.fillTorsoInnerDefaults();

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
        if (!isPreviousTorsoInnerDefault()) {
            return true;
        }

        fillTorsoInnerDefaults();
        return true;
    }

    /** 把躯干内部 15 格灌成当前默认：local 2 = 1 个病变心脏，其余 14 格 = 虫染肉。 */
    private void fillTorsoInnerDefaults() {
        fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_FLESH_LOCALS,
                ModItems.INFESTED_FLESH.get());
        fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_HEART_LOCALS,
                ModItems.DISEASED_HEART.get());
    }

    /**
     * 旧存档是否还停留在上一版默认填充（11 虫染肉 + 3 虫染骨 + 1 病变心脏，心脏在 local 4）。
     * 只有完全吻合才视为「未改动过的默认值」，可以安全迁移到新默认。
     */
    private boolean isPreviousTorsoInnerDefault() {
        OrganSlotGroup group = OrganSlotGroup.TORSO_INNER;
        if (group.size() != PREVIOUS_TORSO_INNER_SLOT_COUNT) {
            return false;
        }
        for (int local = 0; local < group.size(); local++) {
            int index = group.globalIndex(local);
            if (local == PREVIOUS_TORSO_INNER_HEART_LOCAL) {
                if (!isSingleItem(index, ModItems.DISEASED_HEART.get())) return false;
            } else if (containsLocal(PREVIOUS_TORSO_INNER_BONE_LOCALS, local)) {
                if (!isSingleItem(index, ModItems.INFESTED_BONE.get())) return false;
            } else if (containsLocal(PREVIOUS_TORSO_INNER_FLESH_LOCALS, local)) {
                if (!isSingleItem(index, ModItems.INFESTED_FLESH.get())) return false;
            } else {
                return false;
            }
        }
        return true;
    }

    private static boolean containsLocal(int[] locals, int local) {
        for (int value : locals) {
            if (value == local) return true;
        }
        return false;
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

            itemList.add(items[i].isEmpty() ? new CompoundTag() : items[i].save(new CompoundTag()));
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
        ListTag itemList = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        byte[] unlockedBytes = tag.getByteArray(TAG_UNLOCKED);

        if (itemList.size() == LEGACY_TOTAL_SLOTS && size != LEGACY_TOTAL_SLOTS) {
            // 旧存档（52 格布局）→ 新布局（35 格）重映射，避免物品错位。
            for (int old = 0; old < LEGACY_TOTAL_SLOTS; old++) {
                int mapped = LEGACY_INDEX_MAP[old];
                if (mapped < 0 || mapped >= size) continue;
                items[mapped] = ItemStack.of(itemList.getCompound(old));
                if (old < unlockedBytes.length) {
                    unlocked[mapped] = unlockedBytes[old] != 0;
                }
            }
            applyDefaultUnlocked();
        } else {
            for (int i = 0; i < size; i++) {
                if (i < itemList.size()) {
                    items[i] = ItemStack.of(itemList.getCompound(i));
                } else {
                    items[i] = ItemStack.EMPTY;
                }
            }

            for (int i = 0; i < size; i++) {
                if (i < unlockedBytes.length) {
                    unlocked[i] = unlockedBytes[i] != 0;
                }
            }
        }

        if (tag.contains(TAG_HEAD_INNER_DEFAULTS)) {
            headInnerDefaultsApplied = tag.getBoolean(TAG_HEAD_INNER_DEFAULTS);
        }
        if (tag.contains(TAG_TORSO_INNER_DEFAULTS)) {
            torsoInnerDefaultsApplied = tag.getBoolean(TAG_TORSO_INNER_DEFAULTS);
        }
    }

    private void applyDefaultUnlocked() {
        for (OrganSlotGroup group : OrganSlotGroup.ALL) {
            if (!group.kind().defaultUnlocked()) continue;
            for (int i = 0; i < group.size(); i++) {
                unlocked[group.globalIndex(i)] = true;
            }
        }
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
