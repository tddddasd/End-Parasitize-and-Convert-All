package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.events.KeyInputHandler;
import org.tdddd.epca.impl.network.ModNetwork;

import java.util.UUID;


public class KeyPressPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KeyPressPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("key_press"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KeyPressPacket> STREAM_CODEC =
            CustomPacketPayload.codec(KeyPressPacket::encode, KeyPressPacket::decode);

    private final UUID playerId;
    private final boolean spacePressed;
    private final boolean shiftPressed;

    public KeyPressPacket(UUID playerId, boolean spacePressed, boolean shiftPressed) {
        this.playerId = playerId;
        this.spacePressed = spacePressed;
        this.shiftPressed = shiftPressed;
    }

    public static void encode(KeyPressPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeBoolean(packet.spacePressed);
        buffer.writeBoolean(packet.shiftPressed);
    }

    public static KeyPressPacket decode(RegistryFriendlyByteBuf buffer) {
        return new KeyPressPacket(buffer.readUUID(),
                buffer.readBoolean(),
                buffer.readBoolean());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KeyPressPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer)) {
                return;
            }

            KeyInputHandler.updateKeyState(
                    packet.playerId,
                    packet.spacePressed,
                    packet.shiftPressed
            );
        });
    }
}
