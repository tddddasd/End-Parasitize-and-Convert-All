package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeClientCache;
import org.tdddd.epca.impl.overworld.data.LikeBiomeManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client side colour application for like biomes.
 *
 * <p>Rendering rules depend on the erosion stage of the like biome:</p>
 * <ul>
 *     <li>stage below {@link LikeBiome#FULL_COLOR_STAGE} (i.e. &lt; 6): only the water colour is
 *     overridden;</li>
 *     <li>stage from {@link LikeBiome#FULL_COLOR_STAGE} on (i.e. &gt;= 6): water, sky (including
 *     fog) and block (grass/foliage) colours are overridden.</li>
 * </ul>
 *
 * <p>Unit colours are not painted as a hard 4x4x4 block: the outermost block of every unit (the
 * one block wide band hugging the four vertical faces of the unit, Y is untouched) is a smooth
 * blend of the own unit colour and the colour of the unit across that boundary. The blend is a
 * bilinear mix of up to four units (own, +/-X, +/-Z, diagonal), weighted with a smoothstep curve,
 * so a colour change in one unit also fades into its neighbours instead of producing a seam.
 * A neighbour without a like biome simply contributes the vanilla colour the caller passed in, so
 * the like biome fades into the untouched world with the same band width.</p>
 *
 * <p>Only colours the stage allows are blended: water always, grass and foliage only from
 * {@link LikeBiome#FULL_COLOR_STAGE} on. Sky/fog stay per unit (they are driven by the camera
 * position, not by the block band).</p>
 */
public final class LikeBiomeClientEffects {

    /** Water tint: allowed for every stage. */
    private static final int KIND_WATER = 0;

    /** Grass (block) tint: only from {@link LikeBiome#FULL_COLOR_STAGE} on. */
    private static final int KIND_GRASS = 1;

    /** Foliage (block) tint: only from {@link LikeBiome#FULL_COLOR_STAGE} on. */
    private static final int KIND_FOLIAGE = 2;

    /**
     * Width of the colour transition band in blocks, measured inwards from every vertical unit
     * boundary (X and Z only, the band runs through the whole unit height).
     */
    private static final int BAND = 1;

    /** Upper bound for the resolved colour cache, so a huge world edit cannot grow it forever. */
    private static final int UNIT_COLOR_LIMIT = 1 << 16;

    /** Neighbour direction per unit local coordinate, {@code 0} for the interior (no blend). */
    private static final int[] AXIS_DIR = new int[LikeBiomeManager.UNIT_SIZE];

    /** Blend weight of the neighbour per unit local coordinate, {@code 0} for the interior. */
    private static final float[] AXIS_WEIGHT = new float[LikeBiomeManager.UNIT_SIZE];

    static {
        int size = LikeBiomeManager.UNIT_SIZE;
        for (int i = 0; i < size; i++) {
            int distance = Math.min(i, size - 1 - i);
            if (distance >= BAND) {
                continue;
            }
            AXIS_DIR[i] = i < size / 2 ? -1 : 1;
            // A block is one block wide, so the outermost block samples the colour with its centre
            // half a block away from the boundary plane: smoothstep(1 - 0.5) = 0.5. Both sides of
            // one boundary therefore receive the exact same 50/50 mix (no seam), while the next
            // block inwards samples distance 1.5 -> weight 0 and keeps the pure unit colour.
            AXIS_WEIGHT[i] = smoothstep((BAND - (distance + 0.5F)) / BAND);
        }
    }

    private LikeBiomeClientEffects() {
    }

    private static float smoothstep(float t) {
        if (t <= 0.0F) {
            return 0.0F;
        }
        if (t >= 1.0F) {
            return 1.0F;
        }
        return t * t * (3.0F - 2.0F * t);
    }

    // ------------------------------------------------------------------
    // per unit colour resolution (cached, no per block biome lookup)
    // ------------------------------------------------------------------

    /** Resolved like biome colours of one unit, {@code -1} = this kind has no override there. */
    private record UnitColors(long stamp, int water, int grass, int foliage) {

        static final UnitColors NONE = new UnitColors(Long.MIN_VALUE, -1, -1, -1);
    }

    /**
     * Resolved colours per unit. Only units that carry a like biome are stored, so the map stays
     * proportional to the like biome count instead of the explored world size.
     */
    private static final Map<Long, UnitColors> UNIT_COLORS = new ConcurrentHashMap<>();

    /** Cache stamp (like biome generation + erosion stage) the map was filled for. */
    private static volatile long colorStamp = Long.MIN_VALUE;

    private static long stamp(int dimensionStage) {
        return ((long) LikeBiomeClientCache.generation() << 32) | (dimensionStage & 0xFFFFFFFFL);
    }

    private static int dimensionStage() {
        return ClientEvolutionData.getStageForDimension(Minecraft.getInstance().level);
    }

    /** Colour of one unit for one kind, {@code -1} when that unit does not override this kind. */
    private static int unitColor(long unitKey, int kind, int stage, long stamp) {
        UnitColors colors = unitColors(unitKey, stage, stamp);
        return switch (kind) {
            case KIND_WATER -> colors.water();
            case KIND_GRASS -> colors.grass();
            default -> colors.foliage();
        };
    }

    private static UnitColors unitColors(long unitKey, int stage, long stamp) {
        if (stamp != colorStamp) {
            UNIT_COLORS.clear();
            colorStamp = stamp;
        }
        UnitColors cached = UNIT_COLORS.get(unitKey);
        if (cached != null && cached.stamp() == stamp) {
            return cached;
        }
        LikeBiome like = LikeBiomeClientCache.atKey(unitKey);
        if (like == null || !like.overrideColors()) {
            // deliberately not cached: "no like biome" is the overwhelmingly common answer
            return UnitColors.NONE;
        }
        Biome biome = biomeOf(like);
        if (biome == null) {
            return UnitColors.NONE;
        }
        int water = biome.getWaterColor();
        int grass = -1;
        int foliage = -1;
        if (like.effectiveStage(stage) >= LikeBiome.FULL_COLOR_STAGE) {
            grass = biome.getGrassColor(0.5D, 64.0D);
            foliage = biome.getFoliageColor();
        }
        UnitColors resolved = new UnitColors(stamp, water, grass, foliage);
        if (UNIT_COLORS.size() >= UNIT_COLOR_LIMIT) {
            UNIT_COLORS.clear();
        }
        UNIT_COLORS.put(unitKey, resolved);
        return resolved;
    }

    // ------------------------------------------------------------------
    // blend
    // ------------------------------------------------------------------

    /**
     * Colour of {@code pos} after blending with the units across the nearest boundary, or
     * {@code null} when no like biome takes part and the vanilla colour should stay untouched.
     */
    @Nullable
    private static Integer blend(BlockPos pos, int kind, int vanillaColor) {
        if (LikeBiomeClientCache.size() == 0) {
            return null;
        }
        int stage = dimensionStage();
        long stamp = stamp(stage);

        int localX = pos.getX() & LikeBiomeManager.UNIT_MASK;
        int localZ = pos.getZ() & LikeBiomeManager.UNIT_MASK;
        float weightX = AXIS_WEIGHT[localX];
        float weightZ = AXIS_WEIGHT[localZ];

        int unitX = LikeBiomeManager.unitCoord(pos.getX());
        int unitY = LikeBiomeManager.unitCoord(pos.getY());
        int unitZ = LikeBiomeManager.unitCoord(pos.getZ());

        int ownColor = unitColor(LikeBiomeManager.unitKey(unitX, unitY, unitZ), kind, stage, stamp);
        if (weightX == 0.0F && weightZ == 0.0F) {
            // unit centre: always the pure colour, no neighbour lookup at all
            return ownColor < 0 ? null : ownColor;
        }

        float sumR = 0.0F;
        float sumG = 0.0F;
        float sumB = 0.0F;
        float vanillaWeight = 0.0F;
        boolean like = false;

        float ownWeight = (1.0F - weightX) * (1.0F - weightZ);
        if (ownColor >= 0) {
            like = true;
            sumR += ((ownColor >> 16) & 0xFF) * ownWeight;
            sumG += ((ownColor >> 8) & 0xFF) * ownWeight;
            sumB += (ownColor & 0xFF) * ownWeight;
        } else {
            vanillaWeight += ownWeight;
        }

        if (weightX > 0.0F) {
            float weight = weightX * (1.0F - weightZ);
            int color = unitColor(LikeBiomeManager.unitKey(unitX + AXIS_DIR[localX], unitY, unitZ),
                    kind, stage, stamp);
            if (color >= 0) {
                like = true;
                sumR += ((color >> 16) & 0xFF) * weight;
                sumG += ((color >> 8) & 0xFF) * weight;
                sumB += (color & 0xFF) * weight;
            } else {
                vanillaWeight += weight;
            }
        }

        if (weightZ > 0.0F) {
            float weight = weightZ * (1.0F - weightX);
            int color = unitColor(LikeBiomeManager.unitKey(unitX, unitY, unitZ + AXIS_DIR[localZ]),
                    kind, stage, stamp);
            if (color >= 0) {
                like = true;
                sumR += ((color >> 16) & 0xFF) * weight;
                sumG += ((color >> 8) & 0xFF) * weight;
                sumB += (color & 0xFF) * weight;
            } else {
                vanillaWeight += weight;
            }
        }

        if (weightX > 0.0F && weightZ > 0.0F) {
            float weight = weightX * weightZ;
            int color = unitColor(LikeBiomeManager.unitKey(unitX + AXIS_DIR[localX], unitY,
                    unitZ + AXIS_DIR[localZ]), kind, stage, stamp);
            if (color >= 0) {
                like = true;
                sumR += ((color >> 16) & 0xFF) * weight;
                sumG += ((color >> 8) & 0xFF) * weight;
                sumB += (color & 0xFF) * weight;
            } else {
                vanillaWeight += weight;
            }
        }

        if (!like) {
            return null;
        }
        if (vanillaWeight > 0.0F) {
            // "no like biome" participates with the vanilla colour of this very position
            sumR += ((vanillaColor >> 16) & 0xFF) * vanillaWeight;
            sumG += ((vanillaColor >> 8) & 0xFF) * vanillaWeight;
            sumB += (vanillaColor & 0xFF) * vanillaWeight;
        }
        return (channel(sumR) << 16) | (channel(sumG) << 8) | channel(sumB);
    }

    private static int channel(float value) {
        int rounded = Math.round(value);
        return rounded < 0 ? 0 : Math.min(rounded, 255);
    }

    // ------------------------------------------------------------------
    // biome lookup
    // ------------------------------------------------------------------

    /** @return the like biome of the unit containing {@code pos}, only when it may recolor at all. */
    @Nullable
    private static LikeBiome colorSource(BlockPos pos) {
        LikeBiome like = LikeBiomeClientCache.at(pos);
        if (like == null || !like.overrideColors()) {
            return null;
        }
        return like;
    }

    @Nullable
    private static Biome biomeOf(LikeBiome like) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        Identifier id = like.biomeId();
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
        return minecraft.level.registryAccess().lookupOrThrow(Registries.BIOME)
                .get(key).map(holder -> holder.value()).orElse(null);
    }

    /** Effective erosion stage at {@code pos}, {@code Integer.MIN_VALUE} when there is none. */
    public static int stageAt(BlockPos pos) {
        LikeBiome like = LikeBiomeClientCache.at(pos);
        if (like == null) {
            return Integer.MIN_VALUE;
        }
        return like.effectiveStage(dimensionStage());
    }

    /** @return true when this position may override sky and block colours. */
    public static boolean paintsFullColors(BlockPos pos) {
        return stageAt(pos) >= LikeBiome.FULL_COLOR_STAGE;
    }

    @Nullable
    public static Biome activeBiome(BlockPos pos) {
        LikeBiome like = colorSource(pos);
        return like == null ? null : biomeOf(like);
    }

    @Nullable
    private static Biome fullColorBiomeAt(BlockPos pos) {
        if (!paintsFullColors(pos)) {
            return null;
        }
        return activeBiome(pos);
    }

    /**
     * Water colour: applied for every stage, this is the only colour the water only stages change.
     *
     * @param vanillaColor colour the position would have without a like biome, used for the
     *                     transition towards a neighbour that has no like biome
     */
    @Nullable
    public static Integer waterColor(BlockPos pos, int vanillaColor) {
        return blend(pos, KIND_WATER, vanillaColor);
    }

    /**
     * Block (grass) tint: only from {@link LikeBiome#FULL_COLOR_STAGE} on.
     *
     * @param vanillaColor colour the position would have without a like biome, see {@link #waterColor}
     */
    @Nullable
    public static Integer grassColor(BlockPos pos, int vanillaColor) {
        return blend(pos, KIND_GRASS, vanillaColor);
    }

    /**
     * Block (foliage) tint: only from {@link LikeBiome#FULL_COLOR_STAGE} on.
     *
     * @param vanillaColor colour the position would have without a like biome, see {@link #waterColor}
     */
    @Nullable
    public static Integer foliageColor(BlockPos pos, int vanillaColor) {
        return blend(pos, KIND_FOLIAGE, vanillaColor);
    }

    /** Fog colour: only from {@link LikeBiome#FULL_COLOR_STAGE} on. */
    @Nullable
    public static Vector4f fogColor(BlockPos pos) {
        Biome biome = fullColorBiomeAt(pos);
        if (biome == null) {
            return null;
        }
        int color = biome.getAttributes().applyModifier(
                net.minecraft.world.attribute.EnvironmentAttributes.FOG_COLOR,
                net.minecraft.world.attribute.EnvironmentAttributes.FOG_COLOR.defaultValue());
        return new Vector4f(
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                1.0F);
    }

    /**
     * Sky colour: only from {@link LikeBiome#FULL_COLOR_STAGE} on. Deliberately not blended: the sky
     * is sampled at the camera position, there is no block band to fade over.
     */
    @Nullable
    public static Integer skyColor(BlockPos pos) {
        Biome biome = fullColorBiomeAt(pos);
        if (biome == null) {
            return null;
        }
        return biome.getAttributes().applyModifier(
                net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR,
                net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR.defaultValue());
    }
}
