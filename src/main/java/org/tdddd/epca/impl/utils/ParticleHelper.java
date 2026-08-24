package org.tdddd.epca.impl.utils;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.tdddd.epca.impl.overworld.registry.ModParticles;

public class ParticleHelper {

    public static void sendParticleToAllPlayers(ServerLevel level, ParticleOptions particle,
                                                double x, double y, double z,
                                                double xd, double yd, double zd,
                                                int count, double speed) {
        if (particle.getType() == ModParticles.F_ADAPTATION.get() ||
                particle.getType() == ModParticles.P_ADAPTATION.get()) {

            ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                    particle, true, x, y, z,
                    (float) xd, (float) yd, (float) zd,
                    (float) speed, count
            );

            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                player.connection.send(packet);
            }
        } else {
            level.sendParticles(particle, x, y, z, count, xd, yd, zd, speed);
        }
    }
}