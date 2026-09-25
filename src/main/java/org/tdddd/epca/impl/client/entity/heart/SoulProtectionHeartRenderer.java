package org.tdddd.epca.impl.client.entity.heart;

import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.tdddd.epca.impl.client.effect.SoulProtectionClientCache;
import org.tdddd.epca.impl.client.entity.gas.GasCloudRenderType;
import org.tdddd.epca.impl.client.entity.gas.GasCloudRenderer;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-only renderer for the golden "Heart" style of the {@code epca:soul_protection} effect: the
 * tall, irregular golden plasma/flame column of the reference image plus its little golden embers.
 *
 * <p>The class (and the style markers {@link GasCloudRenderType#HEART_STYLE_CHANNEL} /
 * {@link GasCloudRenderType#HEART_MOTE_STYLE_CHANNEL}) keeps the effect's "Heart" wording even
 * though the look is a flame, because those markers are part of the 1.20.1 / 26.1.2 twin
 * contract.</p>
 *
 * <p>Every {@link LivingEntity} that carries the effect gets one column, whatever mod or vanilla type
 * it is: the check is the plain client-visible effect lookup on {@link ModEffects#SOUL_PROTECTION}, so
 * no entity type is special-cased, no packet is introduced and the effect shows up on every client
 * that can see the entity.</p>
 *
 * <h2>Geometry</h2>
 * <p>The column's quad is {@code width : height = 1 : 2.1} ({@link #HEART_ASPECT_HEIGHT}) and its
 * height comes from the entity's hitbox,
 * {@code height = max(bbHeight, bbWidth * 2.1) * 1.10 * 1.20} ({@link #HEART_SIZE_PADDING} times
 * {@link #HEART_SIZE_SCALE}), so a tall mob is sized by its height and a wide one by its width. It is
 * centred on the entity's centre ({@code y + bbHeight * 0.5}) plus the {@link #HEART_BOB_AMPLITUDE}
 * bob, then pushed {@link #HEART_CAMERA_OFFSET_SCALE} times the hitbox width AWAY from the camera, so
 * the aura sits on the far side of the entity and the entity's own model occludes it. Both the column
 * and the embers are submitted through {@link GasCloudRenderer#submitBillboard} with
 * {@link GasCloudRenderer.BillboardMode#YAW_ONLY}, an upright quad that follows only the camera's
 * yaw, so the effect never tilts with the camera pitch. All shape, colour and motion numbers of the
 * look live in that shader's tunable HEART_* block and its mirror below; this class only computes
 * where the quads go, how big they are and which phases/fade the shader gets.</p>
 *
 * <h2>Blending</h2>
 * <p>The column is drawn through {@link GasCloudRenderType#get()}, i.e. the ordinary translucent
 * variant the gas clouds and water specks already use, so the shader's golden ramp reaches the screen
 * as colour. It used to use {@link GasCloudRenderType#getAdditive()}, but {@code SRC_ALPHA / ONE}
 * saturates against a lit world: in daylight every texel of the quad added up past white, so the whole
 * column read as one flat, hard-edged golden block instead of a flame. The tiny embers keep the
 * additive variant, where a blown-out glint is exactly what an ember should look like.</p>
 *
 * <h2>What each vertex colour channel carries</h2>
 * <p>The flame style ignores the tint (its colours come from the shader constants), so the four
 * vertex colour channels are reused as the per-quad animation/fade payload:</p>
 * <ul>
 *   <li>{@code Color.r} - boil/advection phase, {@link #HEART_BOIL_PERIOD_TICKS} tick period;</li>
 *   <li>{@code Color.g} - sway/jitter phase, {@link #HEART_SWAY_PERIOD_TICKS} tick period;</li>
 *   <li>{@code Color.b} - brightness flicker phase, {@link #HEART_FLICKER_PERIOD_TICKS} tick period;</li>
 *   <li>{@code Color.a} - the quad's fade (column and embers alike).</li>
 * </ul>
 * <p>The phases are normalised to {@code [0,1)} and the shader takes sines/cosines of them, so the
 * wrap back to 0 is seamless. The seed channel ({@code UV1.x} here, {@code UV2.x} in 1.20.1) stays
 * unused and the style channel ({@code UV1.y}) carries the style marker.</p>
 *
 * <h2>Motion</h2>
 * <ul>
 *   <li>slow breathing scale pulse of {@code +-}{@link #HEART_THROB_AMPLITUDE} with a
 *       {@link #HEART_THROB_PERIOD_TICKS} tick period (both half extents, so the aspect holds);</li>
 *   <li>gentle vertical bob of {@code +-}{@link #HEART_BOB_AMPLITUDE} blocks with a
 *       {@link #HEART_BOB_PERIOD_TICKS} tick period;</li>
 *   <li>the flame's own boil, sway, jitter and flicker, all inside the shader;</li>
 *   <li>{@link #HEART_MOTE_COUNT_MIN}-{@link #HEART_MOTE_COUNT_MAX} embers per entity, placed once on
 *       a world-space ring around the column at a fixed angle and drifting upward over
 *       {@link #HEART_MOTE_PERIOD_TICKS} ticks while fading in and out at the ends of their loop.</li>
 * </ul>
 *
 * <h2>Fade in and out</h2>
 * <p>The column fades in over {@link #HEART_FADE_IN_TICKS} ticks and out over
 * {@link #HEART_FADE_OUT_TICKS} ticks, the fade-out being proportional to the client-visible
 * remaining {@link MobEffectInstance#getDuration()} so it always leaves smoothly. The fade-in is
 * deliberately not driven by that duration: a beacon (or any other reapplier) refreshes the effect
 * every tick, so a duration-keyed fade-in would restart on every refresh and never reach full
 * opacity. {@link HeartState#ageTicks} therefore counts client ticks since the effect was first
 * observed on that entity id and is reset only when the effect really disappears.</p>
 *
 * <h2>26.1.2 adaptation</h2>
 * <p>26.1.2 renders in two phases: an extraction phase that still has the live entity and a
 * submission phase that only has {@code EntityRenderState}. Everything the 1.20.1 twin reads from
 * the entity at draw time therefore splits in two here, and nowhere else differs:</p>
 * <ul>
 *   <li>{@link #clientTick()} ages {@link HeartState#ageTicks} and builds the ember layout exactly as
 *       the 1.20.1 twin does, and is called from {@code ClientEvents} right after
 *       {@code GasCloudManager.clientTick()};</li>
 *   <li>{@link #onRegisterRenderStateModifiers} recomputes the same
 *       {@code min(fadeIn, fadeOut)} the 1.20.1 twin computes in {@code submit} and attaches it,
 *       together with the entity's {@link HeartState}, to the render state;</li>
 *   <li>{@link #submit} runs at the tail of {@code LivingEntityRenderer#submit} (from
 *       {@code LivingEntityRendererHeartMixin}) and uses the render state's hitbox, age and
 *       interpolated position in place of {@code entity.getBbHeight()}, {@code entity.tickCount} and
 *       {@code entity.getPosition(partialTick)}.</li>
 * </ul>
 *
 * <h2>Where "has the effect" comes from</h2>
 * <p>An entity counts as protected when the client itself holds the effect instance (the local
 * player, and entities whose effect vanilla sent to us) <em>or</em> when
 * {@link SoulProtectionClientCache} knows the id from the {@code SyncSoulProtectionPacket} batch
 * sync. That sync exists because vanilla never sends a mob's active effects to other clients, so
 * without it the aura could not appear on creatures at all. The remaining duration is taken from
 * whichever source is available, and a negative value (infinite effect, or an id whose duration the
 * server did not report) means "no fade-out".</p>
 */
@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class SoulProtectionHeartRenderer {

    // =================================================================
    //  Tunable block - mirrors the HEART_* block of gas_cloud.fsh
    // =================================================================

    // ---- submission geometry and fade timing ----------------------------------------------------

    /**
     * Aspect ratio of the column: {@code width : height = 1 : 2.1}. Mirrors the GLSL const
     * {@code HEART_ASPECT_HEIGHT}, which the fragment shader needs to build isotropic quad
     * coordinates.
     */
    public static final float HEART_ASPECT_HEIGHT = 2.1F;

    /**
     * Padding applied to the hitbox-derived height before it becomes the quad height. Mirrors the
     * GLSL const {@code HEART_SIZE_PADDING}; the shader does not read it, but both trees and the
     * constant check must keep the same value.
     */
    public static final float HEART_SIZE_PADDING = 1.10F;

    /**
     * Extra overall size of the aura, applied on top of {@link #HEART_SIZE_PADDING}. Both half
     * extents are multiplied by it, so the {@code 1 : 2.1} ratio is preserved; it exists so the
     * rendered size can be dialled without touching the hitbox maths.
     */
    public static final float HEART_SIZE_SCALE = 1.50F;

    /**
     * Ticks the column takes to fade in after the effect is first seen. Mirrors the GLSL const
     * {@code HEART_FADE_IN_TICKS}; the shader does not read it (the fade rides in {@code Color.a}).
     */
    public static final int HEART_FADE_IN_TICKS = 8;

    /**
     * Ticks the column takes to fade out; when the effect has less than this left, the fade-out is
     * proportional to the remaining duration instead. Mirrors the GLSL const
     * {@code HEART_FADE_OUT_TICKS}.
     */
    public static final int HEART_FADE_OUT_TICKS = 15;

    // ---- column silhouette ----------------------------------------------------------------------

    /** Fraction of the quad half extent the column spans (the rest is headroom for the wisps). Mirrors {@code HEART_FIT}. */
    public static final float HEART_FIT = 0.76F;
    /** Amplitude of the S-curved spine, in quad units. Mirrors {@code HEART_BEND}. */
    public static final float HEART_BEND = 0.07F;
    /** Frequency of the S-curve over the -1..1 vertical coordinate. Mirrors {@code HEART_BEND_FREQ}. */
    public static final float HEART_BEND_FREQ = 2.2F;
    /** How much the column narrows towards both ends. Mirrors {@code HEART_TAPER}. */
    public static final float HEART_TAPER = 0.45F;
    /** Extra narrowing of the upper half only, so the crown pinches off and dissolves while the foot stays broad. Mirrors {@code HEART_CROWN_TAPER}. */
    public static final float HEART_CROWN_TAPER = 0.45F;
    /** Vertical position at which the column starts fading out towards its ends. Mirrors {@code HEART_VERTICAL_FADE_START}. */
    public static final float HEART_VERTICAL_FADE_START = 0.62F;
    /** Mid-point of the three-octave fbm, subtracted so both noise fields are symmetric around zero. Mirrors {@code HEART_NOISE_CENTRE}. */
    public static final float HEART_NOISE_CENTRE = 0.4375F;
    /** Noise frequency across the quad width (kept high so the structure streaks vertically). Mirrors {@code HEART_NOISE_SCALE_X}. */
    public static final float HEART_NOISE_SCALE_X = 2.6F;
    /** Noise frequency across the quad height; kept well below the X scale so the features become long vertical tongues. Mirrors {@code HEART_NOISE_SCALE_Y}. */
    public static final float HEART_NOISE_SCALE_Y = 1.05F;
    /** Seed of the coarse structure noise. Mirrors {@code HEART_NOISE_SEED}. */
    public static final float HEART_NOISE_SEED = 2.3F;
    /** X frequency of the fine filament noise relative to the coarse field. Mirrors {@code HEART_DETAIL_SCALE_X}. */
    public static final float HEART_DETAIL_SCALE_X = 3.4F;
    /** Y frequency of the fine filament noise; also kept low for vertical streaking. Mirrors {@code HEART_DETAIL_SCALE_Y}. */
    public static final float HEART_DETAIL_SCALE_Y = 0.7F;
    /** Seed of the fine filament noise. Mirrors {@code HEART_DETAIL_SEED}. */
    public static final float HEART_DETAIL_SEED = 11.9F;
    /** How far the coarse field pushes the outline in and out, in half widths. Mirrors {@code HEART_EDGE_NOISE}. */
    public static final float HEART_EDGE_NOISE = 0.50F;
    /** Extra crown erosion: the outline noise is scaled by {@code 1 + HEART_CROWN_WISP * max(v, 0)}. Mirrors {@code HEART_CROWN_WISP}. */
    public static final float HEART_CROWN_WISP = 0.90F;
    /** How far the fine field modulates the interior brightness around 0.5. Mirrors {@code HEART_BREAK_STRENGTH}. */
    public static final float HEART_BREAK_STRENGTH = 0.68F;
    /** Gain that turns the horizontal distance into the 0..1 density ramp. Mirrors {@code HEART_EDGE_GAIN}. */
    public static final float HEART_EDGE_GAIN = 1.0F;

    // ---- core, ramp, colours, opacities ---------------------------------------------------------

    /** Half width of the bright core band around the spine. Mirrors {@code HEART_CORE_WIDTH}. */
    public static final float HEART_CORE_WIDTH = 0.09F;
    /** Soft outer feather of that core band. Mirrors {@code HEART_CORE_FEATHER}. */
    public static final float HEART_CORE_FEATHER = 0.14F;
    /** How much the filaments may dim the core, 0..1. Mirrors {@code HEART_CORE_MIN}. */
    public static final float HEART_CORE_MIN = 0.35F;
    /** How much the core adds to the intensity. Mirrors {@code HEART_CORE_BOOST}. */
    public static final float HEART_CORE_BOOST = 0.55F;
    /** Intensity at which the ramp leaves the darkest wisp colour. Mirrors {@code HEART_RAMP_WISP}. */
    public static final float HEART_RAMP_WISP = 0.10F;
    /** Intensity at which the ramp reaches the outer gold. Mirrors {@code HEART_RAMP_OUTER}. */
    public static final float HEART_RAMP_OUTER = 0.30F;
    /** Intensity at which the ramp reaches the mid gold. Mirrors {@code HEART_RAMP_MID}. */
    public static final float HEART_RAMP_MID = 0.55F;
    /** Intensity at which the ramp reaches the near-white core. Mirrors {@code HEART_RAMP_CORE}. */
    public static final float HEART_RAMP_CORE = 0.85F;
    /** Opacity of the faintest wisps. Mirrors {@code HEART_WISP_ALPHA}. */
    public static final float HEART_WISP_ALPHA = 0.40F;
    /** Opacity of the core. Mirrors {@code HEART_CORE_ALPHA}. */
    public static final float HEART_CORE_ALPHA = 0.80F;
    /** Core gold {@code #FFF7CC} (255, 247, 204). Mirrors the GLSL const {@code HEART_COLOR_CORE}. */
    public static final float HEART_COLOR_CORE_RED = 1.0F;
    public static final float HEART_COLOR_CORE_GREEN = 0.9686275F;
    public static final float HEART_COLOR_CORE_BLUE = 0.8F;
    /** Mid gold {@code #FFD24A} (255, 210, 74). Mirrors the GLSL const {@code HEART_COLOR_MID}. */
    public static final float HEART_COLOR_MID_RED = 1.0F;
    public static final float HEART_COLOR_MID_GREEN = 0.8235294F;
    public static final float HEART_COLOR_MID_BLUE = 0.2901961F;
    /** Outer gold {@code #E08A18} (224, 138, 24). Mirrors the GLSL const {@code HEART_COLOR_OUTER}. */
    public static final float HEART_COLOR_OUTER_RED = 0.8784314F;
    public static final float HEART_COLOR_OUTER_GREEN = 0.5411765F;
    public static final float HEART_COLOR_OUTER_BLUE = 0.0941176F;
    /** Deepest wisp {@code #8A4B08} (138, 75, 8). Mirrors the GLSL const {@code HEART_COLOR_WISP}. */
    public static final float HEART_COLOR_WISP_RED = 0.5411765F;
    public static final float HEART_COLOR_WISP_GREEN = 0.2941176F;
    public static final float HEART_COLOR_WISP_BLUE = 0.0313726F;

    // ---- flame motion ---------------------------------------------------------------------------

    /** How far the noise field is advected upward over one boil cycle; this is what makes the flame read as rising. Mirrors {@code HEART_FLOW}. */
    public static final float HEART_FLOW = 1.10F;
    /** Sideways wobble of the noise field. Mirrors {@code HEART_FLOW_SIDE}. */
    public static final float HEART_FLOW_SIDE = 0.26F;
    /** Sway amplitude of the column's spine. Mirrors {@code HEART_SWAY}. */
    public static final float HEART_SWAY = 0.055F;
    /** Small high-frequency jitter added to the sway. Mirrors {@code HEART_JITTER}. */
    public static final float HEART_JITTER = 0.022F;
    /** Frequency ratio of that jitter. Mirrors {@code HEART_JITTER_RATIO}. */
    public static final float HEART_JITTER_RATIO = 3.7F;
    /** Brightness flicker amplitude, about +-18%. Mirrors {@code HEART_FLICKER_AMPLITUDE}. */
    public static final float HEART_FLICKER_AMPLITUDE = 0.18F;
    /** Frequency ratio that breaks the flicker sine up. Mirrors {@code HEART_FLICKER_RATIO}. */
    public static final float HEART_FLICKER_RATIO = 2.7F;
    /** One full turn: the conversion from a 0..1 phase to the shader's sine/cosine angles. Mirrors {@code HEART_TWO_PI}. */
    public static final float HEART_TWO_PI = 6.2831853F;

    // ---- embers ---------------------------------------------------------------------------------

    /** Gold of the embers {@code #FFE27A} (255, 226, 122). Mirrors the GLSL const {@code HEART_MOTE_COLOR}. */
    public static final float HEART_MOTE_COLOR_RED = 1.0F;
    public static final float HEART_MOTE_COLOR_GREEN = 0.8862745F;
    public static final float HEART_MOTE_COLOR_BLUE = 0.4784314F;
    /** UV radius inside which an ember is at full brightness. Mirrors {@code HEART_MOTE_INNER}. */
    public static final float HEART_MOTE_INNER = 0.25F;

    // =================================================================
    //  CPU-only geometry and timing
    // =================================================================

    /** Relative size pulse of the slow breathing, about +-4%. */
    public static final float HEART_THROB_AMPLITUDE = 0.04F;
    /** Ticks of one breathing cycle. */
    public static final int HEART_THROB_PERIOD_TICKS = 40;
    /** Vertical bob amplitude in blocks, about +-0.03. */
    public static final float HEART_BOB_AMPLITUDE = 0.03F;
    /** Ticks of one bob cycle. */
    public static final int HEART_BOB_PERIOD_TICKS = 60;
    /**
     * How far the whole aura (column and embers) is shifted AWAY from the camera, as a fraction of
     * the entity's hitbox width.
     *
     * <p>Sign: the offset is added along {@code normalize(auraCentre - cameraPos)}, i.e. a positive
     * value pushes the aura to the far side of the entity, where the entity's own model naturally
     * occludes it (a negative value would push it towards the camera, in front of the model). The
     * aura stays depth tested, so terrain still occludes it - a depth-always branch would instead
     * draw it through walls. Set to 0 to get the plain "centred on the hitbox" placement back.</p>
     */
    public static final float HEART_CAMERA_OFFSET_SCALE = 0.40F;

    /** Ticks of one boil (upward advection) cycle. */
    public static final int HEART_BOIL_PERIOD_TICKS = 90;
    /** Ticks of one sway/jitter cycle. */
    public static final int HEART_SWAY_PERIOD_TICKS = 140;
    /** Ticks of one brightness flicker cycle. */
    public static final int HEART_FLICKER_PERIOD_TICKS = 23;

    /** Minimum number of embers per entity. */
    public static final int HEART_MOTE_COUNT_MIN = 2;
    /** Maximum number of embers per entity. */
    public static final int HEART_MOTE_COUNT_MAX = 6;
    /** Ember ring radius as a fraction of the quad width (kept above the column's 0.38 half width). */
    public static final float HEART_MOTE_RING_SCALE = 0.55F;
    /** Random angular jitter of an ember on its ring, in radians. */
    public static final float HEART_MOTE_ANGLE_JITTER = 0.6F;
    /** Ember diameter as a fraction of the quad width. */
    public static final float HEART_MOTE_DIAMETER_SCALE = 0.05F;
    /** Peak opacity of an ember. */
    public static final float HEART_MOTE_ALPHA = 0.55F;
    /** Vertical travel of an ember over one loop, as a fraction of the quad height. */
    public static final float HEART_MOTE_RISE_SCALE = 1.0F;
    /** Ticks of one ember loop (rise + fade in/out). */
    public static final int HEART_MOTE_PERIOD_TICKS = 110;
    /** Radius spread of the ember ring around {@link #HEART_MOTE_RING_SCALE}. */
    private static final float HEART_MOTE_RING_SPREAD = 0.5F;

    /** Alpha below which a quad is not worth submitting (mirrors the gas clouds' threshold). */
    private static final float MIN_VISIBLE_ALPHA = 0.004F;

    /** Tick gap after which the fade state of an unseen entity is dropped. */
    private static final int HEART_STATE_TTL_TICKS = 200;

    /**
     * Fade of the flame, in {@code [0, 1]}, attached to the render state during extraction; absent
     * when the entity does not carry the effect. 26.1.2 only (see the class comment).
     */
    private static final ContextKey<Float> HEART_FADE =
            new ContextKey<>(Identifier.fromNamespaceAndPath(epca.MODID, "soul_heart_fade"));

    /** The entity's fade state and ember layout, attached to the render state during extraction. */
    private static final ContextKey<HeartState> HEART_STATE =
            new ContextKey<>(Identifier.fromNamespaceAndPath(epca.MODID, "soul_heart_state"));

    /** Per-entity fade state and ember layout. */
    private static final Map<Integer, HeartState> STATES = new HashMap<>();

    /** Identity of the client level the current state belongs to; a change resets everything. */
    private static ClientLevel levelIdentity;
    private static long clientTickCounter;

    private SoulProtectionHeartRenderer() {
    }

    /**
     * Ages the per-entity fade state; called once per client tick from {@code ClientEvents}, right
     * after {@code GasCloudManager.clientTick()}. Identical to the 1.20.1 twin's tick.
     *
     * <p>It only reads state the client already has (which entities are rendering and whether they
     * carry the effect), so it sends nothing and changes nothing on the server.</p>
     */
    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            reset();
            return;
        }
        if (levelIdentity != level) {
            // Changing dimension / rejoining: every age belongs to the old level.
            reset();
            levelIdentity = level;
        }
        clientTickCounter++;

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            int id = living.getId();
            if (!living.hasEffect(ModEffects.SOUL_PROTECTION)
                    && !SoulProtectionClientCache.isActive(id)) {
                // The effect is gone (locally and, per the last batch, on the server too): a later
                // reapplication must fade in again from zero.
                STATES.remove(id);
                continue;
            }
            HeartState state = STATES.get(id);
            if (state == null) {
                // First observation of this effect on this entity: age 0, i.e. invisible.
                state = new HeartState();
                STATES.put(id, state);
            } else {
                state.ageTicks = Math.min(state.ageTicks + 1, HEART_FADE_IN_TICKS);
            }
            state.lastSeenTick = clientTickCounter;
            ensureMoteLayout(state, id);
        }
        pruneStates();
    }

    /**
     * Registers the render state modifier that moves the fade from the extraction phase (which still
     * has the live entity and its effect instance) to the submission phase. 26.1.2 only; the value it
     * computes is exactly the {@code min(fadeIn, fadeOut)} the 1.20.1 twin computes in its
     * {@code submit}.
     */
    @SubscribeEvent
    public static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (entity, renderState) -> {
                    int entityId = entity.getId();
                    MobEffectInstance effect = entity.getEffect(ModEffects.SOUL_PROTECTION);
                    int remainingTicks;
                    if (effect != null) {
                        // The client has the instance itself (local player, or an entity whose effect
                        // was sent to us); getDuration() is -1 for an infinite effect.
                        remainingTicks = effect.isInfiniteDuration()
                                ? SoulProtectionClientCache.INFINITE_DURATION
                                : effect.getDuration();
                    } else if (SoulProtectionClientCache.isActive(entityId)) {
                        // Vanilla never sends us a mob's effects; the batch sync does. The cache counts
                        // the reported duration down locally, so the fade-out still works between two
                        // syncs, and -1 (infinite) never fades out.
                        remainingTicks = SoulProtectionClientCache.remainingTicks(entityId);
                    } else {
                        return;
                    }
                    HeartState state = STATES.get(entityId);
                    int ageTicks = state == null ? 0 : state.ageTicks;
                    float partialTick = renderState.partialTick;
                    float fadeIn = Mth.clamp((ageTicks + partialTick) / HEART_FADE_IN_TICKS, 0.0F, 1.0F);
                    // The client ticks the duration down itself (MobEffectInstance#tick ->
                    // tickDownDuration), so this is a per-tick countdown; an infinite duration must
                    // never produce a fade-out.
                    float fadeOut = remainingTicks < 0
                            ? 1.0F
                            : Mth.clamp(remainingTicks / (float) HEART_FADE_OUT_TICKS, 0.0F, 1.0F);
                    renderState.setRenderData(HEART_FADE, Math.min(fadeIn, fadeOut));
                    if (state != null) {
                        renderState.setRenderData(HEART_STATE, state);
                    }
                });
    }

    /**
     * Submits the flame column and the embers of one entity, or nothing when it does not carry the
     * effect, when the custom core shader is not ready, or when the current fade is not visible.
     *
     * <p>Called from {@code impl/mixin/client/LivingEntityRendererHeartMixin} at the end of
     * {@code LivingEntityRenderer#submit}, where the {@link PoseStack} is back at the camera-relative
     * entity translation and the {@link SubmitNodeCollector} of the entity pass is still open -
     * exactly the state {@link GasCloudRenderer} expects.</p>
     *
     * @param basis the camera-relative entity pose the billboards are anchored to
     */
    public static void submit(LivingEntityRenderState state, PoseStack poseStack, PoseStack.Pose basis,
                              SubmitNodeCollector collector) {
        Float fadeValue = state.getRenderData(HEART_FADE);
        if (fadeValue == null) {
            return;
        }
        if (!GasCloudRenderType.isPipelineRegistered()) {
            // The styles are drawn by the shared custom pipeline; this renderer never falls back to a
            // built-in RenderType, so there is simply nothing to draw without that shader.
            return;
        }
        float fade = fadeValue;
        if (fade < MIN_VISIBLE_ALPHA) {
            return;
        }

        // Hitbox-adaptive size: a tall mob is sized by its height (enderman: 0.6 x 2.9 -> 3.19 x
        // 1.52) and a wide one by its width (ravager: 1.95 x 2.2 -> 4.50 x 2.14), then padded and
        // scaled up by HEART_SIZE_SCALE. Both half extents take the same factors, so the 1 : 2.1
        // ratio holds.
        float height = Math.max(state.boundingBoxHeight, state.boundingBoxWidth * HEART_ASPECT_HEIGHT)
                * HEART_SIZE_PADDING * HEART_SIZE_SCALE;
        float width = height / HEART_ASPECT_HEIGHT;

        HeartState heartState = state.getRenderData(HEART_STATE);

        // state.ageInTicks is entity.tickCount + partialTick, i.e. exactly the 1.20.1 twin's `time`.
        float time = state.ageInTicks;
        // Slow breathing (both half extents, so the 1:2.1 aspect holds) and gentle vertical bob.
        float breathing = 1.0F + HEART_THROB_AMPLITUDE
                * Mth.sin(time * HEART_TWO_PI / HEART_THROB_PERIOD_TICKS);
        float bob = HEART_BOB_AMPLITUDE * Mth.sin(time * HEART_TWO_PI / HEART_BOB_PERIOD_TICKS);
        double centerOffsetY = state.boundingBoxHeight * 0.5D + bob;

        // Place the whole aura on the far side of the entity (see HEART_CAMERA_OFFSET_SCALE): the
        // offset is world-aligned and GasCloudRenderer applies it before the vertical billboard yaw.
        Vec3 behindOffset = behindOffset(state);

        // The three animation phases travel in the vertex colour RGB; each wraps to 0 on its own
        // period, and because the shader only takes sines/cosines of them the wrap is seamless.
        float boilPhase = phase(time, HEART_BOIL_PERIOD_TICKS);
        float swayPhase = phase(time, HEART_SWAY_PERIOD_TICKS);
        float flickerPhase = phase(time, HEART_FLICKER_PERIOD_TICKS);

        // Vertical (yaw-only) billboard: the column always stays upright, so it never tilts with the
        // camera's pitch. It is drawn through the ordinary translucent variant (see the class comment
        // on blending), which is what lets the golden flame shape survive a lit background.
        GasCloudRenderer.submitVerticalBillboard(collector, poseStack, basis, state.x, state.y, state.z,
                behindOffset.x, behindOffset.y + centerOffsetY, behindOffset.z,
                width * 0.5F * breathing, height * 0.5F * breathing,
                boilPhase, swayPhase, flickerPhase, fade,
                0, GasCloudRenderType.HEART_STYLE_CHANNEL,
                GasCloudRenderType.get());

        submitMotes(state, poseStack, basis, collector, heartState, width, height,
                centerOffsetY, behindOffset, fade, time);
    }

    /**
     * The offset that moves this entity's aura AWAY from the camera, by
     * {@link #HEART_CAMERA_OFFSET_SCALE} times the hitbox width.
     *
     * <p>The sign is negative on purpose: the position is stepped along the entity-to-camera axis in
     * the opposite direction, which places the aura behind the model (occluded by the entity, still
     * depth-tested against terrain).</p>
     *
     * @return the world-aligned offset vector, or {@link Vec3#ZERO} when the entity is exactly at the
     *         camera (nothing sensible to normalize)
     */
    private static Vec3 behindOffset(LivingEntityRenderState state) {
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        double dx = cameraPos.x - state.x;
        double dy = cameraPos.y - state.y;
        double dz = cameraPos.z - state.z;
        double lengthSqr = dx * dx + dy * dy + dz * dz;
        if (lengthSqr < 1.0E-6D) {
            return Vec3.ZERO;
        }
        double scale = -HEART_CAMERA_OFFSET_SCALE * state.boundingBoxWidth / Math.sqrt(lengthSqr);
        return new Vec3(dx * scale, dy * scale, dz * scale);
    }

    /** Submits the little golden embers that drift upward around the column. */
    private static void submitMotes(LivingEntityRenderState state, PoseStack poseStack, PoseStack.Pose basis,
                                    SubmitNodeCollector collector, HeartState heartState, float width,
                                    float height, double centerOffsetY,
                                    Vec3 behindOffset, float fade, float time) {
        if (heartState == null) {
            return;
        }
        float halfSize = width * HEART_MOTE_DIAMETER_SCALE * 0.5F;
        for (int i = 0; i < heartState.moteCount; i++) {
            // Each ember runs its own 0..1 loop: it rises over the whole column and its alpha is zero
            // at both ends of the loop, so the wrap back to the bottom is invisible.
            float loop = wrap01(time / HEART_MOTE_PERIOD_TICKS + heartState.motePhase[i]);
            float moteAlpha = HEART_MOTE_ALPHA * fade * Mth.sin(loop * (float) Math.PI);
            if (moteAlpha < MIN_VISIBLE_ALPHA) {
                continue;
            }
            float ringRadius = width * HEART_MOTE_RING_SCALE * heartState.moteRadiusScale[i];
            double moteOffsetX = Math.cos(heartState.moteAngle[i]) * ringRadius;
            double moteOffsetZ = Math.sin(heartState.moteAngle[i]) * ringRadius;
            double moteOffsetY = centerOffsetY + (loop - 0.5D) * height * HEART_MOTE_RISE_SCALE;
            // The ember colour comes from the shader's HEART_MOTE_COLOR, so only the fade matters.
            // The behind-the-entity offset is added so the embers move with the column, and the same
            // upright (yaw only) billboard is used for them. They keep the additive variant: a few
            // pixels of blown-out gold read as a glint.
            GasCloudRenderer.submitVerticalBillboard(collector, poseStack, basis, state.x, state.y, state.z,
                    moteOffsetX + behindOffset.x, moteOffsetY + behindOffset.y, moteOffsetZ + behindOffset.z,
                    halfSize, halfSize,
                    1.0F, 1.0F, 1.0F, moteAlpha,
                    0, GasCloudRenderType.HEART_MOTE_STYLE_CHANNEL,
                    GasCloudRenderType.getAdditive());
        }
    }

    /**
     * Generates the ember layout of one entity once, so the embers keep the same angle, radius and
     * loop offset for as long as the effect lasts instead of jumping every frame.
     */
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

    /** Wraps a tick count into the {@code [0,1)} phase of one period. */
    private static float phase(float time, int periodTicks) {
        float wrapped = time % periodTicks;
        if (wrapped < 0.0F) {
            wrapped += periodTicks;
        }
        return wrapped / periodTicks;
    }

    /** Wraps an arbitrary value into {@code [0,1)}. */
    private static float wrap01(float value) {
        float wrapped = value % 1.0F;
        return wrapped < 0.0F ? wrapped + 1.0F : wrapped;
    }

    /** Drops the state of entities that stopped rendering while still carrying the effect. */
    private static void pruneStates() {
        if (STATES.isEmpty()) {
            return;
        }
        STATES.entrySet().removeIf(entry ->
                clientTickCounter - entry.getValue().lastSeenTick > HEART_STATE_TTL_TICKS);
    }

    /** Clears every state and the level identity; used when the client level changes or is left. */
    private static void reset() {
        STATES.clear();
        levelIdentity = null;
        clientTickCounter = 0;
    }

    /** Fade state and ember layout of one entity id. */
    private static final class HeartState {
        /** Client ticks the effect has been observed on the entity; 0 on the first observation. */
        private int ageTicks;
        /** Client tick the effect was last observed on the entity; used to expire stale state. */
        private long lastSeenTick;
        /** Number of embers; 0 until {@link #ensureMoteLayout} ran. */
        private int moteCount;
        /** Ember angle on the ring, in radians. */
        private float[] moteAngle;
        /** Per-ember radius multiplier around the ring radius. */
        private float[] moteRadiusScale;
        /** Per-ember loop offset, so the embers are not in lockstep. */
        private float[] motePhase;
    }
}
