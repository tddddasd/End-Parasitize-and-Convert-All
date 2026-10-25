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

/**
 *  GUI SPEC  1  B2 GUI
 *
 * <h2> C2S </h2>
 * <p>
 * <b></b>{@link RequestOpenHiveLeaderOrgansPacket}</p>
 * <pre>
 *   H
 *     -> C2S RequestOpenHiveLeaderOrgansPacket
 *     -> player()  ->
 *     -> / +
 *     -> S2C SyncHiveLeaderOrgansPacket52  +  +
 *     ->  -> new HiveLeaderOrganScreen()  setScreen
 * </pre>
 * <p>" GUI /"""</p>
 *
 * <h2>STAGE B</h2>
 * <p>{@link HiveLeaderOrganKeys#TELEPORT}  H
 * {@link HiveLeaderOrganTeleportPacket}</p>
 * <ol>
 *   <li> H </li>
 *   <li><b></b>
 *       {@link HiveLeaderOrganClientData#data()}  33   2
 *       <b></b>
 *       </li>
 * </ol>
 *
 * <h2>1.20.1 -&gt; 26.1.2  API </h2>
 * <ul>
 *   <li>{@code ModNetwork.INSTANCE.sendToServer(payload)}1.20.1  {@code SimpleChannel}
 *       -&gt; {@code ClientPacketDistributor.sendToServer(payload)}
 *       26.1.2  NeoForge  {@code ClientHandlerI} </li>
 *   <li>{@code OnlyIn}  {@code net.neoforged.api.distmarker.OnlyIn}</li>
 *   <li>{@code Minecraft#player} / {@code #level} / {@code #screen} / {@code KeyMapping#consumeClick()}
 *       </li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class HiveLeaderOrganClientInput {

    /** SPEC 2 */
    private static final int TELEPORT_REQUIRED_PEARLS = 2;

    private HiveLeaderOrganClientInput() {
    }

    /**
     *  tick
     *
     * <p> {@code consumeClick()}  {@code isDown()}""
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
        if (HiveLeaderOrganKeys.OPEN_ORGANS.consumeClick()) {
            ClientPacketDistributor.sendToServer(new RequestOpenHiveLeaderOrgansPacket());
        }
        //  consumeClick  false
        if (HiveLeaderOrganKeys.TELEPORT.consumeClick()) {
            if (teleportUnlockedLocally()) {
                ClientPacketDistributor.sendToServer(new HiveLeaderOrganTeleportPacket());
            }
        }
        //
        // <b></b>
        //  HiveLeaderDecomposeParasiteHandler
        // ""
        // ""BiomassClientData  isHiveLeader
        //  0 consumeClick()
        if (HiveLeaderOrganKeys.DECOMPOSE_PARASITE.consumeClick()) {
            ClientPacketDistributor.sendToServer(new HiveLeaderDecomposeParasitePacket());
        }
    }

    /**
     *  33  2
     *
     * <p> {@code SyncHiveLeaderOrgansPacket} GUI
     *  {@link HiveLeaderOrganClientData#data()}
     *  false
     * ""</p>
     */
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

