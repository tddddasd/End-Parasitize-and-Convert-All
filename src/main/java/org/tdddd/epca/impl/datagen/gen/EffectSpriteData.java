package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.AtlasIds;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.data.SpriteSourceProvider;
import org.tdddd.epca.impl.epca;

import java.util.concurrent.CompletableFuture;

/**
 * 数据生成器：自动为模组状态效果（MobEffect）生成精灵图源（sprite source）。
 *
 * <p><b>26.1.2 改动</b>
 * <ul>
 *   <li>{@code SpriteSourceProvider} 的构造器由
 *       {@code (PackOutput, ExistingFileHelper, String)} 变为
 *       {@code (PackOutput, CompletableFuture<HolderLookup.Provider>, String)}
 *       —— {@code ExistingFileHelper} 已被平台删除。</li>
 *   <li>移除了 1.20.1 的 atlas id {@code textures/atlas/mob_effects.png}：
 *       26.1.2 的 atlas 配置是 {@code assets/<ns>/atlases/<AtlasIds.id>.json}，
 *       且<b>没有</b> mob_effects 图集。状态效果图标已经由 {@code assets/minecraft/atlases/gui.json}
 *       里的 {@code {"type":"minecraft:directory","prefix":"mob_effect/",...}} 收入 {@code gui} 图集，
 *       因此这里直接往 {@link AtlasIds#GUI} 追加单个文件源（同图集多来源会合并）。</li>
 * </ul>
 */
public class EffectSpriteData extends SpriteSourceProvider {

    public EffectSpriteData(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, epca.MODID);
    }

    @Override
    protected void gather() {
        // 效果精灵图集：26.1.2 用 AtlasIds（短名），输出 assets/epca/atlases/gui.json
        Identifier effectsAtlas = AtlasIds.GUI;

        // 所有模组状态效果的纹理
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
