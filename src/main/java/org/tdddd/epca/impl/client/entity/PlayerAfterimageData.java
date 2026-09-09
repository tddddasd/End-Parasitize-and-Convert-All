package org.tdddd.epca.impl.client.entity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class PlayerAfterimageData {
    public final Vec3 position;
    public final float yRot;
    public final int spawnTick;
    public final int lifetime = 20;
    public final Map<String, ModelPartSnapshot> partSnapshots;

    public PlayerAfterimageData(Player player, int spawnTick, Map<String, ModelPartSnapshot> snapshots) {
        this.position = player.position();
        this.yRot = player.getYRot();
        this.spawnTick = spawnTick;
        this.partSnapshots = snapshots;
    }

    public float getAlpha(int currentTick) {
        int age = currentTick - spawnTick;
        if (age < 0 || age >= lifetime) return 0.0f;
        return 1.0f - (float) age / lifetime;
    }

    public boolean isAlive(int currentTick) {
        int age = currentTick - spawnTick;
        return age >= 0 && age < lifetime;
    }
}