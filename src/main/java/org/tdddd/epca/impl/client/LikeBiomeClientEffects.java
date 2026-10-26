package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.LikeBiomeClientCache;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Applies the colour part of a like biome on the client.
 *
 * <p>Fog is handled here: it is a per frame camera colour, so it can be replaced without touching any
 * block or chunk data, which is exactly what "layer a biome on top" means. Water and grass colours go
 * through the biome colour hooks, and the sky colour still needs a {@code DimensionSpecialEffects}
 * hook because 1.20.1 has no viewport event for it.</p>
 *
 * <p>Nothing happens when the layer has {@code overrideColors} off or when the biome id no longer exists
 * in the client registry.</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LikeBiomeClientEffects {

    private LikeBiomeClientEffects() {
    }

    /** The like biome in force at the camera position, or {@code null}. */
    @Nullable
    public static Biome activeBiome(BlockPos pos) {
        ResourceLocation id = LikeBiomeClientCache.biomeId(pos);
        if (id == null || !LikeBiomeClientCache.overridesColors(pos)) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
        Optional<Biome> biome = minecraft.level.registryAccess()
                .registryOrThrow(Registries.BIOME).getOptional(key);
        return biome.orElse(null);
    }

    @Nullable
    private static Biome activeAtCamera(ViewportEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        BlockPos pos = BlockPos.containing(event.getCamera().getPosition());
        return activeBiome(pos);
    }

    /** Fog colour follows the like biome. */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Biome biome = activeAtCamera(event);
        if (biome == null) {
            return;
        }
        int color = biome.getFogColor();
        event.setRed(((color >> 16) & 0xFF) / 255.0F);
        event.setGreen(((color >> 8) & 0xFF) / 255.0F);
        event.setBlue((color & 0xFF) / 255.0F);
    }

}
