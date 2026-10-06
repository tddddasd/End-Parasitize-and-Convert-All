package org.tdddd.epca.impl.client.organ;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.c2s.NestLeaderDecomposeParasitePacket;
import org.tdddd.epca.impl.network.packet.c2s.NestLeaderOrganTeleportPacket;
import org.tdddd.epca.impl.network.packet.c2s.RequestOpenNestLeaderOrgansPacket;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.registry.ModItems;

/**
 *  GUI SPEC  1  B2 GUI
 *
 * <h2> C2S </h2>
 * <p>
 *</b>{@link RequestOpenNestLeaderOrgansPacket}</p>
 * <pre>
 *   H
 *     -> C2S RequestOpenNestLeaderOrgansPacket
 *     -> getSender()  ->
 *     -> / +
 *     -> S2C SyncNestLeaderOrgansPacket52  +  +
 *     ->  -> new NestLeaderOrganScreen()  setScreen
 * </pre>
 * <p>" GUI /"""</p>
 *
 * <h2>STAGE B</h2>
 * <p>{@link NestLeaderOrganKeys#TELEPORT}  H
 * {@link NestLeaderOrganTeleportPacket}
 *  {@code NestLeaderOrganTeleportHandler}</p>
 * <ol>
 *   <li> H </li>
 *   <li><b></b>
 *       {@link NestLeaderOrganClientData#data()}  33   2
 *       <b></b>
 *       </li>
 * </ol>
 */
@OnlyIn(Dist.CLIENT)
public final class NestLeaderOrganClientInput {

    /** SPEC 2 */
    private static final int TELEPORT_REQUIRED_PEARLS = 2;

    private NestLeaderOrganClientInput() {
    }

    /**
     *  {@link NestLeaderOrganKeys#OPEN_ORGANS}
     *
     *
     * <p> {@code consumeClick()}  {@code isDown()} Forge/""
     * </p>
     */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (minecraft.screen != null) {
            return;
        }
        //  tick
        if (NestLeaderOrganKeys.OPEN_ORGANS.consumeClick()) {
            ModNetwork.sendToServer(new RequestOpenNestLeaderOrgansPacket());
        }
        //  consumeClick  false
        if (NestLeaderOrganKeys.TELEPORT.consumeClick()) {
            if (teleportUnlockedLocally()) {
                ModNetwork.sendToServer(new NestLeaderOrganTeleportPacket());
            }
        }
        //
        // <b></b>
        //  NestLeaderDecomposeParasiteHandler
        // ""
        // ""BiomassClientData  isNestLeader
        //  0 consumeClick()
        if (NestLeaderOrganKeys.DECOMPOSE_PARASITE.consumeClick()) {
            ModNetwork.sendToServer(new NestLeaderDecomposeParasitePacket());
        }
    }

    /**
     *  33  2
     *
     * <p> {@code SyncNestLeaderOrgansPacket} GUI
     *  {@link NestLeaderOrganClientData#data()}
     *  false
     * ""</p>
     *
     * <p> {@link OrganSlotGroup#HEAD_INNER}  9
     * {@link NestLeaderOrganData#getItem(int)} + {@code stack.is(item)}
     *  API</p>
     */
    private static boolean teleportUnlockedLocally() {
        OrganSlotGroup group = OrganSlotGroup.HEAD_INNER;
        NestLeaderOrganData data = NestLeaderOrganClientData.data();
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

