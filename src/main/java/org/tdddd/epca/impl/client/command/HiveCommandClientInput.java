package org.tdddd.epca.impl.client.command;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.c2s.HiveCommandActionPacket;

import javax.annotation.Nullable;

/**
 * 客户端按键轮询：把三个指挥按键翻译成 C2S 请求（服务端校验领巢者身份后才执行）。
 *
 * <ul>
 *     <li>打开菜单：只发 {@code OPEN}，服务端回同步包并附“打开界面”标记；</li>
 *     <li>加路线节点：算好准星落点后发 {@code ROUTE_ADD_POINT}。<b>不经过菜单</b>，
 *     所以玩家可以边走边按，第二个、第三个点自然不会是同一个方块；</li>
 *     <li>结束并下达：发 {@code ROUTE_BIND_EDITING}，把当前编辑路线绑给选中的寄生体。</li>
 * </ul>
 *
 * <p>有界面打开时一律不处理（避免和输入框/菜单点击抢键）。</p>
 *
 * <p>26.1.2 差异：{@code TickEvent.ClientTickEvent(phase == END)} → {@link ClientTickEvent.Post}。</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class HiveCommandClientInput {

    /** 准星取点的最大射线长度（格）；服务端还会用 ROUTE_RADIUS 再校验一次。 */
    private static final double AIM_DISTANCE = 64.0D;

    private static ClientLevel lastLevel;

    private HiveCommandClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != lastLevel) {
            // 换世界/换维度后旧实体 id 全部失效：清空等待服务端下一次同步
            lastLevel = minecraft.level;
            HiveCommandClientData.clear();
        }
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        if (HiveCommandKeys.OPEN_MENU.consumeClick()) {
            ModNetwork.sendToServer(HiveCommandActionPacket.open());
        }
        if (HiveCommandKeys.ADD_ROUTE_POINT.consumeClick()) {
            // 空 routeId = “加到当前编辑路线”，服务端没有编辑路线时会自动新建一条。
            ModNetwork.sendToServer(HiveCommandActionPacket.routeAddPoint("", aimPoint()));
        }
        if (HiveCommandKeys.FINISH_ROUTE.consumeClick()) {
            ModNetwork.sendToServer(HiveCommandActionPacket.routeBindEditing());
        }
    }

    /**
     * 准星落点：命中方块时取“方块上方”（朝上的面）或该方块本身；没命中任何方块时退回玩家脚下。
     *
     * <p>直接现算射线，而不是读 {@code minecraft.hitResult}：后者每帧只在游戏内更新，
     * 界面打开期间是冻结的旧值，这正是“在菜单里连点两次却总是同一个点”的根源。</p>
     */
    @Nullable
    public static BlockPos aimPoint() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(AIM_DISTANCE));
        BlockHitResult hit = minecraft.level.clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            return hit.getDirection() == Direction.UP ? pos.above() : pos;
        }
        return player.blockPosition();
    }
}
