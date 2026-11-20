package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.organ.species.HiveLeaderSpeciesManager;

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
public class HiveLeaderSpeciesAbilityPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HiveLeaderSpeciesAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("hiveleader_species_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HiveLeaderSpeciesAbilityPacket> STREAM_CODEC =
            CustomPacketPayload.codec(HiveLeaderSpeciesAbilityPacket::encode,
                    HiveLeaderSpeciesAbilityPacket::decode);

    private final int action;

    public HiveLeaderSpeciesAbilityPacket(int action) {
        this.action = action;
    }

    public static void encode(HiveLeaderSpeciesAbilityPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
    }

    public static HiveLeaderSpeciesAbilityPacket decode(RegistryFriendlyByteBuf buf) {
        return new HiveLeaderSpeciesAbilityPacket(buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HiveLeaderSpeciesAbilityPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;
            HiveLeaderSpeciesManager.handleAbility(sender, msg.action);
        });
    }

    public int action() {
        return action;
    }
}
