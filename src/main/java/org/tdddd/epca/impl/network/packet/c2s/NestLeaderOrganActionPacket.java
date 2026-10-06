package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncNestLeaderOrgansPacket;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganGate;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganSavedData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotUnlock;

import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

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
 *   <li> C2S {@code getSender()}  null</li>
 *   <li>{@link NestLeaderManager#isNestLeader(java.util.UUID)}</li>
 *   <li>{@link Action#INVENTORY_SLOT}  {@code [0, 36)}
 *       {@link #INVENTORY_SLOT_COUNT} {@code Inventory#INVENTORY_SIZE}
 *        return<b></b> tag /  / 777 </li>
 *   <li> {@code [0, OrganSlotGroup#totalSlots())}
 *       {@link NestLeaderOrganData#size()} </li>
 *   <li> {@code isUnlocked}
 *       {@link OrganSlotUnlock#tryUnlock}
 *        {@link NestLeaderOrganGate#scan} / {@code canModifySlot}</li>
 *   <li> {@link NestLeaderOrganData#acceptsItem(ItemStack)}
 *       {@code epca:organ_part} </li>
 *   <li>
 *       </li>
 * </ol>
 */
public class NestLeaderOrganActionPacket {

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
        /**
         * <b></b> {@code slot}  035
         *
         * <p> 0/1  {@code AbstractContainerMenu#doClick}
         * {@code ClickType.PICKUP} _tmp_vanilla_src  412435 </p>
         * <ul>
         *   <li> + </li>
         *   <li> +  1 </li>
         *   <li> +  /  1 </li>
         * </ul>
         * <p>Shift  =  + {@link NestLeaderOrganActionPacket#BUTTON_INVENTORY_QUICK_MOVE}
         *  {@code epca:organ_part}  / </p>
         */
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
     * {@code Inventory#items} = 3   9  + 9
     *
     * <p> {@code Inventory#INVENTORY_SIZE} _tmp_vanilla_src/normalized Inventory.java  29
     * </p>
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
     * 2
     *  action </p>
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


    public static void encode(NestLeaderOrganActionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.id());
        buf.writeVarInt(msg.slot);
        buf.writeVarInt(msg.button);
    }

    public static NestLeaderOrganActionPacket decode(FriendlyByteBuf buf) {
        Action action = Action.byId(buf.readVarInt());
        int slot = buf.readVarInt();
        int button = buf.readVarInt();
        return new NestLeaderOrganActionPacket(action == null ? Action.RETURN_CARRIED : action, slot, button);
    }


    public static void handle(NestLeaderOrganActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (!NestLeaderManager.isNestLeader(sender.getUUID())) {
                return;
            }

            NestLeaderOrganData data = NestLeaderOrganSavedData.readOrCreate(sender);
            NestLeaderOrganActionHandler.handle(sender, data, msg);
            NestLeaderOrganSavedData.write(sender, data);

            NestLeaderOrganGate.Gates gates = NestLeaderOrganGate.scan(sender);
            ModNetwork.sendToPlayer(sender, new SyncNestLeaderOrgansPacket(data, gates, false));
        });
        context.setPacketHandled(true);
    }
}

