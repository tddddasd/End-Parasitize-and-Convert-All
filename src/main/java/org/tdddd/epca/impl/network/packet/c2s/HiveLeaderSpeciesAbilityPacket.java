package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.organ.species.HiveLeaderSpeciesManager;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：种阶主动技能。
 *
 * <ul>
 *   <li>{@link HiveLeaderSpeciesManager#ACTION_CONVERT_RIPPER} —— 初探种：15 生物质把准星裂兽转成虫巢节点；</li>
 *   <li>{@link HiveLeaderSpeciesManager#ACTION_RESHAPE_BUFF} —— 重塑种：3 生物质换 15 秒力量/迅捷/饱和。</li>
 * </ul>
 *
 * <p>服务端在 {@link HiveLeaderSpeciesManager#handleAbility} 里重新校验：是否领巢者、当前种阶是否匹配、
 * 生物质是否足够、冷却、目标是否合法。客户端不做任何权威判断。</p>
 */
public class HiveLeaderSpeciesAbilityPacket {

    private final int action;

    public HiveLeaderSpeciesAbilityPacket(int action) {
        this.action = action;
    }

    public HiveLeaderSpeciesAbilityPacket(FriendlyByteBuf buf) {
        this.action = buf.readVarInt();
    }

    public static void encode(HiveLeaderSpeciesAbilityPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
    }

    public static HiveLeaderSpeciesAbilityPacket decode(FriendlyByteBuf buf) {
        return new HiveLeaderSpeciesAbilityPacket(buf);
    }

    public static void handle(HiveLeaderSpeciesAbilityPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) return;
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            HiveLeaderSpeciesManager.handleAbility(sender, msg.action);
        });
        context.setPacketHandled(true);
    }

    public int action() {
        return action;
    }
}
