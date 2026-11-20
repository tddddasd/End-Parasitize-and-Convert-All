package org.tdddd.epca.impl.client.organ.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 头部 / 躯干内器官窗口的「挖洞」状态。
 *
 * <p>窗口要向模型内部凹 3 像素（3 个模型单位），所以第一层皮肤的正面那一块必须
 * <b>不参与渲染</b>——否则它会把深处的深红底面挡住。做法：</p>
 * <ol>
 *   <li>{@code ModelFeatureRendererOrganFaceMixin} 在 {@code ModelFeatureRenderer#renderModel}
 *       头尾记录「当前正在铺缓冲的 ModelSubmit 的渲染态 + 模型」（26 的渲染全部走
 *       SubmitNodeCollector 延迟提交，所以必须挂在真正写顶点的那一层，而不是实体 submit 那一层）；</li>
 *   <li>{@code ModelPartOrganFaceMixin} 在 {@code ModelPart#render} 的头尾记录"当前正在渲染的人体部件"；</li>
 *   <li>{@code ModelPartCubeOrganFaceMixin} 在 {@code ModelPart.Cube#compile} 里发现"当前部件是头/躯干的
 *       基础皮肤层、且该玩家打开了内器官显示"时，跳过正面那个四边形，其余五个面照原样输出。</li>
 * </ol>
 *
 * <p>只有"有内器官且开关打开"的玩家才会被挖洞；其余玩家、以及所有非头部/躯干部件完全走原版逻辑。
 * 所有相关 mixin 都用 {@code require = 0}：如果 26 的某个目标方法签名再变，就不会注入，
 * 结果只是"窗口不挖洞"（功能降级，不会崩游戏）。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class OrganFaceMask {

    private static final ThreadLocal<AvatarRenderState> STATE = new ThreadLocal<>();
    private static final ThreadLocal<PlayerModel> MODEL = new ThreadLocal<>();
    private static final ThreadLocal<ModelPart> PART = new ThreadLocal<>();

    private OrganFaceMask() {
    }

    public static void beginRender(@Nullable Object state, @Nullable Object model) {
        if (state instanceof AvatarRenderState avatarState && model instanceof PlayerModel playerModel) {
            STATE.set(avatarState);
            MODEL.set(playerModel);
            PART.remove();
            return;
        }
        // 非玩家模型：只有确实残留过玩家状态时才需要清理，避免每个模型每帧做 ThreadLocal 操作。
        if (STATE.get() != null) {
            clear();
        }
    }

    public static void endRender() {
        if (STATE.get() != null) {
            clear();
        }
    }

    private static void clear() {
        STATE.remove();
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
        AvatarRenderState state = STATE.get();
        PlayerModel model = MODEL.get();
        ModelPart part = PART.get();
        if (state == null || model == null || part == null) return false;
        if (state.isInvisible) return false;

        UUID playerId = playerId(state);
        if (playerId == null) return false;
        if (part == model.head) return OrganModelClientData.showHead(playerId);
        if (part == model.body) return OrganModelClientData.showTorso(playerId);
        return false;
    }

    /**
     * 当前部件的正面所在 z（模型单位）：头部立方体 z 为 -4..4，躯干 z 为 -2..2。
     */
    public static float frontPlaneZ() {
        PlayerModel model = MODEL.get();
        ModelPart part = PART.get();
        if (model != null && part == model.body) return -2.0F;
        return -4.0F;
    }

    /** 渲染态 → 玩家 UUID（26 的渲染态只带实体 id）。 */
    @Nullable
    public static UUID playerId(@Nullable AvatarRenderState state) {
        if (state == null) return null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        Entity entity = minecraft.level.getEntity(state.id);
        return entity instanceof Player player ? player.getUUID() : null;
    }
}
