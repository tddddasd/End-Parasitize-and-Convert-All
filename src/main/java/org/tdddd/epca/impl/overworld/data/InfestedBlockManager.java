package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.InfestedSourcePacket;
import org.tdddd.epca.impl.network.packet.s2c.SyncAllInfestedSourcesPacket;

public class InfestedBlockManager {
    public static void addInfestedBlock(ServerLevel level, BlockPos pos) {
        InfestedBlockSavedData data = InfestedBlockSavedData.get(level);
        data.add(pos);
        InfestedSourcePacket.AddInfestedSourcePacket packet = new InfestedSourcePacket.AddInfestedSourcePacket(pos);
        ModNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void removeInfestedBlock(ServerLevel level, BlockPos pos) {
        InfestedBlockSavedData data = InfestedBlockSavedData.get(level);
        data.remove(pos);
        InfestedSourcePacket.RemoveInfestedSourcePacket packet = new InfestedSourcePacket.RemoveInfestedSourcePacket(pos);
        ModNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void syncAllToPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        InfestedBlockSavedData data = InfestedBlockSavedData.get(level);
        SyncAllInfestedSourcesPacket packet = new SyncAllInfestedSourcesPacket(data.getAll());
        ModNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}