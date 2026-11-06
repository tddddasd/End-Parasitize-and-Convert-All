package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.joml.Vector4f;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeClientCache;

import javax.annotation.Nullable;

public final class LikeBiomeClientEffects {

    private LikeBiomeClientEffects() {
    }

    @Nullable
    public static Biome activeBiome(BlockPos pos) {
        if (pos == null || !LikeBiomeClientCache.overridesColors(pos)) {
            return null;
        }
        Identifier id = LikeBiomeClientCache.biomeId(pos);
        Minecraft minecraft = Minecraft.getInstance();
        if (id == null || minecraft.level == null) {
            return null;
        }
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
        return minecraft.level.registryAccess().lookupOrThrow(Registries.BIOME)
                .get(key).map(holder -> holder.value()).orElse(null);
    }

    @Nullable
    public static Integer waterColor(BlockPos pos) {
        Biome biome = activeBiome(pos);
        return biome == null ? null : biome.getWaterColor();
    }

    @Nullable
    public static Integer grassColor(BlockPos pos) {
        Biome biome = activeBiome(pos);
        return biome == null ? null : biome.getGrassColor(0.5D, 64.0D);
    }

    @Nullable
    public static Integer foliageColor(BlockPos pos) {
        Biome biome = activeBiome(pos);
        return biome == null ? null : biome.getFoliageColor();
    }

    @Nullable
    public static org.joml.Vector4f fogColor(BlockPos pos) {
        Biome biome = activeBiome(pos);
        if (biome == null) {
            return null;
        }
        int color = biome.getAttributes().applyModifier(
                net.minecraft.world.attribute.EnvironmentAttributes.FOG_COLOR,
                net.minecraft.world.attribute.EnvironmentAttributes.FOG_COLOR.defaultValue());
        return new org.joml.Vector4f(
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                1.0F);
    }

    @Nullable
    public static Integer skyColor(net.minecraft.core.BlockPos pos) {
        Biome biome = activeBiome(pos);
        if (biome == null) {
            return null;
        }
        return biome.getAttributes().applyModifier(
                net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR,
                net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR.defaultValue());
    }
}
