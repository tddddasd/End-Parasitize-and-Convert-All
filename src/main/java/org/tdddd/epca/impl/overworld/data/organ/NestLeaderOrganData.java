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

/**
 * SPEC  2  1
 *
 * <h2></h2>
 * <p><b></b> {@code NestLeaderOrganSavedData}
 * {@code <world>/data/nestleader_organs.dat} UUID
 *  {@code NestLeaderSavedData}{@code nestleaders.dat}
 *  SavedData<b></b> {@code NestLeaderManager#isNestLeader(UUID)}
 *  UUID /
 * </p>
 *
 * <p> {@code Player#getPersistentData()}
 * {@link #PERSISTENT_KEY} 1.20.1  Forge
 * {@code PlayerPersisted}
 * <b></b> {@code NestLeaderOrganSavedData#readOrCreate}</p>
 *
 * <h2></h2>
 * <p> {@link OrganSlotGroup#totalSlots()}= 52<b></b>
 *  [0,2) [2,4) [4,7) [7,10) [10,13) [13,16)
 *  [16,43) [43,52) {@link OrganSlotGroup}</p>
 *
 * <h2></h2>
 * <p>{@link #save(CompoundTag)} / {@link #load(CompoundTag)} </p>
 * <ul>
 *   <li>{@code Items}{@link ListTag} i  i
 *       {@code ItemStack#save} </li>
 *   <li>{@code Unlocked}{@code byte[]} = 52 0
 *       </li>
 * </ul>
 *
 * <h2>1.20.1 -&gt; 26.1.2  API </h2>
 * <p>26.1.2  {@code CompoundTag} " Optional"
 * {@code tag.getList(name, type)}  {@code tag.getListOrEmpty(name)}
 * {@code tag.getByteArray(name)}  {@code tag.getByteArray(name).orElse()}
 *  {@code put*}/{@code putByteArray}  {@code contains}
 *  {@code Optional} {@code getBooleanOr(name, false)}
 * {@link ItemStack#save(CompoundTag)} / {@link ItemStack#of(CompoundTag)} were deleted in 26.1.2,
 *  NBT  {@link ItemStack#CODEC} {@link NbtOps#INSTANCE} {@code encodeStack}
 *  ItemStack.EMPTY</p>
 */
public final class NestLeaderOrganData {

    /**
     * <b></b>{@code Player#getPersistentData()}  {@code NestLeaderOrgans}
     *
     * <p><b></b>{@code NestLeaderOrganSavedData#readOrCreate}
     *  UUID
     *  {@code NestLeaderOrganSavedData} </p>
     */
    public static final String PERSISTENT_KEY = "NestLeaderOrgans";

    private static final String TAG_ITEMS = "Items";
    private static final String TAG_UNLOCKED = "Unlocked";
    /** {@code getByteArray(...).orElse(...)}  */
    private static final byte[] NO_UNLOCKED_BYTES = new byte[0];
    /**
     *  7  {@link #createDefault()}
     *  {@link #applyHeadInnerDefaultsOnce()}
     *
     * <p>  {@link #load(CompoundTag)}  false
     * </p>
     */
    private static final String TAG_HEAD_INNER_DEFAULTS = "HeadInnerDefaultsApplied";
    /**
     * 39  {@link #createDefault()}
     *  {@link #applyTorsoInnerDefaultsOnce()}
     *
     * <p> {@link #TAG_HEAD_INNER_DEFAULTS}
     * {@link #load(CompoundTag)}  false
     * </p>
     *
     * <p> {@code Items} / {@code Unlocked} <b></b>
     * {@code SyncNestLeaderOrgansPacket}  {@code data.save(tag)}
     * tag <b></b></p>
     */
    private static final String TAG_TORSO_INNER_DEFAULTS = "TorsoInnerDefaultsApplied";

    /**
     * 39 <b></b> +0.1
     * <b></b> 22 {@code 0,1,3,5,7,8,9,10,12,13,14,16,17,18,19,20,21,22,23,24,25,26}
     *  {@link #DEFAULT_TORSO_INNER_BONE_LOCALS}
     * {@link #DEFAULT_TORSO_INNER_HEART_LOCALS}  2461115
     *
     * <p>{@code 0..8} {@code 9..17} {@code 18..26}
     * <b> 27 </b>22 + 4 + 1 = 27
     * {@link #createDefault()}  {@link #applyTorsoInnerDefaultsOnce()}
     *     </p>
     *
     * <p> {@link OrganSlotGroup#TORSO_INNER}  {@code firstIndex()} = 16
     *  {@code 16 + local}</p>
     */
    public static final int[] DEFAULT_TORSO_INNER_FLESH_LOCALS = {
            0, 1, 3, 5, 7, 8, 9, 10, 12, 13, 14, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};

    /**
     * 39 <b></b> +1
     * {@code 2, 6, 11, 15} {@code 18, 22, 27, 31} 4
     */
    public static final int[] DEFAULT_TORSO_INNER_BONE_LOCALS = {2, 6, 11, 15};

    /**
     * 39 <b></b>
     *  +2  10  I 6  {@code 4}
     *  {@code 20} 1
     */
    public static final int[] DEFAULT_TORSO_INNER_HEART_LOCALS = {4};

    /** SPEC D5= {@link #DEFAULT_TORSO_INNER_FLESH_LOCALS}  */
    public static final int DEFAULT_TORSO_INNER_FLESH = DEFAULT_TORSO_INNER_FLESH_LOCALS.length;
    /** SPEC D5= {@link #DEFAULT_TORSO_INNER_HEART_LOCALS}  */
    public static final int DEFAULT_TORSO_INNER_HEARTS = DEFAULT_TORSO_INNER_HEART_LOCALS.length;
    /** SPEC D5= {@link #DEFAULT_TORSO_INNER_BONE_LOCALS}  */
    public static final int DEFAULT_TORSO_INNER_BONES = DEFAULT_TORSO_INNER_BONE_LOCALS.length;

    /**
     * <b></b> {@link #applyTorsoInnerDefaultsOnce()}
     *  {@code 0..21}  22  {@code 22}  1  {@code 23..26}
     * 4
     *
     * <p>{@link #DEFAULT_TORSO_INNER_FLESH_LOCALS}  27
     * <b></b> 22  + 1  + 4 </p>
     */
    private static final int LEGACY_TORSO_INNER_FLESH_LOCAL_COUNT = 22;
    /**  22  */
    private static final int LEGACY_TORSO_INNER_HEART_LOCAL = LEGACY_TORSO_INNER_FLESH_LOCAL_COUNT;
    /**  */
    private static final int LEGACY_TORSO_INNER_BONE_FIRST_LOCAL = LEGACY_TORSO_INNER_HEART_LOCAL + 1;
    /** 4  23..26 */
    private static final int LEGACY_TORSO_INNER_BONE_COUNT = 4;

    /**
     * 33 <b></b>
     * {@code 0,1,2,3,4,5,7}  1  7  6  8
     *
     * <p>{@code 0,1,2} {@code 3,4,5} {@code 6,7,8}
     *  7 <b></b>
     * {@code 02}{@code 35}{@code 68}<b></b>
     * {@code 1} / {@code 4} / {@code 7}
     *  + </p>
     *
     * <p> {@code 43,44,45,46,47,48,50}{@link OrganSlotGroup#HEAD_INNER}
     *  {@code firstIndex()} = 43 {@code 49} 6 {@code 51} 8</p>
     */
    public static final int[] DEFAULT_HEAD_INNER_FLESH_LOCALS = {0, 1, 2, 3, 4, 5, 7};

    private final int size;
    private final ItemStack[] items;
    private final boolean[] unlocked;
    /**  {@link #TAG_HEAD_INNER_DEFAULTS} */
    private boolean headInnerDefaultsApplied;
    /**  {@link #TAG_TORSO_INNER_DEFAULTS} */
    private boolean torsoInnerDefaultsApplied;

    public NestLeaderOrganData(int size) {
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

    /**
     *  52
     *
     * <p> {@link #createDefault()}
     *  {@code createDefault()} </p>
     */
    public static NestLeaderOrganData createEmpty() {
        return new NestLeaderOrganData(OrganSlotGroup.totalSlots());
    }

    /**
     * SPEC
     * <ul>
     *   <li>6 16 <b></b></li>
     *   <li> 3927 <b></b><b></b> 27
     *       22 {@link #DEFAULT_TORSO_INNER_FLESH_LOCALS}+ 4
     *        261115 {@link #DEFAULT_TORSO_INNER_BONE_LOCALS}
     *       + 1  4 {@link #DEFAULT_TORSO_INNER_HEART_LOCALS}</li>
     *   <li> 339 <b></b><b> 05  7  1 </b>
     *        7  4348  50 6  8 4951
     *        {@link #DEFAULT_HEAD_INNER_FLESH_LOCALS}</li>
     * </ul>
     */
    public static NestLeaderOrganData createDefault() {
        NestLeaderOrganData data = createEmpty();

        for (OrganSlotGroup group : OrganSlotGroup.ALL) {
            if (group.kind().defaultUnlocked()) {
                for (int i = 0; i < group.size(); i++) {
                    data.unlocked[group.globalIndex(i)] = true;
                }
            }
        }

        //  39 ->  27
        //  27
        // 22  + 1  + 4
        //  OrganStatSummary 220.1 + 12.0 41.0
        data.fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_FLESH_LOCALS,
                ModItems.INFESTED_FLESH.get());
        data.fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_BONE_LOCALS,
                ModItems.INFESTED_BONE.get());
        data.fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_HEART_LOCALS,
                ModItems.DISEASED_HEART.get());

        //  33
        for (int local : DEFAULT_HEAD_INNER_FLESH_LOCALS) {
            if (local < 0 || local >= OrganSlotGroup.HEAD_INNER.size()) continue;
            data.items[OrganSlotGroup.HEAD_INNER.globalIndex(local)] =
                    newStack(ModItems.INFESTED_FLESH.get());
        }
        //  applyHeadInnerDefaultsOnce()
        data.headInnerDefaultsApplied = true;
        //  applyTorsoInnerDefaultsOnce()
        data.torsoInnerDefaultsApplied = true;

        return data;
    }

    /**
     * 33  7
     * {@link #DEFAULT_HEAD_INNER_FLESH_LOCALS}
     *
     * <p><b></b>{@link #createDefault()}
     * 33
     * 39  7 </p>
     *
     * <p><b></b></p>
     * <ul>
     *   <li>{@link #TAG_HEAD_INNER_DEFAULTS} true   false</li>
     *   <li> false  33 <b></b>   7 </li>
     *   <li> false <b></b>  <b></b>
     *       </li>
     * </ul>
     * <p>
     *   </p>
     *
     * @return  false  true <b></b> NBT
     */
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

    /** <b></b> {@link #applyHeadInnerDefaultsOnce()}  */
    private boolean isGroupEmpty(OrganSlotGroup group) {
        for (int i = 0; i < group.size(); i++) {
            if (!this.items[group.globalIndex(i)].isEmpty()) return false;
        }
        return true;
    }

    /**
     * 39 <b></b><b></b>
     * {@link #DEFAULT_TORSO_INNER_FLESH_LOCALS} / {@link #DEFAULT_TORSO_INNER_BONE_LOCALS} /
     * {@link #DEFAULT_TORSO_INNER_HEART_LOCALS}
     *
     * <p><b></b>{@link #createDefault()}
     *
     *  0..21 22 23..26 </p>
     *
     * <p><b></b></p>
     * <ul>
     *   <li>{@link #TAG_TORSO_INNER_DEFAULTS} true   false</li>
     *   <li> false  39 <b></b>
     *       {@link #isLegacyTorsoInnerDefault()}22  0..211  22
     *       4  23..26 1  </li>
     *   <li> false <b></b>
     *        1 <b></b></li>
     * </ul>
     * <p>
     * </p>
     *
     * <p> {@code unlocked}/
     * 22  + 1  + 4 </p>
     *
     * @return  false  true <b></b> NBT
     */
    public boolean applyTorsoInnerDefaultsOnce() {
        if (this.torsoInnerDefaultsApplied) return false;
        this.torsoInnerDefaultsApplied = true;
        if (!isLegacyTorsoInnerDefault()) {
            return true;
        }
        //  27  27
        //  22 / 2461115
        fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_FLESH_LOCALS,
                ModItems.INFESTED_FLESH.get());
        fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_BONE_LOCALS,
                ModItems.INFESTED_BONE.get());
        fillLocals(OrganSlotGroup.TORSO_INNER, DEFAULT_TORSO_INNER_HEART_LOCALS,
                ModItems.DISEASED_HEART.get());
        return true;
    }

    /**
     * 39 <b></b>
     * {@link #LEGACY_TORSO_INNER_FLESH_LOCAL_COUNT}
     *
     * <p><b></b>
     * <b></b>
     *  false
     *  22  1  4
     * </p>
     *
     * <p><b> 1 </b>{@code getCount() == 1}{@link #createDefault()}
     *  createDefault  1
     * </p>
     */
    private boolean isLegacyTorsoInnerDefault() {
        OrganSlotGroup group = OrganSlotGroup.TORSO_INNER;
        if (group.size() != LEGACY_TORSO_INNER_BONE_FIRST_LOCAL + LEGACY_TORSO_INNER_BONE_COUNT) {
            //  /
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

    /** <b></b> 1  {@code item} */
    private boolean isSingleItem(int globalIndex, net.minecraft.world.item.Item item) {
        ItemStack stack = this.items[globalIndex];
        return item != null && !stack.isEmpty() && stack.getCount() == 1 && stack.is(item);
    }

    /**
     *  {@code group}  {@code locals} <b></b> {@code item}  1
     *  {@link #createDefault()}
     */
    private void fillLocals(OrganSlotGroup group, int[] locals, net.minecraft.world.item.Item item) {
        for (int local : locals) {
            if (local < 0 || local >= group.size()) continue;
            this.items[group.globalIndex(local)] = newStack(item);
        }
    }

    /**  ItemStack */
    private static ItemStack newStack(net.minecraft.world.item.Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }


    /** 52 */
    public int size() {
        return size;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("器官槽位下标越界: " + index + " (size=" + size + ")");
        }
    }

    /**  null {@link ItemStack#EMPTY} */
    public ItemStack getItem(int index) {
        checkIndex(index);
        return items[index];
    }

    /** {@code null}  */
    public void setItem(int index, ItemStack stack) {
        checkIndex(index);
        items[index] = (stack == null) ? ItemStack.EMPTY : stack;
    }

    /**  */
    public void clearItem(int index) {
        setItem(index, ItemStack.EMPTY);
    }

    /**  */
    public boolean isUnlocked(int index) {
        checkIndex(index);
        return unlocked[index];
    }

    /**  */
    public boolean setUnlocked(int index, boolean value) {
        checkIndex(index);
        if (unlocked[index] == value) return false;
        unlocked[index] = value;
        return true;
    }

    /**  */
    public OrganSlotGroup groupOf(int index) {
        checkIndex(index);
        return OrganSlotGroup.byGlobalIndex(index);
    }

    /**  {@code epca:organ_part} /  */
    public static boolean acceptsItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(ModTags.ORGAN_PART);
    }

    /** {@link #acceptsItem(ItemStack)}  */
    public static boolean isOrganPart(ItemStack stack) {
        return acceptsItem(stack);
    }

    /**
     *
     *
     * <p></p>
     */
    public List<Integer> lockedPurpleSlots() {
        List<Integer> locked = new ArrayList<>();
        for (int index : OrganSlotGroup.purpleSlotIndices()) {
            if (!unlocked[index]) locked.add(index);
        }
        return Collections.unmodifiableList(locked);
    }

    /**  */
    public int unlockedPurpleCount() {
        int count = 0;
        for (int index : OrganSlotGroup.purpleSlotIndices()) {
            if (unlocked[index]) count++;
        }
        return count;
    }


    /**  {@code tag} */
    public void save(CompoundTag tag) {
        ListTag itemList = new ListTag();
        for (int i = 0; i < size; i++) {
            //  ItemStack.EMPTY
            // 26.1.2: ItemStack#save(CompoundTag) was deleted - use ItemStack.CODEC + NbtOps.
            itemList.add(encodeStack(items[i]));
        }
        tag.put(TAG_ITEMS, itemList);

        byte[] unlockedBytes = new byte[size];
        for (int i = 0; i < size; i++) {
            unlockedBytes[i] = (byte) (unlocked[i] ? 1 : 0);
        }
        tag.putByteArray(TAG_UNLOCKED, unlockedBytes);

        //  = false =
        if (headInnerDefaultsApplied) {
            tag.putBoolean(TAG_HEAD_INNER_DEFAULTS, true);
        }
        //  TAG_TORSO_INNER_DEFAULTS
        //  Items / Unlocked
        //  SyncNestLeaderOrgansPacket
        if (torsoInnerDefaultsApplied) {
            tag.putBoolean(TAG_TORSO_INNER_DEFAULTS, true);
        }
    }

    /**
     *  {@code tag}
     *
     * <p>
     * {@link #fromNbtOrDefaults(CompoundTag)}  {@link #createDefault()}
     *  {@link #fromNbt(CompoundTag)}
     *  =  + </p>
     */
    public void load(CompoundTag tag) {
        ListTag itemList = tag.getListOrEmpty(TAG_ITEMS);
        for (int i = 0; i < size; i++) {
            if (i < itemList.size()) {
                // 26.1.2: ItemStack#of(CompoundTag) was deleted; ListTag#getCompound(int) now
                // returns Optional<CompoundTag>, so use getCompoundOrEmpty(int) and parse it.
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

        //  false
        // fromNbtOrDefaults()  createDefault() true load
        //  false
        if (tag.contains(TAG_HEAD_INNER_DEFAULTS)) {
            headInnerDefaultsApplied = tag.getBooleanOr(TAG_HEAD_INNER_DEFAULTS, false);
        }
        if (tag.contains(TAG_TORSO_INNER_DEFAULTS)) {
            torsoInnerDefaultsApplied = tag.getBooleanOr(TAG_TORSO_INNER_DEFAULTS, false);
        }
    }

    /**
     *  {@link ItemStack}  NBT {@link #save(CompoundTag)}
     *
     * <p>26.1.2: {@code ItemStack#save(CompoundTag)} / {@code ItemStack#of(CompoundTag)} are gone;
     * the only remaining NBT round-trip for an item stack is {@link ItemStack#CODEC} together with
     * {@link NbtOps#INSTANCE}.  An empty stack is written as an empty compound tag, which is what
     * keeps "one list element per slot index" from drifting.</p>
     *
     * <p>Identical to the already-converted sibling {@code NestLeaderOrganCarry#describe} in this
     * same tree: a failed encode (null or a non-compound tag) degrades to an empty compound tag,
     * which decodes back to {@link ItemStack#EMPTY}.</p>
     */
    private static CompoundTag encodeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        Tag encoded = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElse(null);
        return encoded instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    /** {@code createEmpty().load(tag)} +  */
    public static NestLeaderOrganData fromNbt(CompoundTag tag) {
        NestLeaderOrganData data = createEmpty();
        data.load(tag);
        return data;
    }

    /**
     *  {@link #createDefault()}
     *
     * <p>
     *  {@code NestLeaderOrgans}
     *  {@code NestLeaderOrganSavedData#readOrCreate}
     *  {@link #fromNbt(CompoundTag)}  {@link #createDefault()}</p>
     */
    public static NestLeaderOrganData fromNbtOrDefaults(CompoundTag tag) {
        NestLeaderOrganData data = createDefault();
        data.load(tag);
        return data;
    }

    /** / */
    @Override
    public String toString() {
        return "NestLeaderOrganData[size=" + size
                + ", purpleUnlocked=" + unlockedPurpleCount()
                + "/" + OrganSlotGroup.purpleSlotIndices().length + "]";
    }
}

