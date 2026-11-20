package org.tdddd.epca.impl.client.organ;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.c2s.HiveLeaderDecomposeParasitePacket;
import org.tdddd.epca.impl.network.packet.c2s.HiveLeaderOrganTeleportPacket;
import org.tdddd.epca.impl.network.packet.c2s.HiveLeaderSpeciesAbilityPacket;
import org.tdddd.epca.impl.network.packet.c2s.RequestOpenHiveLeaderOrgansPacket;
import org.tdddd.epca.impl.network.packet.c2s.ToggleOrganModelPacket;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.data.organ.species.HiveLeaderSpeciesManager;
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
            ModNetwork.sendToServer(new RequestOpenHiveLeaderOrgansPacket());
        }

        if (HiveLeaderOrganKeys.TELEPORT.consumeClick()) {
            if (teleportUnlockedLocally()) {
                ModNetwork.sendToServer(new HiveLeaderOrganTeleportPacket());
            }
        }

        if (HiveLeaderOrganKeys.DECOMPOSE_PARASITE.consumeClick()) {
            ModNetwork.sendToServer(new HiveLeaderDecomposeParasitePacket());
        }

        if (HiveLeaderOrganKeys.TOGGLE_ORGAN_MODEL.consumeClick()) {
            // 状态由服务端持久化并广播（自己与其他玩家一致）。
            ModNetwork.sendToServer(new ToggleOrganModelPacket());
        }

        // 种阶主动技能：客户端只负责发包，种阶/生物质/目标/冷却全部由服务端校验。
        if (HiveLeaderOrganKeys.SPECIES_CONVERT_RIPPER.consumeClick()) {
            ModNetwork.sendToServer(new HiveLeaderSpeciesAbilityPacket(
                    HiveLeaderSpeciesManager.ACTION_CONVERT_RIPPER));
        }

        if (HiveLeaderOrganKeys.SPECIES_RESHAPE_BUFF.consumeClick()) {
            ModNetwork.sendToServer(new HiveLeaderSpeciesAbilityPacket(
                    HiveLeaderSpeciesManager.ACTION_RESHAPE_BUFF));
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
