package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import org.tdddd.epca.impl.epca;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Hall 世界生成数据提供器。
 * <p>
 * 在 datagen 阶段将自定义群系（Hall Wasteland）注册到数据包中，
 * 使其可以在游戏中被引用。
 */
public class HallWorldGenData extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.BIOME, HallBiomeData::bootstrap);

    public HallWorldGenData(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(epca.MODID));
    }
}
