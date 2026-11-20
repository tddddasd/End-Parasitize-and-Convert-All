package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.organ.HiveLeaderSpeciesClientData;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.organ.species.OrganSpeciesCounts;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 服务端 → 客户端：领巢者当前特化种阶 + 进化击杀数 + 四种种阶器官计数 + 重塑种伤害适应性进度。
 *
 * <p>{@code adaptations} 是 {@code DamageType id → 已适应层数} 的只读快照（已过滤黑名单，
 * 按层数从高到低排序，最多 {@link #MAX_ADAPTATION_ENTRIES} 条）。客户端只做展示
 * （HUD / 器官界面读数），权威状态在服务端玩家持久化数据里。</p>
 */
public class SyncHiveLeaderSpeciesPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncHiveLeaderSpeciesPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("hiveleader_sync_species"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncHiveLeaderSpeciesPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncHiveLeaderSpeciesPacket::encode,
                    SyncHiveLeaderSpeciesPacket::decode);

    /** 客户端展示用的适应性条目上限。 */
    public static final int MAX_ADAPTATION_ENTRIES = 8;

    private final String speciesId;
    private final int kills;
    private final int infested;
    private final int onesent;
    private final int poverty;
    private final int reshape;
    private final boolean modelVisible;
    private final Map<String, Integer> adaptations;

    public SyncHiveLeaderSpeciesPacket(String speciesId, int kills,
                                       OrganSpeciesCounts counts, boolean modelVisible,
                                       @Nullable Map<String, Integer> adaptations) {
        this.speciesId = speciesId == null ? "" : speciesId;
        this.kills = kills;
        OrganSpeciesCounts safe = counts == null ? OrganSpeciesCounts.EMPTY : counts;
        this.infested = safe.infested();
        this.onesent = safe.onesent();
        this.poverty = safe.poverty();
        this.reshape = safe.reshape();
        this.modelVisible = modelVisible;
        this.adaptations = copyAdaptations(adaptations);
    }

    private SyncHiveLeaderSpeciesPacket(String speciesId, int kills, int infested, int onesent,
                                        int poverty, int reshape, boolean modelVisible,
                                        @Nullable Map<String, Integer> adaptations) {
        this.speciesId = speciesId == null ? "" : speciesId;
        this.kills = kills;
        this.infested = infested;
        this.onesent = onesent;
        this.poverty = poverty;
        this.reshape = reshape;
        this.modelVisible = modelVisible;
        this.adaptations = copyAdaptations(adaptations);
    }

    @Nullable
    private static Map<String, Integer> copyAdaptations(@Nullable Map<String, Integer> source) {
        Map<String, Integer> copy = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, Integer> entry : source.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) continue;
                copy.put(entry.getKey(), entry.getValue());
                if (copy.size() >= MAX_ADAPTATION_ENTRIES) break;
            }
        }
        return copy;
    }

    public static void encode(SyncHiveLeaderSpeciesPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeUtf(msg.speciesId);
        buf.writeVarInt(msg.kills);
        buf.writeVarInt(msg.infested);
        buf.writeVarInt(msg.onesent);
        buf.writeVarInt(msg.poverty);
        buf.writeVarInt(msg.reshape);
        buf.writeBoolean(msg.modelVisible);

        buf.writeVarInt(msg.adaptations.size());
        for (Map.Entry<String, Integer> entry : msg.adaptations.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    public static SyncHiveLeaderSpeciesPacket decode(RegistryFriendlyByteBuf buf) {
        String speciesId = buf.readUtf();
        int kills = buf.readVarInt();
        int infested = buf.readVarInt();
        int onesent = buf.readVarInt();
        int poverty = buf.readVarInt();
        int reshape = buf.readVarInt();
        boolean modelVisible = buf.readBoolean();

        int size = Math.min(MAX_ADAPTATION_ENTRIES, Math.max(0, buf.readVarInt()));
        Map<String, Integer> adaptations = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            int stacks = buf.readVarInt();
            if (key == null || key.isBlank() || stacks <= 0) continue;
            adaptations.put(key, stacks);
        }

        return new SyncHiveLeaderSpeciesPacket(
                speciesId, kills, infested, onesent, poverty, reshape, modelVisible, adaptations);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncHiveLeaderSpeciesPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> HiveLeaderSpeciesClientData.update(
                msg.speciesId, msg.kills, msg.infested, msg.onesent, msg.poverty, msg.reshape,
                msg.modelVisible, msg.adaptations));
    }

    public String speciesId() {
        return speciesId;
    }

    public int kills() {
        return kills;
    }

    public int infested() {
        return infested;
    }

    public int onesent() {
        return onesent;
    }

    public int poverty() {
        return poverty;
    }

    public int reshape() {
        return reshape;
    }

    public boolean modelVisible() {
        return modelVisible;
    }

    public Map<String, Integer> adaptations() {
        return adaptations;
    }
}
