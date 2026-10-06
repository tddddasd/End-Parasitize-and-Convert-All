package org.tdddd.epca.impl.overworld.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncNestLeadersPacket;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganSavedData;

import java.util.Set;
import java.util.UUID;

public class NestLeaderManager {
    public static NestLeaderSavedData getSavedData() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        ServerLevel overworld = server.overworld();
        return NestLeaderSavedData.get(overworld);
    }

    public static boolean isNestLeader(UUID uuid) {
        NestLeaderSavedData data = getSavedData();
        return data != null && data.isLeader(uuid);
    }

    public static boolean addNestLeader(UUID uuid) {
        NestLeaderSavedData data = getSavedData();
        if (data != null) {
            data.addLeader(uuid);
            // STAGE 1 SPEC
            //  16 39  22  / 1  / 4
            // 33  0-5  7  1  6  8
            initOrganDefaults(uuid);
            broadcastLeaders();
            return true;
        }
        return false;
    }

    /**
     *
     *
     * <p><b></b> SavedData {@code nestleader_organs.dat}
     *  {@code NestLeaderOrganSavedData} UUID
     *  {@code ServerPlayer}
     * <b></b>{@code Player#getPersistentData()}
     * {@code NestLeaderOrgans} {@code Player}
     *  {@code /epca_hiveleader add}  {@code EntityArgument.player()}
     *  tick  /
     * {@code NestLeaderOrganEffects#tick}  tick
     * {@code NestLeaderOrganSavedData#readOrCreate} / </p>
     */
    private static void initOrganDefaults(UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) return;
        NestLeaderOrganSavedData.initDefaultsIfAbsent(player);
    }

    public static boolean removeNestLeader(UUID uuid) {
        NestLeaderSavedData data = getSavedData();
        if (data != null) {
            data.removeLeader(uuid);
            broadcastLeaders();
            return true;
        }
        return false;
    }

    private static void broadcastLeaders() {
        NestLeaderSavedData data = getSavedData();
        if (data == null) return;
        Set<UUID> leaders = data.getLeaders();
        ModNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncNestLeadersPacket(leaders));
    }
}