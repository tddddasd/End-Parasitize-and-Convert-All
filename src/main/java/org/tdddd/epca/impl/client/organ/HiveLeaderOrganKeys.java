package org.tdddd.epca.impl.client.organ;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class HiveLeaderOrganKeys {

    public static final String CATEGORY = "key.categories.epca";

    public static final KeyMapping OPEN_ORGANS = new KeyMapping(
            "key.epca.open_organs",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY);

    public static final KeyMapping TELEPORT = new KeyMapping(
            "key.epca.organ_teleport",
            KeyConflictContext.IN_GAME,

            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    public static final KeyMapping DECOMPOSE_PARASITE = new KeyMapping(
            "key.epca.decompose_parasite",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    /**
     * 切换「显示 / 隐藏内器官模型」（默认未绑定、默认关闭）。
     * 状态由服务端持久化并广播，见 {@code ToggleOrganModelPacket}。
     */
    public static final KeyMapping TOGGLE_ORGAN_MODEL = new KeyMapping(
            "key.epca.toggle_organ_model",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    /**
     * 初探种主动技能：消耗 15 生物质把准星指向的裂兽变成虫巢节点（默认未绑定）。
     *
     * <p>语义上就是需求里的「V 键」，但为了不与已有的硬编码 V 键（{@code KeyPacket} /
     * {@code VKeyStatePacket}）冲突，这里做成独立的可绑定按键、默认不占用任何键。</p>
     */
    public static final KeyMapping SPECIES_CONVERT_RIPPER = new KeyMapping(
            "key.epca.species_convert_ripper",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    /** 重塑种主动技能：消耗 3 生物质 → 15 秒力量 I / 迅捷 I / 饱和 I（默认未绑定）。 */
    public static final KeyMapping SPECIES_RESHAPE_BUFF = new KeyMapping(
            "key.epca.species_reshape_buff",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    private HiveLeaderOrganKeys() {
    }
}
