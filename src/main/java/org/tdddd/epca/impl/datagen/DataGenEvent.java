package org.tdddd.epca.impl.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.datagen.gen.*;
import org.tdddd.epca.impl.datagen.gen.lang.LangDataCN;
import org.tdddd.epca.impl.datagen.gen.lang.LangDataEN;


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenEvent {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var efh = event.getExistingFileHelper();
        PackOutput out = event.getGenerator().getPackOutput();
        var lp = event.getLookupProvider();

        // ── 客户端 ──
        var blockState = new BlockStateData(out, efh);
        event.getGenerator().addProvider(event.includeClient(), blockState);
        event.getGenerator().addProvider(event.includeClient(), new ItemGenData(out, efh));
        event.getGenerator().addProvider(event.includeClient(), new LangDataCN(out, "zh_cn"));
        event.getGenerator().addProvider(event.includeClient(), new LangDataEN(out, "en_us"));
        event.getGenerator().addProvider(event.includeClient(), new SoundData(out, efh));
        event.getGenerator().addProvider(event.includeClient(), new ParticleData(out, efh));
        event.getGenerator().addProvider(event.includeClient(), new EffectSpriteData(out, efh));

        // ── 服务端 ──
        var blockTags = new BlockTagData(out, lp, efh);
        event.getGenerator().addProvider(event.includeServer(), blockTags);
        event.getGenerator().addProvider(event.includeServer(),
                new ItemAndEntityTagsData.ItemTagsGen(out, lp, blockTags, efh));
        event.getGenerator().addProvider(event.includeServer(),
                new ItemAndEntityTagsData.EntityTagsGen(out, lp, efh));
        event.getGenerator().addProvider(event.includeServer(), new RecipeProviderData(out));
        event.getGenerator().addProvider(event.includeServer(), new HallWorldGenData(out, lp));

        // ── 自定义数据 ──
        event.getGenerator().addProvider(event.includeServer(),
                new CustomDataProviders.EntityConversionDataProvider(out));
        event.getGenerator().addProvider(event.includeServer(),
                new CustomDataProviders.EntityCarryDataProvider(out));
        event.getGenerator().addProvider(event.includeServer(),
                new CustomDataProviders.BlockConversionDataProvider(out));
        event.getGenerator().addProvider(event.includeServer(),
                new CustomDataProviders.BiomassSpawnDataProvider(out));
        event.getGenerator().addProvider(event.includeServer(),
                new CustomDataProviders.AltarPointDataProvider(out));
    }
}
