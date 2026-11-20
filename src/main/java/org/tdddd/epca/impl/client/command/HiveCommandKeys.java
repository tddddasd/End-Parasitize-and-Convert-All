package org.tdddd.epca.impl.client.command;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import org.tdddd.epca.impl.client.organ.HiveLeaderOrganKeys;

/**
 * 寄生体指挥系统的按键（领巢者用）。
 *
 * <p>三个键默认都未绑定，玩家在“选项 - 控制 - 按键绑定 - E-PCA”里自行绑定；
 * 注册在 {@code ClientSetup.onRegisterKeyMappings}，轮询在 {@link HiveCommandClientInput}。</p>
 *
 * <ul>
 *     <li>{@link #OPEN_MENU}：打开指挥菜单（含路线库管理）；</li>
 *     <li>{@link #ADD_ROUTE_POINT}：<b>不打开菜单</b>直接在准星位置给“当前编辑路线”加一个节点。
 *     第一次按时会自动新建一条路线；可以和走路配合，一边走一边加点；</li>
 *     <li>{@link #FINISH_ROUTE}：结束编辑并下达 —— 把当前编辑路线绑给选中的寄生体（巡逻模式），
 *     各自原有的攻击目标保留。</li>
 * </ul>
 *
 * <p>26.1.2 差异：{@code String category} → {@code KeyMapping.Category}（{@code KeyMapping.Category.register(Identifier)}）。
 * 这里复用 {@link HiveLeaderOrganKeys#CATEGORY}（{@code epca:keys}），保证所有 epca 按键都在同一个
 * “E-PCA”类别里，而不是分裂成两个同名类别；其翻译键由 {@code Category#label()} 生成为
 * {@code key.category.epca.keys}（lang 里同时保留了 1.20.1 的 {@code key.categories.epca} 别名）。
 * 三个键默认 {@code GLFW_KEY_UNKNOWN}（未绑定），与 1.20.1 一致。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class HiveCommandKeys {

    public static final KeyMapping.Category CATEGORY = HiveLeaderOrganKeys.CATEGORY;

    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.epca.open_hive_command",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    /** 准星处加一个路线节点（默认未绑定）。 */
    public static final KeyMapping ADD_ROUTE_POINT = new KeyMapping(
            "key.epca.hive_add_route_point",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    /** 结束编辑并下达：当前编辑路线 -> 选中的寄生体（默认未绑定）。 */
    public static final KeyMapping FINISH_ROUTE = new KeyMapping(
            "key.epca.hive_finish_route",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    private HiveCommandKeys() {
    }
}
