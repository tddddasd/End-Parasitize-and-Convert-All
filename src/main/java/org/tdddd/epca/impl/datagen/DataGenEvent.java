package org.tdddd.epca.impl.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.tdddd.epca.impl.datagen.gen.*;
import org.tdddd.epca.impl.datagen.gen.lang.LangDataCN;
import org.tdddd.epca.impl.datagen.gen.lang.LangDataEN;

/**
 * epca 的数据生成入口。
 *
 * <h2>26.1.2 适配</h2>
 * <ul>
 *   <li>原来的 {@code @EventBusSubscriber()} + {@code @SubscribeEvent} 不再适用：
 *       {@link GatherDataEvent} 是<b>模组总线</b>事件，而 26.1.2 的 {@code @EventBusSubscriber}
 *       固定挂游戏总线。改为在 {@code epca} 的构造器里
 *       {@code modEventBus.addListener(DataGenEvent::gatherClientData)} /
 *       {@code ...::gatherServerData}。</li>
 *   <li>{@link GatherDataEvent} 变成抽象类并拆成 {@link GatherDataEvent.Client} 与
 *       {@link GatherDataEvent.Server}，因此需要两个监听方法；
 *       {@code includeClient()/includeServer()} 已删除，{@code addProvider(boolean, provider)}
 *       的第一个参数就是原来的开关。</li>
 *   <li>{@code ExistingFileHelper} 已删除，所有 Provider 都不再接收它。</li>
 *   <li>部分 Provider 需要 {@code CompletableFuture<HolderLookup.Provider>}
 *       （{@code event.getLookupProvider()}），作为注册表解析上下文。</li>
 * </ul>
 */
public final class DataGenEvent {

    private DataGenEvent() {
    }

    /** 客户端资源（assets：blockstates / models / items 定义 / lang / sounds / particles / 效果图集）。 */
    public static void gatherClientData(GatherDataEvent.Client event) {
        PackOutput out = event.getGenerator().getPackOutput();
        var lp = event.getLookupProvider();

        event.getGenerator().addProvider(true, new BlockStateData(out));
        event.getGenerator().addProvider(true, new ItemGenData(out));
        event.getGenerator().addProvider(true, new LangDataCN(out, "zh_cn"));
        event.getGenerator().addProvider(true, new LangDataEN(out, "en_us"));
        event.getGenerator().addProvider(true, new SoundData(out));
        event.getGenerator().addProvider(true, new ParticleData(out));
        event.getGenerator().addProvider(true, new EffectSpriteData(out, lp));
    }

    /** 服务端数据（data：tags / recipes / 自定义数据 / 世界生成）。 */
    public static void gatherServerData(GatherDataEvent.Server event) {
        PackOutput out = event.getGenerator().getPackOutput();
        var lp = event.getLookupProvider();

        BlockTagData blockTags = new BlockTagData(out, lp);
        event.getGenerator().addProvider(true, blockTags);
        event.getGenerator().addProvider(true,
                new ItemAndEntityTagsData.ItemTagsGen(out, lp));
        event.getGenerator().addProvider(true,
                new ItemAndEntityTagsData.EntityTagsGen(out, lp));
        event.getGenerator().addProvider(true, new RecipeProviderData.Runner(out, lp));
        event.getGenerator().addProvider(true, new HallWorldGenData(out, lp));

        // ── 自定义数据 ──
        event.getGenerator().addProvider(true,
                new CustomDataProviders.EntityConversionDataProvider(out));
        event.getGenerator().addProvider(true,
                new CustomDataProviders.EntityCarryDataProvider(out));
        event.getGenerator().addProvider(true,
                new CustomDataProviders.BlockConversionDataProvider(out));
        event.getGenerator().addProvider(true,
                new CustomDataProviders.BiomassSpawnDataProvider(out));
    }
}
