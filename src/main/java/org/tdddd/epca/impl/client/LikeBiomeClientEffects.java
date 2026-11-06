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

@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LikeBiomeClientEffects {

    private LikeBiomeClientEffects() {
    }

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

    public static Integer waterColor(BlockPos pos) {
        Biome biome = activeBiome(pos);
        return biome == null ? null : biome.getWaterColor();
    }

    public static Integer grassColor(BlockPos pos) {
        Biome biome = activeBiome(pos);
        return biome == null ? null : biome.getGrassColor(0.5D, 64.0D);
    }

    public static Integer foliageColor(BlockPos pos) {
        Biome biome = activeBiome(pos);
        return biome == null ? null : biome.getFoliageColor();
    }

    public static Integer skyColor(BlockPos pos) {
        Biome biome = activeBiome(pos);
        return biome == null ? null : biome.getSkyColor();
    }
}
