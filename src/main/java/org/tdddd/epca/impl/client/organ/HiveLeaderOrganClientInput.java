package org.tdddd.epca.impl.client.organ;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.tdddd.epca.impl.network.packet.c2s.HiveLeaderDecomposeParasitePacket;
import org.tdddd.epca.impl.network.packet.c2s.HiveLeaderOrganTeleportPacket;
import org.tdddd.epca.impl.network.packet.c2s.RequestOpenHiveLeaderOrgansPacket;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.registry.ModItems;

@OnlyIn(Dist.CLIENT)
public final class HiveLeaderOrganClientInput {

    private static final int TELEPORT_REQUIRED_PEARLS = 2;

    private HiveLeaderOrganClientInput() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (minecraft.screen != null) {
            return;
        }

        if (HiveLeaderOrganKeys.OPEN_ORGANS.consumeClick()) {
            ClientPacketDistributor.sendToServer(new RequestOpenHiveLeaderOrgansPacket());
        }

        if (HiveLeaderOrganKeys.TELEPORT.consumeClick()) {
            if (teleportUnlockedLocally()) {
                ClientPacketDistributor.sendToServer(new HiveLeaderOrganTeleportPacket());
            }
        }

        if (HiveLeaderOrganKeys.DECOMPOSE_PARASITE.consumeClick()) {
            ClientPacketDistributor.sendToServer(new HiveLeaderDecomposeParasitePacket());
        }
    }

    private static boolean teleportUnlockedLocally() {
        OrganSlotGroup group = OrganSlotGroup.HEAD_INNER;
        HiveLeaderOrganData data = HiveLeaderOrganClientData.data();
        int found = 0;
        for (int local = 0; local < group.size(); local++) {
            ItemStack stack = data.getItem(group.globalIndex(local));
            if (!stack.isEmpty() && stack.is(ModItems.INFESTED_ENDER_PEARL.get())) {
                found++;
                if (found >= TELEPORT_REQUIRED_PEARLS) {
                    return true;
                }
            }
        }
        return false;
    }
}
