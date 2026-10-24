package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.WaterColorEffectsManager;
import org.tdddd.epca.impl.overworld.data.InfestedBlockManager;

import java.util.function.Supplier;

public class InfestedSourcePacket {
    public static class AddInfestedSourcePacket {
        private final BlockPos pos;

        public AddInfestedSourcePacket(BlockPos pos) {
            this.pos = pos;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
        }

        public static AddInfestedSourcePacket decode(FriendlyByteBuf buf) {
            return new AddInfestedSourcePacket(buf.readBlockPos());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                
                WaterColorEffectsManager.addInfestedSource(pos);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    
    public static class RemoveInfestedSourcePacket {
        private final BlockPos pos;

        public RemoveInfestedSourcePacket(BlockPos pos) {
            this.pos = pos;
        }
        public void encode(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
        }

        public static RemoveInfestedSourcePacket decode(FriendlyByteBuf buf) {
            return new RemoveInfestedSourcePacket(buf.readBlockPos());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                
                WaterColorEffectsManager.removeInfestedSource(pos);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class RequestAllInfestedSourcesPacket {
        public void encode(FriendlyByteBuf buf) {
            
        }

        public static RequestAllInfestedSourcesPacket decode(FriendlyByteBuf buf) {
            return new RequestAllInfestedSourcesPacket();
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    InfestedBlockManager.syncAllToPlayer(player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * Many adds and removes in one packet.
     *
     * <p>Mass infestation (a beckon converting a whole area, or a hive node spreading) used to send one
     * {@link AddInfestedSourcePacket} per block to every player on the server. Batching turns thousands of
     * packets into one per tick.</p>
     */
    public static class BatchInfestedSourcePacket {
        private final java.util.List<BlockPos> added;
        private final java.util.List<BlockPos> removed;

        public BatchInfestedSourcePacket(java.util.List<BlockPos> added, java.util.List<BlockPos> removed) {
            this.added = added;
            this.removed = removed;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.added.size());
            for (BlockPos pos : this.added) {
                buf.writeBlockPos(pos);
            }
            buf.writeVarInt(this.removed.size());
            for (BlockPos pos : this.removed) {
                buf.writeBlockPos(pos);
            }
        }

        public static BatchInfestedSourcePacket decode(FriendlyByteBuf buf) {
            int addCount = buf.readVarInt();
            java.util.List<BlockPos> added = new java.util.ArrayList<>(addCount);
            for (int i = 0; i < addCount; i++) {
                added.add(buf.readBlockPos());
            }
            int removeCount = buf.readVarInt();
            java.util.List<BlockPos> removed = new java.util.ArrayList<>(removeCount);
            for (int i = 0; i < removeCount; i++) {
                removed.add(buf.readBlockPos());
            }
            return new BatchInfestedSourcePacket(added, removed);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                for (BlockPos pos : this.removed) {
                    WaterColorEffectsManager.removeInfestedSource(pos);
                }
                for (BlockPos pos : this.added) {
                    WaterColorEffectsManager.addInfestedSource(pos);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
