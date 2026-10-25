package org.tdddd.epca.impl.overworld.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncHiveLeadersPacket;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganSavedData;

import java.util.Set;
import java.util.UUID;

public class HiveLeaderManager {
    public static HiveLeaderSavedData getSavedData() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        ServerLevel overworld = server.overworld();
        return HiveLeaderSavedData.get(overworld);
    }

    public static boolean isHiveLeader(UUID uuid) {
        HiveLeaderSavedData data = getSavedData();
        return data != null && data.isLeader(uuid);
    }

    public static boolean addHiveLeader(UUID uuid) {
        HiveLeaderSavedData data = getSavedData();
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
     * <p><b></b> SavedData {@code hiveleader_organs.dat}
     *  {@code HiveLeaderOrganSavedData} UUID
     *  {@code ServerPlayer}
     * <b></b>{@code Player#getPersistentData()}
     * {@code HiveLeaderOrgans} {@code Player}
     *  {@code /epca_hiveleader add}  {@code EntityArgument.player()}
     *  tick  /
     * {@code HiveLeaderOrganEffects#tick}  tick
     * {@code HiveLeaderOrganSavedData#readOrCreate} / </p>
     */
    private static void initOrganDefaults(UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) return;
        HiveLeaderOrganSavedData.initDefaultsIfAbsent(player);
    }

    public static boolean removeHiveLeader(UUID uuid) {
        HiveLeaderSavedData data = getSavedData();
        if (data != null) {
            data.removeLeader(uuid);
            broadcastLeaders();
            return true;
        }
        return false;
    }

    private static void broadcastLeaders() {
        HiveLeaderSavedData data = getSavedData();
        if (data == null) return;
        Set<UUID> leaders = data.getLeaders();
        ModNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncHiveLeadersPacket(leaders));
    }
}