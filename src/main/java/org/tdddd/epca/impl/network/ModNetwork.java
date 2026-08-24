package org.tdddd.epca.impl.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.packet.c2s.*;
import org.tdddd.epca.impl.network.packet.s2c.*;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(epca.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, FlightStatePacket.class,
                FlightStatePacket::toBytes,
                FlightStatePacket::new,
                FlightStatePacket::handle);

        INSTANCE.registerMessage(id++, PlayerMotionPacket.class,
                PlayerMotionPacket::toBytes,
                PlayerMotionPacket::new,
                PlayerMotionPacket::handle);

        INSTANCE.registerMessage(id++, KeyPacket.class,
                KeyPacket::toBytes,
                KeyPacket::new,
                KeyPacket::handle);

        INSTANCE.registerMessage(id++, AcidWaterColorPacket.class,
                AcidWaterColorPacket::encode,
                AcidWaterColorPacket::new,
                AcidWaterColorPacket::handle);

        INSTANCE.registerMessage(id++, KeyPressPacket.class,
                KeyPressPacket::encode,
                KeyPressPacket::decode,
                KeyPressPacket::handle
        );

        INSTANCE.registerMessage(
                id++,
                EnderErosionSyncPacket.class,
                EnderErosionSyncPacket::encode,
                EnderErosionSyncPacket::decode,
                EnderErosionSyncPacket::handle
        );

        INSTANCE.registerMessage(
                id++,
                ClearErosionDisplayPacket.class,
                ClearErosionDisplayPacket::encode,
                ClearErosionDisplayPacket::decode,
                ClearErosionDisplayPacket::handle
        );

        INSTANCE.registerMessage(
                id++,
                SyncEvolutionStagePacket.class,
                SyncEvolutionStagePacket::encode,
                SyncEvolutionStagePacket::decode,
                SyncEvolutionStagePacket::handle
        );

        INSTANCE.registerMessage(id++, PedestalItemSyncPacket.class,
                PedestalItemSyncPacket::encode,
                PedestalItemSyncPacket::decode,
                PedestalItemSyncPacket::handle
        );

        INSTANCE.registerMessage(id++, SyncNoteTabsPacket.class,
                SyncNoteTabsPacket::encode,
                SyncNoteTabsPacket::decode,
                SyncNoteTabsPacket::handle);

        INSTANCE.registerMessage(id++, ColorEffectPacket.class,
                ColorEffectPacket::encode,
                ColorEffectPacket::new,
                ColorEffectPacket::handle);

        INSTANCE.messageBuilder(SmallItemFrameDataPacket.class, id++)
                .encoder(SmallItemFrameDataPacket::encode)
                .decoder(SmallItemFrameDataPacket::decode)
                .consumerMainThread(SmallItemFrameDataPacket::handle)
                .add();

        INSTANCE.registerMessage(id++, InfestedSourcePacket.AddInfestedSourcePacket.class,
                InfestedSourcePacket.AddInfestedSourcePacket::encode,
                InfestedSourcePacket.AddInfestedSourcePacket::decode,
                InfestedSourcePacket.AddInfestedSourcePacket::handle);
        INSTANCE.registerMessage(id++, InfestedSourcePacket.RemoveInfestedSourcePacket.class,
                InfestedSourcePacket.RemoveInfestedSourcePacket::encode,
                InfestedSourcePacket.RemoveInfestedSourcePacket::decode,
                InfestedSourcePacket.RemoveInfestedSourcePacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(Object packet) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static <MSG> void send(PacketDistributor.PacketTarget target, MSG message) {
        INSTANCE.send(target, message);
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendToAllTracking(Object packet, LivingEntity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }

    public static void sendToAllTracking(Entity entity, Object packet) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }
}