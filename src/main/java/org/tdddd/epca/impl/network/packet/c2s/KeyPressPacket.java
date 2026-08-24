package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.events.KeyInputHandler;

import java.util.UUID;
import java.util.function.Supplier;

public class KeyPressPacket {
    private final UUID playerId;
    private final boolean spacePressed;
    private final boolean shiftPressed;

    public KeyPressPacket(UUID playerId, boolean spacePressed, boolean shiftPressed) {
        this.playerId = playerId;
        this.spacePressed = spacePressed;
        this.shiftPressed = shiftPressed;
    }

    public static void encode(KeyPressPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeBoolean(packet.spacePressed);
        buffer.writeBoolean(packet.shiftPressed);
    }

    public static KeyPressPacket decode(FriendlyByteBuf buffer) {
        return new KeyPressPacket(buffer.readUUID(),
                buffer.readBoolean(),
                buffer.readBoolean());
    }

    public static void handle(KeyPressPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }

            
            KeyInputHandler.updateKeyState(
                    packet.playerId,
                    packet.spacePressed,
                    packet.shiftPressed
            );
        });
        context.setPacketHandled(true);
    }
}