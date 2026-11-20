package org.tdddd.epca.impl.client.organ.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * 头部 / 躯干内器官窗口的「挖洞」状态。
 *
 * <p>窗口要向模型内部凹 3 像素（3 个模型单位），所以第一层皮肤的正面那一块必须
 * <b>不参与渲染</b>——否则它会把深处的深红底面挡住。做法（侵入性最小）：</p>
 * <ol>
 *   <li>{@code LivingEntityRendererOrganFaceMixin} 在渲染玩家时把当前实体 + PlayerModel 记到 ThreadLocal；</li>
 *   <li>{@code ModelPartOrganFaceMixin} 在 ModelPart.render 的头尾记录"当前正在渲染的人体部件"；</li>
 *   <li>{@code ModelPartCubeOrganFaceMixin} 在 ModelPart.Cube.compile 里发现"当前部件是头/躯干的
 *       基础皮肤层、且该玩家打开了内器官显示"时，跳过正面那个四边形，其余五个面照原样输出。</li>
 * </ol>
 *
 * <p>只有"有内器官且开关打开"的玩家才会被挖洞；其余玩家、以及所有非头部/躯干部件完全走原版逻辑。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class OrganFaceMask {

    private static final ThreadLocal<AbstractClientPlayer> PLAYER = new ThreadLocal<>();
    private static final ThreadLocal<PlayerModel<?>> MODEL = new ThreadLocal<>();
    private static final ThreadLocal<ModelPart> PART = new ThreadLocal<>();

    private OrganFaceMask() {
    }

    public static void beginRender(@Nullable LivingEntity entity, @Nullable Object model) {
        if (entity instanceof AbstractClientPlayer player && model instanceof PlayerModel<?> playerModel) {
            PLAYER.set(player);
            MODEL.set(playerModel);
            PART.remove();
            return;
        }
        // 非玩家实体：只有确实残留过玩家状态时才需要清理，避免每个生物每帧做 ThreadLocal 操作。
        if (PLAYER.get() != null) {
            clear();
        }
    }

    public static void endRender() {
        if (PLAYER.get() != null) {
            clear();
        }
    }

    private static void clear() {
        PLAYER.remove();
        MODEL.remove();
        PART.remove();
    }

    public static void pushPart(@Nullable ModelPart part) {
        PART.set(part);
    }

    public static void popPart() {
        PART.remove();
    }

    /** 当前正在渲染的基础皮肤部件是否需要挖掉正面。 */
    public static boolean shouldHideFront() {
        AbstractClientPlayer player = PLAYER.get();
        PlayerModel<?> model = MODEL.get();
        ModelPart part = PART.get();
        if (player == null || model == null || part == null || player.isInvisible()) return false;
        if (part == model.head) return OrganModelClientData.showHead(player);
        if (part == model.body) return OrganModelClientData.showTorso(player);
        return false;
    }

    /**
     * 当前部件的正面所在 z（模型单位）：头部立方体 z 为 -4..4，躯干 z 为 -2..2。
     */
    public static float frontPlaneZ() {
        PlayerModel<?> model = MODEL.get();
        ModelPart part = PART.get();
        if (model != null && part == model.body) return -2.0F;
        return -4.0F;
    }
}
