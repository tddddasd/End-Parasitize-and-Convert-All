package org.tdddd.epca.impl.client.entity.heart;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.client.effect.SoulProtectionClientCache;
import org.tdddd.epca.impl.client.entity.gas.GasCloudRenderType;
import org.tdddd.epca.impl.client.entity.gas.GasCloudRenderer;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

import java.util.HashMap;
import java.util.Map;

public final class SoulProtectionHeartRenderer {

    public static final float HEART_ASPECT_HEIGHT = 2.1F;

    public static final float HEART_SIZE_PADDING = 1.10F;

    public static final int HEART_FADE_IN_TICKS = 8;

    public static final int HEART_FADE_OUT_TICKS = 15;

    public static final float HEART_FIT = 0.76F;

    public static final float HEART_BEND = 0.07F;

    public static final float HEART_BEND_FREQ = 2.2F;

    public static final float HEART_TAPER = 0.45F;

    public static final float HEART_CROWN_TAPER = 0.45F;

    public static final float HEART_VERTICAL_FADE_START = 0.62F;

    public static final float HEART_NOISE_CENTRE = 0.4375F;

    public static final float HEART_NOISE_SCALE_X = 2.6F;

    public static final float HEART_NOISE_SCALE_Y = 1.05F;

    public static final float HEART_NOISE_SEED = 2.3F;

    public static final float HEART_DETAIL_SCALE_X = 3.4F;

    public static final float HEART_DETAIL_SCALE_Y = 0.7F;

    public static final float HEART_DETAIL_SEED = 11.9F;

    public static final float HEART_EDGE_NOISE = 0.50F;

    public static final float HEART_CROWN_WISP = 0.90F;

    public static final float HEART_BREAK_STRENGTH = 0.68F;

    public static final float HEART_EDGE_GAIN = 1.0F;

    public static final float HEART_CORE_WIDTH = 0.09F;

    public static final float HEART_CORE_FEATHER = 0.14F;

    public static final float HEART_CORE_MIN = 0.35F;

    public static final float HEART_CORE_BOOST = 0.55F;

    public static final float HEART_RAMP_WISP = 0.10F;

    public static final float HEART_RAMP_OUTER = 0.30F;

    public static final float HEART_RAMP_MID = 0.55F;

    public static final float HEART_RAMP_CORE = 0.85F;

    public static final float HEART_WISP_ALPHA = 0.40F;

    public static final float HEART_CORE_ALPHA = 0.80F;

    public static final float HEART_COLOR_CORE_RED = 1.0F;
    public static final float HEART_COLOR_CORE_GREEN = 0.9686275F;
    public static final float HEART_COLOR_CORE_BLUE = 0.8F;

    public static final float HEART_COLOR_MID_RED = 1.0F;
    public static final float HEART_COLOR_MID_GREEN = 0.8235294F;
    public static final float HEART_COLOR_MID_BLUE = 0.2901961F;

    public static final float HEART_COLOR_OUTER_RED = 0.8784314F;
    public static final float HEART_COLOR_OUTER_GREEN = 0.5411765F;
    public static final float HEART_COLOR_OUTER_BLUE = 0.0941176F;

    public static final float HEART_COLOR_WISP_RED = 0.5411765F;
    public static final float HEART_COLOR_WISP_GREEN = 0.2941176F;
    public static final float HEART_COLOR_WISP_BLUE = 0.0313726F;

    public static final float HEART_FLOW = 1.10F;

    public static final float HEART_FLOW_SIDE = 0.26F;

    public static final float HEART_SWAY = 0.055F;

    public static final float HEART_JITTER = 0.022F;

    public static final float HEART_JITTER_RATIO = 3.7F;

    public static final float HEART_FLICKER_AMPLITUDE = 0.18F;

    public static final float HEART_FLICKER_RATIO = 2.7F;

    public static final float HEART_TWO_PI = 6.2831853F;

    public static final float HEART_MOTE_COLOR_RED = 1.0F;
    public static final float HEART_MOTE_COLOR_GREEN = 0.8862745F;
    public static final float HEART_MOTE_COLOR_BLUE = 0.4784314F;

    public static final float HEART_MOTE_INNER = 0.25F;

    public static final float HEART_THROB_AMPLITUDE = 0.04F;

    public static final int HEART_THROB_PERIOD_TICKS = 40;

    public static final float HEART_BOB_AMPLITUDE = 0.03F;

    public static final int HEART_BOB_PERIOD_TICKS = 60;

    public static final int HEART_BOIL_PERIOD_TICKS = 90;

    public static final int HEART_SWAY_PERIOD_TICKS = 140;

    public static final int HEART_FLICKER_PERIOD_TICKS = 23;

    public static final int HEART_MOTE_COUNT_MIN = 2;

    public static final int HEART_MOTE_COUNT_MAX = 6;

    public static final float HEART_MOTE_RING_SCALE = 0.55F;

    public static final float HEART_MOTE_ANGLE_JITTER = 0.6F;

    public static final float HEART_MOTE_DIAMETER_SCALE = 0.05F;

    public static final float HEART_MOTE_ALPHA = 0.55F;

    public static final float HEART_MOTE_RISE_SCALE = 1.0F;

    public static final int HEART_MOTE_PERIOD_TICKS = 110;

    private static final float HEART_MOTE_RING_SPREAD = 0.5F;

    public static final float HEART_SIZE_SCALE = 1.50F;

    public static final float HEART_CAMERA_OFFSET_SCALE = 0.40F;

    private static final float MIN_VISIBLE_ALPHA = 0.004F;

    private static final int HEART_STATE_TTL_TICKS = 200;

    private static final Map<Integer, HeartState> STATES = new HashMap<>();

    private static ClientLevel levelIdentity;
    private static long clientTickCounter;

    private SoulProtectionHeartRenderer() {
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            reset();
            return;
        }
        if (levelIdentity != level) {

            reset();
            levelIdentity = level;
        }
        clientTickCounter++;

        MobEffect effectType = ModEffects.SOUL_PROTECTION.get();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            int id = living.getId();

            if (effectType == null || (!living.hasEffect(effectType)
                    && !SoulProtectionClientCache.hasEffect(id))) {

                STATES.remove(id);
                continue;
            }
            HeartState state = STATES.get(id);
            if (state == null) {

                state = new HeartState();
                STATES.put(id, state);
            } else {
                state.ageTicks = Math.min(state.ageTicks + 1, HEART_FADE_IN_TICKS);
            }
            state.lastSeenTick = clientTickCounter;
        }
        pruneStates();
    }

    private static final boolean RENDER_EFFECT_ENABLED = false;

    public static void submit(LivingEntity entity, PoseStack poseStack, MultiBufferSource bufferSource,
                              float partialTick) {
        if (RENDER_EFFECT_ENABLED) {
            submitFlame(entity, poseStack, bufferSource, partialTick);
        }
    }

    private static void submitFlame(LivingEntity entity, PoseStack poseStack, MultiBufferSource bufferSource,
                              float partialTick) {
        MobEffect effectType = ModEffects.SOUL_PROTECTION.get();
        if (effectType == null) {
            return;
        }

        MobEffectInstance effect = entity.getEffect(effectType);
        int remainingTicks;
        if (effect != null) {
            remainingTicks = effect.isInfiniteDuration()
                    ? SoulProtectionClientCache.UNKNOWN_REMAINING_TICKS
                    : effect.getDuration();
        } else {
            if (!SoulProtectionClientCache.hasEffect(entity.getId())) {
                return;
            }
            remainingTicks = SoulProtectionClientCache.getRemainingTicks(entity.getId());
        }
        if (!GasCloudRenderType.isShaderReady()) {

            return;
        }

        float height = Math.max(entity.getBbHeight(), entity.getBbWidth() * HEART_ASPECT_HEIGHT)
                * HEART_SIZE_PADDING * HEART_SIZE_SCALE;
        float width = height / HEART_ASPECT_HEIGHT;

        HeartState state = STATES.get(entity.getId());
        int ageTicks = state == null ? 0 : state.ageTicks;

        float time = entity.tickCount + partialTick;
        float fadeIn = Mth.clamp((ageTicks + partialTick) / HEART_FADE_IN_TICKS, 0.0F, 1.0F);

        float fadeOut = remainingTicks < 0
                ? 1.0F
                : Mth.clamp(remainingTicks / (float) HEART_FADE_OUT_TICKS, 0.0F, 1.0F);
        float fade = Math.min(fadeIn, fadeOut);
        if (fade < MIN_VISIBLE_ALPHA) {
            return;
        }

        float breathing = 1.0F + HEART_THROB_AMPLITUDE
                * Mth.sin(time * HEART_TWO_PI / HEART_THROB_PERIOD_TICKS);
        float bob = HEART_BOB_AMPLITUDE * Mth.sin(time * HEART_TWO_PI / HEART_BOB_PERIOD_TICKS);
        double centerOffsetY = entity.getBbHeight() * 0.5D + bob;

        Vec3 behindOffset = behindOffset(entity, partialTick);

        float boilPhase = phase(time, HEART_BOIL_PERIOD_TICKS);
        float swayPhase = phase(time, HEART_SWAY_PERIOD_TICKS);
        float flickerPhase = phase(time, HEART_FLICKER_PERIOD_TICKS);

        GasCloudRenderer.submitVerticalBillboard(entity, poseStack, bufferSource, partialTick,
                behindOffset.x, behindOffset.y + centerOffsetY, behindOffset.z,
                width * 0.5F * breathing, height * 0.5F * breathing,
                boilPhase, swayPhase, flickerPhase, fade,
                0, GasCloudRenderType.HEART_STYLE_CHANNEL,
                GasCloudRenderType.get());

        submitMotes(entity, poseStack, bufferSource, partialTick, state, width, height,
                centerOffsetY, fade, time, behindOffset);
    }

    private static Vec3 behindOffset(LivingEntity entity, float partialTick) {
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 toCamera = cameraPos.subtract(entity.getPosition(partialTick));
        if (toCamera.lengthSqr() < 1.0E-6D) {
            return Vec3.ZERO;
        }
        return toCamera.normalize().scale(-HEART_CAMERA_OFFSET_SCALE * entity.getBbWidth());
    }

    private static void submitMotes(LivingEntity entity, PoseStack poseStack, MultiBufferSource bufferSource,
                                    float partialTick, HeartState state, float width, float height,
                                    double centerOffsetY, float fade, float time, Vec3 behindOffset) {
        if (state == null) {
            return;
        }
        ensureMoteLayout(state, entity.getId());
        float halfSize = width * HEART_MOTE_DIAMETER_SCALE * 0.5F;
        for (int i = 0; i < state.moteCount; i++) {

            float loop = wrap01(time / HEART_MOTE_PERIOD_TICKS + state.motePhase[i]);
            float moteAlpha = HEART_MOTE_ALPHA * fade * Mth.sin(loop * (float) Math.PI);
            if (moteAlpha < MIN_VISIBLE_ALPHA) {
                continue;
            }
            float ringRadius = width * HEART_MOTE_RING_SCALE * state.moteRadiusScale[i];
            double offsetX = behindOffset.x + Math.cos(state.moteAngle[i]) * ringRadius;
            double offsetZ = behindOffset.z + Math.sin(state.moteAngle[i]) * ringRadius;
            double offsetY = behindOffset.y + centerOffsetY
                    + (loop - 0.5D) * height * HEART_MOTE_RISE_SCALE;

            GasCloudRenderer.submitVerticalBillboard(entity, poseStack, bufferSource, partialTick,
                    offsetX, offsetY, offsetZ, halfSize, halfSize,
                    1.0F, 1.0F, 1.0F, moteAlpha,
                    0, GasCloudRenderType.HEART_MOTE_STYLE_CHANNEL,
                    GasCloudRenderType.getAdditive());
        }
    }

    private static void ensureMoteLayout(HeartState state, int entityId) {
        if (state.moteAngle != null) {
            return;
        }
        RandomSource random = RandomSource.create(entityId * 341873128712L + 132897987541L);
        int count = HEART_MOTE_COUNT_MIN
                + random.nextInt(HEART_MOTE_COUNT_MAX - HEART_MOTE_COUNT_MIN + 1);
        state.moteCount = count;
        state.moteAngle = new float[count];
        state.moteRadiusScale = new float[count];
        state.motePhase = new float[count];
        for (int i = 0; i < count; i++) {
            state.moteAngle[i] = (float) (i * (Math.PI * 2.0D / count))
                    + (random.nextFloat() - 0.5F) * HEART_MOTE_ANGLE_JITTER;
            state.moteRadiusScale[i] = 1.0F
                    + (random.nextFloat() - 0.5F) * HEART_MOTE_RING_SPREAD;
            state.motePhase[i] = random.nextFloat();
        }
    }

    private static float phase(float time, int periodTicks) {
        float wrapped = time % periodTicks;
        if (wrapped < 0.0F) {
            wrapped += periodTicks;
        }
        return wrapped / periodTicks;
    }

    private static float wrap01(float value) {
        float wrapped = value % 1.0F;
        return wrapped < 0.0F ? wrapped + 1.0F : wrapped;
    }

    private static void pruneStates() {
        if (STATES.isEmpty()) {
            return;
        }
        STATES.entrySet().removeIf(entry ->
                clientTickCounter - entry.getValue().lastSeenTick > HEART_STATE_TTL_TICKS);
    }

    private static void reset() {
        STATES.clear();

        SoulProtectionClientCache.clear();
        levelIdentity = null;
        clientTickCounter = 0;
    }

    private static final class HeartState {

        private int ageTicks;

        private long lastSeenTick;

        private int moteCount;

        private float[] moteAngle;

        private float[] moteRadiusScale;

        private float[] motePhase;
    }
}
