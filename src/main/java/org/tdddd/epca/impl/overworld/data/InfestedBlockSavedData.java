package org.tdddd.epca.impl.overworld.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InfestedBlockSavedData extends SavedData {
    /** 1.20.1 stored this as "epca_infested_blocks"; it stays a valid Identifier path. */
    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "epca_infested_blocks");

    /** Same X/Y/Z compound shape that the removed NbtUtils.writeBlockPos/readBlockPos used in 1.20.1. */
    private static final Codec<BlockPos> BLOCK_POS_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("X").forGetter(BlockPos::getX),
            Codec.INT.fieldOf("Y").forGetter(BlockPos::getY),
            Codec.INT.fieldOf("Z").forGetter(BlockPos::getZ)
    ).apply(i, BlockPos::new));

    /** Legacy field name "blocks" kept, so an old data compound can be moved under {"data": ...} unchanged. */
    public static final Codec<InfestedBlockSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            BLOCK_POS_CODEC.listOf().optionalFieldOf("blocks", List.of())
                    .forGetter(InfestedBlockSavedData::blockList)
    ).apply(i, InfestedBlockSavedData::new));

    public static final SavedDataType<InfestedBlockSavedData> TYPE =
            new SavedDataType<InfestedBlockSavedData>(DATA_ID, InfestedBlockSavedData::new, CODEC);

    private final Set<BlockPos> infestedBlocks = new HashSet<>();

    public InfestedBlockSavedData() {}

    /** Codec decode constructor. */
    private InfestedBlockSavedData(List<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            infestedBlocks.add(pos.immutable());
        }
    }

    private List<BlockPos> blockList() {
        return new ArrayList<>(infestedBlocks);
    }

    public static InfestedBlockSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void add(BlockPos pos) {
        if (infestedBlocks.add(pos.immutable())) {
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (infestedBlocks.remove(pos.immutable())) {
            setDirty();
        }
    }

    public Set<BlockPos> getAll() {
        return new HashSet<>(infestedBlocks);
    }

    public boolean contains(BlockPos pos) {
        return infestedBlocks.contains(pos);
    }
}
