package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.AtlasIds;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.data.SpriteSourceProvider;
import org.tdddd.epca.impl.epca;

import java.util.concurrent.CompletableFuture;


public class EffectSpriteData extends SpriteSourceProvider {

    public EffectSpriteData(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, epca.MODID);
    }

    @Override
    protected void gather() {
        
        Identifier effectsAtlas = AtlasIds.GUI;

        
        String[] effectTextures = {
                "bleeding", "viral", "fear", "coth",
                "contempt_inorganic", "corrosive", "rage", "needler",
                "deep_sneak", "solidify", "ender_erosion",
                "camouflage", "spirit", "soul_protection"
        };

        for (String name : effectTextures) {
            Identifier textureLoc = Identifier.fromNamespaceAndPath(epca.MODID, "mob_effect/" + name);
            atlas(effectsAtlas).addSource(new SingleFile(textureLoc));
        }
    }
}
