package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;


public class VKeyStatePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<VKeyStatePacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("vkey_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VKeyStatePacket> STREAM_CODEC =
            CustomPacketPayload.codec(VKeyStatePacket::encode, VKeyStatePacket::new);

    private final boolean pressed;

    public VKeyStatePacket(boolean pressed) {
        this.pressed = pressed;
    }

    public VKeyStatePacket(RegistryFriendlyByteBuf buf) {
        this.pressed = buf.readBoolean();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(pressed);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VKeyStatePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                player.getPersistentData().putBoolean("VKeyPressed", packet.pressed);
            }
        });
    }
}
