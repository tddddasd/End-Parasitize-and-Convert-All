package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModTags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 虫咒笔记的「已发现 / 详细数据已解锁」持久化（按玩家、按存档）。
 *
 * <p>存储位置：{@code Player#getPersistentData()}（NeoForge 的 {@code ForgeData} 复合标签），
 * 它随玩家数据一起写进存档的 playerdata，所以：</p>
 * <ul>
 *   <li>换维度不丢（玩家数据跟着玩家走）；</li>
 *   <li>退出重进不丢；</li>
 *   <li>不同存档互不影响（就在这份存档的玩家数据里）。</li>
 * </ul>
 *
 * <p>两个键：</p>
 * <ul>
 *   <li>{@link #KEY_UNLOCKED} = 已发现（条目名 + 正文可见）；</li>
 *   <li>{@link #KEY_DETAIL} = 详细数据已解锁（消耗材料后）。</li>
 * </ul>
 *
 * <p>值都是字符串列表（实体 id），顺序 = 解锁顺序，读写时保持稳定。</p>
 *
 * <p><b>体型归一化</b>：笔记里只列主条目（{@code infested_slime_size0}、{@code living_flesh_size0}），
 * 但玩家实际遇到的可能是 size1 / size3。{@link #normalize(Identifier)} 会把
 * {@code <base>_sizeN} 折成 {@code <base>_size0}（仅在该主条目确实注册过时），
 * 保证“遇见大史莱姆”也能点亮“虫染史莱姆”那一条。</p>
 *
 * <p><b>26.1.2 移植说明</b>（相对 1.20.1）：</p>
 * <ul>
 *   <li>{@code ResourceLocation} -&gt; {@code Identifier}（{@code fromNamespaceAndPath / tryParse}）。</li>
 *   <li>{@code ForgeRegistries.ENTITY_TYPES.containsKey} -&gt; {@code BuiltInRegistries.ENTITY_TYPE.containsKey}。</li>
 *   <li>1.21+ 把 NBT 读取改成 Optional 风格：{@code data.getList(key, Tag.TAG_STRING)} 换成
 *       {@code data.getListOrEmpty(key)}，{@code ListTag#getString(i)} 换成 {@code getStringOr(i, "")}。</li>
 *   <li>{@code EntityType#is(TagKey)} 已移除，改用
 *       {@code BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(ModTags.PARASITE)}。</li>
 * </ul>
 */
public final class EPCANoteUnlocks {

    public static final String KEY_UNLOCKED = "epca_note_unlocked";
    public static final String KEY_DETAIL = "epca_note_detail_unlocked";

    private static final Pattern SIZE_SUFFIX = Pattern.compile("^(.*_size)(\\d+)$");

    private EPCANoteUnlocks() {
    }

    // ======================= 归一化 =======================

    /** 把体型变体折到主条目（{@code xxx_size3} -&gt; {@code xxx_size0}）；其它 id 原样返回 */
    public static Identifier normalize(Identifier id) {
        if (id == null) {
            return null;
        }
        Matcher matcher = SIZE_SUFFIX.matcher(id.getPath());
        if (!matcher.matches()) {
            return id;
        }
        Identifier canonical = Identifier.fromNamespaceAndPath(id.getNamespace(), matcher.group(1) + "0");
        try {
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(canonical)) {
                return canonical;
            }
        } catch (Throwable ignored) {
        }
        return id;
    }

    // ======================= 读取 =======================

    public static Set<Identifier> unlocked(Player player) {
        return read(player, KEY_UNLOCKED);
    }

    public static Set<Identifier> detailUnlocked(Player player) {
        return read(player, KEY_DETAIL);
    }

    public static boolean isUnlocked(Player player, Identifier entityId) {
        if (player == null || entityId == null) {
            return false;
        }
        return unlocked(player).contains(normalize(entityId));
    }

    public static boolean isDetailUnlocked(Player player, Identifier entityId) {
        if (player == null || entityId == null) {
            return false;
        }
        return detailUnlocked(player).contains(normalize(entityId));
    }

    // ======================= 写入 =======================

    /**
     * 标记“已发现”。
     *
     * @return 状态是否发生变化（调用方据此决定要不要发同步包）
     */
    public static boolean unlock(Player player, Identifier entityId) {
        return put(player, KEY_UNLOCKED, normalize(entityId));
    }

    /** 标记“详细数据已解锁”，调用方必须已经扣过材料并校验过 */
    public static boolean unlockDetail(Player player, Identifier entityId) {
        return put(player, KEY_DETAIL, normalize(entityId));
    }

    /** 调试 / 管理用：清空该玩家的两项记录 */
    public static void clear(Player player) {
        if (player == null) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        data.remove(KEY_UNLOCKED);
        data.remove(KEY_DETAIL);
    }

    // ======================= 内部 =======================

    private static boolean put(Player player, String key, Identifier entityId) {
        if (player == null || entityId == null) {
            return false;
        }
        Set<Identifier> current = read(player, key);
        if (!current.add(entityId)) {
            return false;
        }
        write(player, key, current);
        return true;
    }

    private static Set<Identifier> read(Player player, String key) {
        Set<Identifier> out = new LinkedHashSet<>();
        if (player == null) {
            return out;
        }
        try {
            CompoundTag data = player.getPersistentData();
            ListTag list = data.getListOrEmpty(key);
            for (int i = 0; i < list.size(); i++) {
                Identifier id = Identifier.tryParse(list.getStringOr(i, ""));
                if (id != null) {
                    out.add(id);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static void write(Player player, String key, Collection<Identifier> ids) {
        ListTag list = new ListTag();
        for (Identifier id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        player.getPersistentData().put(key, list);
    }

    /** 给网络包用：转成稳定的字符串列表 */
    public static List<String> toStringList(Collection<Identifier> ids) {
        List<String> out = new ArrayList<>(ids.size());
        for (Identifier id : ids) {
            out.add(id.toString());
        }
        return out;
    }

    /** 给网络包用：解析字符串列表（坏数据直接丢弃，不抛异常） */
    public static Set<Identifier> fromStringList(Collection<String> ids) {
        Set<Identifier> out = new LinkedHashSet<>();
        if (ids == null) {
            return out;
        }
        for (String raw : ids) {
            Identifier id = Identifier.tryParse(raw);
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    /** 该实体类型是否属于本模组的寄生体（用于 8 格扫描过滤） */
    public static boolean isNoteSpecies(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        try {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null || !epca.MODID.equals(id.getNamespace())) {
                return false;
            }
            return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(ModTags.PARASITE);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
