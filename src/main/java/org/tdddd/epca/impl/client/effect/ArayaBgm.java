package org.tdddd.epca.impl.client.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ArayaConstants;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;

/**
 * The looping BGM the Alayavijnana staff plays while its 天杀 counter is at or above
 * {@link ArayaConstants#TIANSHA_THRESHOLD}.
 *
 * <h2>Why this is an {@link AbstractSoundInstance} and not a {@code level.playSound}</h2>
 * <p>A level sound is fire-and-forget: vanilla picks one {@code Sound} from the event's list, plays it
 * once and forgets it. This track is 184 seconds long, has to <b>loop</b> for as long as the condition
 * holds, has to be stoppable the instant the holder loses the staff or walks away, and must not be
 * distance-filtered by vanilla's own attenuation (that would fade a 3 MB music track down to nothing
 * over 16 blocks). Owning the instance is the only way to get all three.</p>
 *
 * <h2>Positioning and volume</h2>
 * <p>The instance is deliberately <b>non-attenuated</b> ({@link SoundInstance.Attenuation#NONE}) and
 * carries the holder's position as its own position. Vanilla's positional sound engine treats a
 * non-attenuated instance as 2D, so the track is not panned or occluded; the audible radius is instead
 * expressed by {@link #getVolume()}, which fades the track down over the outer part of
 * {@link ArayaConstants#BGM_STOP_RADIUS} and is recomputed on every query. That keeps the "nearby
 * players hear it" rule in the feature's own hands instead of in {@code sounds.json}'s
 * {@code attenuation_distance}, which only accepts an integer and cannot express a hysteresis.</p>
 *
 * <h2>Lifecycle</h2>
 * <p>One instance is created when the loop starts and destroyed ({@code SoundManager#stop}) when it
 * ends. {@link ArayaBgmManager} owns the single live instance, so there is never more than one track
 * playing and never a dangling one: the manager stops it when the counter drops below the threshold,
 * when the holder disappears, when the listener is too far away, and when the client level changes.</p>
 */
public final class ArayaBgm extends AbstractSoundInstance {

    /** Sound event name; the {@code sounds.json} entry must list the file with {@code stream: true}. */
    public static final ResourceLocation ID = new ResourceLocation(epca.MODID, "araya");

    /** Holder position this instance currently follows. */
    private Vec3 holderPosition;

    private ArayaBgm(Vec3 holderPosition) {
        super(ModSoundEvents.ARAYA.get(), SoundSource.MUSIC, RandomSource.create());
        this.holderPosition = holderPosition;
        this.volume = ArayaConstants.BGM_VOLUME;
        this.pitch = ArayaConstants.BGM_PITCH;
        this.looping = true;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = holderPosition.x;
        this.y = holderPosition.y;
        this.z = holderPosition.z;
    }

    /** Starts the loop at the given holder, or returns {@code null} when the asset is not installed. */
    public static ArayaBgm start(SoundManager soundManager, Vec3 holderPosition) {
        if (!ArayaBgmManager.isAssetPresent()) {
            // The sound event exists (sounds.json ships it) but the OGG may not: rather than firing a
            // missing-sound warning every time somebody reaches nine kills, the loop is simply skipped.
            return null;
        }
        ArayaBgm instance = new ArayaBgm(holderPosition);
        soundManager.play(instance);
        return instance;
    }

    /** Follows the holder; the position is only used for the source, the volume is computed. */
    public void follow(Vec3 position) {
        this.holderPosition = position;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    public Vec3 holderPosition() {
        return this.holderPosition;
    }

    /**
     * Volume as a function of how far the local listener is from the holder.
     *
     * <p>Full volume up to {@link ArayaConstants#BGM_RADIUS}, then a linear ramp down to silence at
     * {@link ArayaConstants#BGM_STOP_RADIUS}; {@link ArayaBgmManager} stops the instance once that
     * distance is reached, so the ramp only ever runs over the last few blocks.</p>
     */
    @Override
    public float getVolume() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return this.volume;
        }
        double distance = minecraft.player.position().distanceTo(this.holderPosition);
        if (distance <= ArayaConstants.BGM_RADIUS) {
            return this.volume;
        }
        double span = ArayaConstants.BGM_STOP_RADIUS - ArayaConstants.BGM_RADIUS;
        double t = (distance - ArayaConstants.BGM_RADIUS) / span;
        return (float) Math.max(0.0D, this.volume * (1.0D - t));
    }

    /** Distance from the local listener to the holder, or {@code Double.MAX_VALUE} without a player. */
    public double listenerDistance() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return Double.MAX_VALUE;
        }
        return minecraft.player.position().distanceTo(this.holderPosition);
    }
}
