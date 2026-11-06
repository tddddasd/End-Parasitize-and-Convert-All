package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncHiveLeaderOrgansPacket;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganGate;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganSavedData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotUnlock;

import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

public class HiveLeaderOrganActionPacket {

    public enum Action {

        CLICK,

        QUICK_MOVE,

        CLEAR,

        RETURN_CARRIED,

        INVENTORY_SLOT;

        public int id() {
            return switch (this) {
                case CLICK -> 0;
                case QUICK_MOVE -> 1;
                case CLEAR -> 2;
                case RETURN_CARRIED -> 3;
                case INVENTORY_SLOT -> 4;
            };
        }

        @Nullable
        public static Action byId(int id) {
            return switch (id) {
                case 0 -> CLICK;
                case 1 -> QUICK_MOVE;
                case 2 -> CLEAR;
                case 3 -> RETURN_CARRIED;
                case 4 -> INVENTORY_SLOT;
                default -> null;
            };
        }
    }

    public static final int INVENTORY_SLOT_COUNT = 36;

    public static final int BUTTON_LEFT = 0;

    public static final int BUTTON_RIGHT = 1;

    public static final int BUTTON_MIDDLE = 2;

    public static final int BUTTON_INVENTORY_QUICK_MOVE = BUTTON_MIDDLE;

    private final Action action;
    private final int slot;
    private final int button;

    public HiveLeaderOrganActionPacket(Action action, int slot, int button) {
        this.action = action == null ? Action.RETURN_CARRIED : action;
        this.slot = slot;
        this.button = button;
    }

    public HiveLeaderOrganActionPacket(Action action, int slot) {
        this(action, slot, BUTTON_LEFT);
    }

    public static HiveLeaderOrganActionPacket returnCarried() {
        return new HiveLeaderOrganActionPacket(Action.RETURN_CARRIED, -1, BUTTON_LEFT);
    }

    public static HiveLeaderOrganActionPacket inventorySlot(int inventorySlot, int button) {
        return new HiveLeaderOrganActionPacket(Action.INVENTORY_SLOT, inventorySlot, button);
    }

    public static HiveLeaderOrganActionPacket inventoryQuickMove(int inventorySlot) {
        return new HiveLeaderOrganActionPacket(Action.INVENTORY_SLOT, inventorySlot,
                BUTTON_INVENTORY_QUICK_MOVE);
    }

    public Action action() {
        return action;
    }

    public int slot() {
        return slot;
    }

    public int button() {
        return button;
    }

    public static void encode(HiveLeaderOrganActionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.id());
        buf.writeVarInt(msg.slot);
        buf.writeVarInt(msg.button);
    }

    public static HiveLeaderOrganActionPacket decode(FriendlyByteBuf buf) {
        Action action = Action.byId(buf.readVarInt());
        int slot = buf.readVarInt();
        int button = buf.readVarInt();
        return new HiveLeaderOrganActionPacket(action == null ? Action.RETURN_CARRIED : action, slot, button);
    }

    public static void handle(HiveLeaderOrganActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (!HiveLeaderManager.isHiveLeader(sender.getUUID())) {
                return;
            }

            HiveLeaderOrganData data = HiveLeaderOrganSavedData.readOrCreate(sender);
            HiveLeaderOrganActionHandler.handle(sender, data, msg);
            HiveLeaderOrganSavedData.write(sender, data);

            HiveLeaderOrganGate.Gates gates = HiveLeaderOrganGate.scan(sender);
            ModNetwork.sendToPlayer(sender, new SyncHiveLeaderOrgansPacket(data, gates, false));
        });
        context.setPacketHandled(true);
    }
}
