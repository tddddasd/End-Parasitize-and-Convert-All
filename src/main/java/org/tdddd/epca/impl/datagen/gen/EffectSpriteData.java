package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SpriteSourceProvider;
import org.tdddd.epca.impl.epca;

import java.util.Optional;

/**
 * 数据生成器：自动为模组状态效果（MobEffect）生成精灵图源（sprite source）。
 */
public class EffectSpriteData extends SpriteSourceProvider {

    public EffectSpriteData(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, fileHelper, epca.MODID);
    }

    @Override
    protected void addSources() {
        // 效果精灵图集
        ResourceLocation effectsAtlas = new ResourceLocation("textures/atlas/mob_effects.png");

        // 所有模组状态效果的纹理
        String[] effectTextures = {
                "bleeding", "viral", "fear", "coth",
                "contempt_inorganic", "corrosive", "rage", "needler",
                "deep_sneak", "solidify", "ender_erosion",
                "camouflage", "spirit", "soul_protection"
        };

        for (String name : effectTextures) {
            ResourceLocation textureLoc = new ResourceLocation(epca.MODID, "mob_effect/" + name);
            atlas(effectsAtlas).addSource(new SingleFile(textureLoc, Optional.empty()));
        }
    }
}
