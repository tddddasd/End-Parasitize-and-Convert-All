package org.tdddd.epca.impl.overworld.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import org.tdddd.epca.impl.epca;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.LongStream;

/**
 * 献祭仪式中「尚未跑完的方块转换任务」的持久化存储（I19a）。
 *
 * <h2>为什么需要它</h2>
 * 转换队列原先只存在于 {@code BlockConversionManager.sacrificeTasks} 这个内存 {@code HashMap} 里。
 * 服务器重启 / 崩服会把队列清空，于是闪电、COTH 效果与 {@code NestLeaderManager.addNestLeader}
 * 永远不会发生，而区块已经被改造了一半 —— 玩家既没有提示也没有补偿。
 * 本类把「还差多少」写进存档，由 {@link org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager}
 * 在服务端启动时（{@code ServerStartedEvent}）重新入队。
 *
 * <h2>26.1.2 的写法</h2>
 * 26.1.2 的 {@code SavedData} 改为 codec 驱动：{@link SavedDataType#SavedDataType(Identifier, java.util.function.Supplier, Codec)}
 * + {@code SavedDataStorage#computeIfAbsent(SavedDataType)}，落盘格式是 {@code {"data": &lt;codec 编码结果&gt;}}。
 * 本类照抄同一工作区里已经移植好的 {@link EvolutionDataStorage} / {@link NestLeaderSavedData} 的形状。
 *
 * <h2>存档兼容</h2>
 * 文件是新建的（{@code data/epca/sacrifice_ritual.dat}），旧存档里不存在该文件，
 * {@code SavedDataStorage#readSavedData} 直接返回 {@code null}，随后走空构造函数 —— 等价于
 * 「没有进行中的仪式」，不会破坏已有存档。所有字段都是 {@code optionalFieldOf}，
 * 并且解码失败的条目只会被丢弃 + 记一条 warn，不会让 {@code parse} 整体失败。
 */
public class SacrificeSavedData extends SavedData {

    /** 与 epca 其它 SavedData 一致：Identifier 用小写路径。 */
    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "sacrifice_ritual");

    /** 单个轴允许的偏移位数。半径 72 &lt; 2^20，21 位足够且留有余量。 */
    private static final int AXIS_BITS = 21;
    private static final long AXIS_MASK = (1L << AXIS_BITS) - 1L;
    /** 偏移的合法范围（含）。 */
    private static final int AXIS_LIMIT = (1 << (AXIS_BITS - 1)) - 1;

    private static final Codec<BlockPos> BLOCK_POS_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("x").forGetter(BlockPos::getX),
            Codec.INT.fieldOf("y").forGetter(BlockPos::getY),
            Codec.INT.fieldOf("z").forGetter(BlockPos::getZ)
    ).apply(i, BlockPos::new));

    /** long[] &lt;-&gt; LongStream，便于用 {@link Codec#LONG_STREAM}（NBT long 数组）落盘。 */
    private static final Codec<long[]> POSITIONS_CODEC = Codec.LONG_STREAM
            .comapFlatMap(stream -> DataResult.success(stream.toArray()), LongStream::of);

    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(Registries.DIMENSION);

    /**
     * 一条进行中的转换任务：维度 + 仪式中心 + 发起者 + 快照坐标 + 已经处理到第几个。
     *
     * @param positions 相对 {@code center} 的偏移（{@link #encodeOffset}），顺序与运行时一致（按距离升序）
     * @param nextIndex 已经处理到的下标；恢复后从它继续，因此不会把已经转换过的方块再走一遍
     */
    public record SacrificeEntry(ResourceKey<Level> dimension, BlockPos center, UUID playerId,
                                 long[] positions, int nextIndex) {}

    /**
     * 磁盘上的条目形状。全部字段可空，这样「空 compound」也能解码成功（只是产出 0 条任务），
     * 以符合「文件缺失/内容不完整 = 空状态」的存档兼容要求。
     */
    private record StoredEntry(Optional<ResourceKey<Level>> dimension, Optional<BlockPos> center,
                               Optional<String> playerId, long[] positions, int nextIndex) {
        private static final Codec<StoredEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                DIMENSION_CODEC.optionalFieldOf("Dimension").forGetter(StoredEntry::dimension),
                BLOCK_POS_CODEC.optionalFieldOf("Center").forGetter(StoredEntry::center),
                Codec.STRING.optionalFieldOf("Player").forGetter(StoredEntry::playerId),
                POSITIONS_CODEC.optionalFieldOf("Positions", new long[0]).forGetter(StoredEntry::positions),
                Codec.INT.optionalFieldOf("NextIndex", 0).forGetter(StoredEntry::nextIndex)
        ).apply(i, StoredEntry::new));
    }

    /**
     * 26.1.2 通过 codec 持久化。用「列表」而不是「维度 -&gt; 任务」的 map：{@code Codec.unboundedMap}
     * 在 NBT 下要求键可序列化为字符串，而 {@link EvolutionDataStorage} 已经验证过
     * 「列表 + 记录里带维度字段」这种形状是可靠的。
     */
    public static final Codec<SacrificeSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            StoredEntry.CODEC.listOf().optionalFieldOf("Tasks", List.of())
                    .flatXmap(SacrificeSavedData::decodeTasks, SacrificeSavedData::encodeTasks)
                    .forGetter(SacrificeSavedData::taskList)
    ).apply(i, SacrificeSavedData::new));

    public static final SavedDataType<SacrificeSavedData> TYPE =
            new SavedDataType<SacrificeSavedData>(DATA_ID, SacrificeSavedData::new, CODEC);

    private final List<SacrificeEntry> tasks = new ArrayList<>();

    public SacrificeSavedData() {}

    /** 反序列化构造函数；不填充任何默认值，与 {@link NestLeaderSavedData} 的做法一致。 */
    private SacrificeSavedData(List<SacrificeEntry> entries) {
        tasks.addAll(entries);
    }

    /**
     * 读取某个维度对应的那份 SavedData。SavedData 是「每个 ServerLevel 一份」，
     * 恢复流程会遍历所有维度再汇总。
     */
    public static SacrificeSavedData get(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<SacrificeEntry> taskList() {
        return new ArrayList<>(tasks);
    }

    /** @return 当前记录的全部任务（副本）。 */
    public List<SacrificeEntry> getTasks() {
        return new ArrayList<>(tasks);
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    public void clear() {
        tasks.clear();
        setDirty();
    }

    /**
     * 整体替换该维度的任务集合。{@code BlockConversionManager} 每完成一次推进都会把
     * 「该维度当前还在跑的任务」整体写回，因此这里先删掉旧维度条目再写入新的。
     */
    public void setTasksForDimension(ResourceKey<Level> dimension, List<SacrificeEntry> entries) {
        tasks.removeIf(entry -> entry.dimension().equals(dimension));
        tasks.addAll(entries);
        setDirty();
    }

    /** 推进某个任务已处理到的下标（不改变坐标快照）。 */
    public void updateProgress(ResourceKey<Level> dimension, BlockPos center, int nextIndex) {
        for (int i = 0; i < tasks.size(); i++) {
            SacrificeEntry entry = tasks.get(i);
            if (entry.dimension().equals(dimension) && entry.center().equals(center)) {
                tasks.set(i, new SacrificeEntry(entry.dimension(), entry.center(), entry.playerId(),
                        entry.positions(), Math.max(0, nextIndex)));
                setDirty();
                return;
            }
        }
    }

    /** 移除某个维度里指定中心的全部任务（取消仪式时使用）。 */
    public boolean removeTasksAt(ResourceKey<Level> dimension, BlockPos center) {
        boolean removed = tasks.removeIf(entry ->
                entry.dimension().equals(dimension) && entry.center().equals(center));
        if (removed) setDirty();
        return removed;
    }

    /** 移除某个维度里由指定玩家发起的全部任务（玩家退出/死亡时取消）。 */
    public boolean removeTasksForPlayer(ResourceKey<Level> dimension, UUID playerId) {
        boolean removed = tasks.removeIf(entry ->
                entry.dimension().equals(dimension) && entry.playerId().equals(playerId));
        if (removed) setDirty();
        return removed;
    }

    // ═══════════════════ 坐标编码 ═══════════════════

    /** 把「相对中心的偏移」压进一个 long（每轴 21 位、有符号）。 */
    public static long encodeOffset(int dx, int dy, int dz) {
        return ((long) dx & AXIS_MASK) | (((long) dy & AXIS_MASK) << AXIS_BITS)
                | (((long) dz & AXIS_MASK) << (AXIS_BITS * 2));
    }

    private static int decodeSigned(long packed, int shift) {
        int value = (int) ((packed >>> shift) & AXIS_MASK);
        // 符号扩展：21 位有符号
        return (value << (32 - AXIS_BITS)) >> (32 - AXIS_BITS);
    }

    /** 把 long[] 还原成绝对位置数组。 */
    public static BlockPos[] decodePositions(long[] packed, BlockPos center) {
        BlockPos[] result = new BlockPos[packed.length];
        for (int i = 0; i < packed.length; i++) {
            int dx = decodeSigned(packed[i], 0);
            int dy = decodeSigned(packed[i], AXIS_BITS);
            int dz = decodeSigned(packed[i], AXIS_BITS * 2);
            result[i] = center.offset(dx, dy, dz);
        }
        return result;
    }

    /** 偏移是否落在可编码范围内。 */
    public static boolean isEncodableOffset(int dx, int dy, int dz) {
        return Math.abs(dx) <= AXIS_LIMIT && Math.abs(dy) <= AXIS_LIMIT && Math.abs(dz) <= AXIS_LIMIT;
    }

    // ═══════════════════ 编解码辅助 ═══════════════════

    private static DataResult<List<SacrificeEntry>> decodeTasks(List<StoredEntry> stored) {
        List<SacrificeEntry> result = new ArrayList<>(stored.size());
        for (StoredEntry entry : stored) {
            if (entry.dimension().isEmpty() || entry.center().isEmpty() || entry.playerId().isEmpty()) {
                epca.LOGGER.warn("Dropping incomplete sacrifice task entry in SacrificeSavedData");
                continue;
            }
            UUID playerId = parseUuid(entry.playerId().get());
            if (playerId == null) continue;
            result.add(new SacrificeEntry(entry.dimension().get(), entry.center().get(), playerId,
                    entry.positions(), Math.max(0, entry.nextIndex())));
        }
        return DataResult.success(result);
    }

    /** 写盘方向：把运行中的条目原样转成磁盘形状（字段全为 present）。 */
    private static DataResult<List<StoredEntry>> encodeTasks(List<SacrificeEntry> entries) {
        List<StoredEntry> result = new ArrayList<>(entries.size());
        for (SacrificeEntry entry : entries) {
            result.add(new StoredEntry(Optional.of(entry.dimension()),
                    Optional.of(entry.center()),
                    Optional.of(entry.playerId().toString()),
                    entry.positions(), entry.nextIndex()));
        }
        return DataResult.success(result);
    }

    @Nullable
    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            // 与 NestLeaderSavedData 一致：坏数据只丢弃这一条，不炸掉整个存储。
            epca.LOGGER.warn("Invalid UUID in SacrificeSavedData: {}", value);
            return null;
        }
    }
}
