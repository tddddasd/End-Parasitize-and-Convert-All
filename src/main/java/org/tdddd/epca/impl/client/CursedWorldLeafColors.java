package org.tdddd.epca.impl.client;

import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.tdddd.epca.impl.epca;

import java.util.List;

/**
 * Makes the vanilla leaves that ignore the biome foliage colour render with the biome's foliage colour.
 *
 * <p>In 26.1.2 block tinting runs through {@code BlockTintSource}s instead of the old {@code BlockColor}
 * getters, so there is no {@code BlockColorsMixin} in this port to extend. The equivalent hook is the
 * NeoForge {@link RegisterColorHandlersEvent.BlockTintSources} mod-bus event, used here to re-register the
 * leaves whose vanilla entries are constant colours rather than {@code BlockTintSources.foliage()}:
 * spruce ({@code -10380959}), birch ({@code -8345771}) and the unregistered cherry, pale oak, azalea and
 * flowering azalea leaves.
 *
 * <p>Only leaf blocks are touched; every other block keeps its vanilla tint source. In a cursed world the
 * value becomes the parasite biome's foliage colour (purple, {@code #8e44ad} from
 * {@code data/epca/worldgen/biome/parasite_biome.json}); in every other world these leaves simply render
 * exactly like oak leaves always did, because the resolver is the standard vanilla foliage lookup.
 */
@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class CursedWorldLeafColors {

    private CursedWorldLeafColors() {}

    @SubscribeEvent
    public static void registerBlockTintSources(RegisterColorHandlersEvent.BlockTintSources event) {
        // Same source vanilla uses for oak/jungle/acacia/dark oak/mangrove leaves and vines.
        event.register(
                List.of(BlockTintSources.foliage()),
                leaf("spruce_leaves"),
                leaf("birch_leaves"),
                leaf("cherry_leaves"),
                leaf("pale_oak_leaves"),
                leaf("azalea_leaves"),
                leaf("flowering_azalea_leaves")
        );
    }

    /** Looks a vanilla block up by name; an unknown name (removed in a future version) resolves to air. */
    private static Block leaf(String path) {
        Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getValue(Identifier.withDefaultNamespace(path));
        return block == null ? net.minecraft.world.level.block.Blocks.AIR : block;
    }
}
