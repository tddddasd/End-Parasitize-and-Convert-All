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
 * The Araya NAMING EASTER EGG theme: a LOOPING, ATTENUATED track that plays while a renamed infested
 * enderman is in Araya mode.
 *
 * <p>Modelled directly on {@link ArayaBgm}, which is the mod's proven looping-audio pattern for this
 * version: an {@link AbstractSoundInstance} constructed once and handed to the
 * {@link SoundManager}, with {@code looping = true} and {@code attenuation = NONE} (the volume is
 * computed by hand from listener distance, so vanilla's own attenuation must be off or it would be
 * applied twice). A {@code TickableSoundInstance} is NOT used: the manager already runs from the client
 * tick and re-checks the entity every tick, which gives explicit control over when the loop stops.</p>
 *
 * <p><b>Why a separate instance from {@link ArayaBgm}:</b> that track belongs to the staff aura and this
 * one to the named enderman. They have different lifetimes and must be able to overlap, so they cannot
 * share one sound instance or one stop path.</p>
 *
 * <p><b>No leaks:</b> the instance is owned solely by {@link ArayaBossBgmManager}, which stops it when
 * the entity is dead, removed, out of range, no longer in Araya mode, or the client level changes.</p>
 */
public final class ArayaBossBgm extends AbstractSoundInstance {

    /** Sound event name; the {@code sounds.json} entry lists the file with {@code stream: true}. */
    public static final ResourceLocation ID = new ResourceLocation(epca.MODID, "araya_boss");

    /** Position this instance currently follows. */
    private Vec3 holderPosition;

    private ArayaBossBgm(Vec3 holderPosition) {
        super(ModSoundEvents.ARAYA_BOSS.get(), SoundSource.MUSIC, RandomSource.create());
        this.holderPosition = holderPosition;
        this.volume = ArayaConstants.BGM_VOLUME;
        this.pitch = ArayaConstants.BGM_PITCH;
        this.looping = true;
        this.relative = false;
        // Volume is computed manually in getVolume(), so vanilla attenuation must be OFF or the two
        // would compound and the track would fade twice as fast.
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = holderPosition.x;
        this.y = holderPosition.y;
        this.z = holderPosition.z;
    }

    /** Starts the loop at the given position, or returns {@code null} when the asset is not installed. */
    public static ArayaBossBgm start(SoundManager soundManager, Vec3 holderPosition) {
        if (!ArayaBossBgmManager.isAssetPresent()) {
            // The event exists in sounds.json but the OGG may not be shipped: skip silently rather than
            // firing a missing-sound warning every time somebody renames an enderman.
            return null;
        }
        ArayaBossBgm instance = new ArayaBossBgm(holderPosition);
        soundManager.play(instance);
        return instance;
    }

    /** Follows the holder; only the source position is used, the volume is computed. */
    public void follow(Vec3 position) {
        this.holderPosition = position;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    public Vec3 holderPosition() {
        return this.holderPosition;
    }

    /** Same distance ramp as {@link ArayaBgm}: full inside BGM_RADIUS, linear to silence at STOP. */
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

    /** Distance from the local listener to the holder, or max value without a player. */
    public double listenerDistance() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return Double.MAX_VALUE;
        }
        return minecraft.player.position().distanceTo(this.holderPosition);
    }
}
