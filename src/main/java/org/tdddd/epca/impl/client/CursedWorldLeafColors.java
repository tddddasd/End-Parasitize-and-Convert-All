package org.tdddd.epca.impl.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.tdddd.epca.impl.epca;

import java.util.List;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class CursedWorldLeafColors {

    private CursedWorldLeafColors() {}

    @SubscribeEvent
    public static void registerBlockTintSources(RegisterColorHandlersEvent.BlockTintSources event) {

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

    private static Block leaf(String path) {
        Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getValue(Identifier.withDefaultNamespace(path));
        return block == null ? net.minecraft.world.level.block.Blocks.AIR : block;
    }
}
