package org.tdddd.epca.impl.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.tdddd.epca.impl.datagen.gen.*;
import org.tdddd.epca.impl.datagen.gen.lang.LangDataCN;
import org.tdddd.epca.impl.datagen.gen.lang.LangDataEN;


public final class DataGenEvent {

    private DataGenEvent() {
    }

    
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
