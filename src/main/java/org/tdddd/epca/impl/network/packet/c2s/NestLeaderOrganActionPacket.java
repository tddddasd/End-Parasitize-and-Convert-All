package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncNestLeaderOrgansPacket;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganGate;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganSavedData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;

import javax.annotation.Nullable;

/**
 *  -&gt;  GUI
 * SPEC  2  2 +
 *
 * <h2></h2>
 * <ul>
 *   <li>{@code action} {@link Action}</li>
 *   <li>{@code slot}{@link Action#INVENTORY_SLOT} <b> 035</b>
 *       <b></b>{@link OrganSlotGroup#globalIndex(int)}051
 *        {@link Action#RETURN_CARRIED} </li>
 *   <li>{@code button}0 = 1 =  {@link Action#CLICK} /
 *       {@link Action#INVENTORY_SLOT} </li>
 * </ul>
 * <p><b></b> id/count/NBT
 *  {@code Inventory}
 * </p>
 *
 * <h2></h2>
 * <ul>
 *   <li>{@link Action#CLICK}///
 *        {@code AbstractContainerMenu#clicked}  0/1
 *       <b></b> 1
 *       ""</li>
 *   <li>{@link Action#QUICK_MOVE}Shift
 *       <b></b></li>
 *   <li>{@link Action#CLEAR}</li>
 *   <li>{@link Action#RETURN_CARRIED}GUI  / </li>
 *   <li>{@link Action#INVENTORY_SLOT}39  + 9  035</li>
 * </ul>
 * <p> {@link SyncNestLeaderOrgansPacket}
 *  52  +  STAGE 1  S2C </p>
 *
 * <h2> return</h2>
 * <ol>
 *   <li>{@code ctx.player()}  {@link ServerPlayer}</li>
 *   <li>{@link NestLeaderManager#isNestLeader(java.util.UUID)}</li>
 *   <li>{@link Action#INVENTORY_SLOT}  {@code [0, 36)}
 *       {@link #INVENTORY_SLOT_COUNT} {@code Inventory#INVENTORY_SIZE}  return</li>
 *   <li> {@code [0, OrganSlotGroup#totalSlots())} </li>
 *   <li> {@code isUnlocked}
 *        {@link NestLeaderOrganGate#canModifySlot}</li>
 *   <li> {@link NestLeaderOrganData#acceptsItem(net.minecraft.world.item.ItemStack)}
 *       {@code epca:organ_part} </li>
 *   <li></li>
 * </ol>
 *
 * <h2>1.20.1 -&gt; 26.1.2 </h2>
 * <p>{@code SimpleChannel} + {@code FriendlyByteBuf#writeVarInt/readVarInt}
 * {@link CustomPacketPayload} + {@link StreamCodec} varint
 * {@code RegistryFriendlyByteBuf}  {@code FriendlyByteBuf}</p>
 */
public class NestLeaderOrganActionPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NestLeaderOrganActionPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("nestleader_organ_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NestLeaderOrganActionPacket> STREAM_CODEC =
            CustomPacketPayload.codec(NestLeaderOrganActionPacket::encode,
                    NestLeaderOrganActionPacket::decode);

    /**  */
    public enum Action {
        /**  /  */
        CLICK,
        /** Shift  */
        QUICK_MOVE,
        /**  */
        CLEAR,
        /**  */
        RETURN_CARRIED,
        /** <b></b> {@code slot}  035 */
        INVENTORY_SLOT;

        /**  id ordinal */
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

    /**
     *  = 3   9  + 9
     *
     * <p> {@code Inventory#INVENTORY_SIZE} 26.1.2  36
     *  _tmp_26src Inventory.java  30 </p>
     */
    public static final int INVENTORY_SLOT_COUNT = 36;

    /**  */
    public static final int BUTTON_LEFT = 0;
    /**  */
    public static final int BUTTON_RIGHT = 1;
    /**  GUI  {@link Action#CLEAR} */
    public static final int BUTTON_MIDDLE = 2;
    /**
     * {@link Action#INVENTORY_SLOT} Shift
     *
     * <p> varint  {@code button} 0/1
     * 2 </p>
     */
    public static final int BUTTON_INVENTORY_QUICK_MOVE = BUTTON_MIDDLE;

    private final Action action;
    private final int slot;
    private final int button;

    public NestLeaderOrganActionPacket(Action action, int slot, int button) {
        this.action = action == null ? Action.RETURN_CARRIED : action;
        this.slot = slot;
        this.button = button;
    }

    public NestLeaderOrganActionPacket(Action action, int slot) {
        this(action, slot, BUTTON_LEFT);
    }

    /**  */
    public static NestLeaderOrganActionPacket returnCarried() {
        return new NestLeaderOrganActionPacket(Action.RETURN_CARRIED, -1, BUTTON_LEFT);
    }

    /**
     *  035{@code button}
     *
     * <p><b></b> {@code Inventory#getItem(int)}</p>
     */
    public static NestLeaderOrganActionPacket inventorySlot(int inventorySlot, int button) {
        return new NestLeaderOrganActionPacket(Action.INVENTORY_SLOT, inventorySlot, button);
    }

    /** GUI  shift  */
    public static NestLeaderOrganActionPacket inventoryQuickMove(int inventorySlot) {
        return new NestLeaderOrganActionPacket(Action.INVENTORY_SLOT, inventorySlot,
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

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void encode(NestLeaderOrganActionPacket packet, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(packet.action.id());
        buf.writeVarInt(packet.slot);
        buf.writeVarInt(packet.button);
    }

    public static NestLeaderOrganActionPacket decode(RegistryFriendlyByteBuf buf) {
        Action action = Action.byId(buf.readVarInt());
        int slot = buf.readVarInt();
        int button = buf.readVarInt();
        return new NestLeaderOrganActionPacket(action == null ? Action.RETURN_CARRIED : action, slot, button);
    }


    public static void handle(NestLeaderOrganActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            if (!NestLeaderManager.isNestLeader(sender.getUUID())) {
                return;
            }

            NestLeaderOrganData data = NestLeaderOrganSavedData.readOrCreate(sender);
            NestLeaderOrganActionHandler.handle(sender, data, packet);
            NestLeaderOrganSavedData.write(sender, data);

            NestLeaderOrganGate.Gates gates = NestLeaderOrganGate.scan(sender);
            ModNetwork.sendToPlayer(sender, new SyncNestLeaderOrgansPacket(data, gates, false));
        });
    }
}

