package org.tdddd.epca.impl.client.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ArayaConstants;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;

public final class ArayaBossBgm extends AbstractSoundInstance {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(epca.MODID, "araya_boss");

    private Vec3 holderPosition;

    private ArayaBossBgm(Vec3 holderPosition) {
        super(ModSoundEvents.ARAYA_BOSS.get(), SoundSource.MUSIC, RandomSource.create());
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

    public static ArayaBossBgm start(SoundManager soundManager, Vec3 holderPosition) {
        if (!ArayaBossBgmManager.isAssetPresent()) {

            return null;
        }
        ArayaBossBgm instance = new ArayaBossBgm(holderPosition);
        soundManager.play(instance);
        return instance;
    }

    public void follow(Vec3 position) {
        this.holderPosition = position;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    public Vec3 holderPosition() {
        return this.holderPosition;
    }

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

    public double listenerDistance() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return Double.MAX_VALUE;
        }
        return minecraft.player.position().distanceTo(this.holderPosition);
    }
}
