package org.tdddd.epca.impl.client.command;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;
import org.tdddd.epca.impl.events.ParasiteEvents;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.c2s.HiveCommandActionPacket;
import org.tdddd.epca.impl.overworld.data.command.HiveCommandHighlight;
import org.tdddd.epca.impl.overworld.data.command.HiveCommandMode;
import org.tdddd.epca.impl.overworld.data.command.HiveRoute;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 「寄生体指挥菜单」：领巢者用。
 *
 * <p>结构（左：寄生体列表 + 路线库；右：模式/目标/编辑中路线/下达）：</p>
 * <ul>
 *     <li><b>附近寄生体列表</b>：32 格内按距离排序，点击切换选中（多选）；
 *     另有“准星选择”按钮直接把准星指向的寄生体加入/移出选中名单。</li>
 *     <li><b>路线库</b>：列出服务端保存的所有路线（名称/节点数/绑定数/是否闭环），点击切换
 *     “正在编辑”的路线；可新建 / 重命名 / 删除。路线库是服务端权威数据，这里只发请求、只显示回传。</li>
 *     <li><b>编辑中路线</b>：显示节点列表；「添加当前点」「删除末点」「清空路线」「首尾相连」。
 *     节点只增不减（除显式删除末点/清空），不会被同步包冲掉——因为节点本来就在服务端。</li>
 *     <li><b>模式 / 攻击目标 / 下达指令</b>：下达时把<b>选中的那条路线</b>绑给选中的寄生体；
 *     同一条路线可以给多只寄生体共用，不同寄生体也可以各绑一条。</li>
 * </ul>
 *
 * <p>界面只发 C2S 请求、只读服务端同步回来的状态；选中列表、模式、目标、路线库都由服务端裁决。</p>
 *
 * <p>加节点的两条路：菜单里的按钮（准星冻结时会提示“与上一点太近”），
 * 或者不打开菜单直接按 {@link HiveCommandKeys#ADD_ROUTE_POINT}（推荐：可以边走边按）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class HiveCommandScreen extends Screen {

    private static final int PANEL_W = 460;
    private static final int PANEL_H = 300;

    // 左列
    private static final int LIST_X = 12;
    private static final int LIST_W = 200;
    private static final int NEARBY_ROWS = 6;
    private static final int ROW_HEIGHT = 16;
    private static final int NEARBY_TOP = 42;
    private static final int ROUTE_LIB_TOP = 178;
    private static final int ROUTE_LIB_ROWS = 4;

    // 右列
    private static final int RX = 234;
    private static final int RW = 214;

    private static final int PANEL_FILL = 0xE0100618;
    private static final int PANEL_BORDER = 0xFF5A2A7A;
    private static final int COLOR_TITLE = 0xFFE9C7FF;
    private static final int COLOR_SECTION = 0xFFCC66FF;
    private static final int COLOR_TEXT = 0xFFE0D0F0;
    private static final int COLOR_HINT = 0xFF9A8AA8;
    private static final int COLOR_SELECTED = 0xFF55FF7A;
    private static final int COLOR_EDITING = 0xFFD39BFF;
    private static final int COLOR_WARN = 0xFFFFAA55;
    private static final int COLOR_OK = 0xFF7CFFB0;
    private static final int COLOR_BUTTON = 0xFF2A1236;
    private static final int COLOR_BUTTON_HOVER = 0xFF3E1B52;
    private static final int COLOR_BUTTON_ACTIVE = 0xFF5A2A7A;
    private static final int COLOR_BUTTON_OFF = 0xFF1E0A2A;
    private static final int COLOR_BUTTON_BORDER = 0xFF6A3A8A;
    private static final int COLOR_BUTTON_BORDER_ACTIVE = 0xFFB060E0;
    private static final int COLOR_BUTTON_TEXT_OFF = 0xFF7A6A88;

    private static final int MESSAGE_TICKS = 90;
    /** 与服务端 HiveCommandManager.MAX_ROUTES 保持一致（客户端不引用服务端类）。 */
    private static final int MAX_ROUTES = 32;

    private final List<PaintButton> buttons = new ArrayList<>();
    private final List<Mob> nearby = new ArrayList<>();

    private int left;
    private int top;
    private int routeScroll;
    private int draftMode = HiveCommandMode.FOLLOW.ordinal();
    private int draftTargetId = HiveCommandHighlight.NO_ENTITY;
    private boolean draftInitialized;

    private boolean renaming;
    @Nullable
    private EditBox nameBox;

    @Nullable
    private Component message;
    private boolean messageWarning;
    private int messageTicks;

    public HiveCommandScreen() {
        super(Component.translatable("epca.hive_command_gui.title"));
    }

    @Override
    protected void init() {
        super.init();
        left = Math.max(2, (width - PANEL_W) / 2);
        top = Math.max(2, (height - PANEL_H) / 2);
        // 尺寸变化会重建控件：直接取消重命名，避免留下孤立的输入框状态
        renaming = false;
        nameBox = null;
        refreshNearby();
        if (!draftInitialized) {
            initDraftFromServer();
        }
        buildButtons();
        syncRouteScroll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        refreshNearby();
        if (messageTicks > 0 && --messageTicks == 0) {
            message = null;
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, width, height, 0x99000000);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        if (!draftInitialized) {
            initDraftFromServer();
        }
        syncRouteScroll();
        drawPanel(guiGraphics);
        drawHeader(guiGraphics);
        drawNearbyList(guiGraphics, mouseX, mouseY);
        drawRouteLibrary(guiGraphics, mouseX, mouseY);
        drawRightColumn(guiGraphics, mouseX, mouseY);
        if (renaming) {
            drawRenameOverlay(guiGraphics);
        }
        for (PaintButton button : buttons) {
            button.render(guiGraphics, mouseX, mouseY);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (PaintButton widget : buttons) {
                if (widget.clicked(mouseX, mouseY)) {
                    widget.action.run();
                    return true;
                }
            }
            if (!renaming) {
                int routeIndex = routeRowAt(mouseX, mouseY);
                if (routeIndex >= 0) {
                    HiveRoute route = routeAt(routeIndex);
                    if (route != null) {
                        ModNetwork.sendToServer(HiveCommandActionPacket.routeSelect(route.id()));
                        return true;
                    }
                }
                Mob row = nearbyAt(mouseX, mouseY);
                if (row != null) {
                    ModNetwork.sendToServer(HiveCommandActionPacket.select(row.getId()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (isInside(mouseX, mouseY, left + LIST_X, top + ROUTE_LIB_TOP - 2,
                LIST_W, ROUTE_LIB_ROWS * ROW_HEIGHT + 4)) {
            int max = Math.max(0, HiveCommandClientData.routes().size() - ROUTE_LIB_ROWS);
            int next = Math.max(0, Math.min(max, routeScroll + (scrollY > 0 ? -1 : 1)));
            if (next != routeScroll) {
                routeScroll = next;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (renaming) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                finishRename(true);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                finishRename(false);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ------------------------------------------------------------------
    // 布局 / 绘制
    // ------------------------------------------------------------------

    private void buildButtons() {
        buttons.clear();
        int rx = left + RX;
        int lx = left + LIST_X;

        // 模式：三个互斥按钮
        for (int i = 0; i < HiveCommandMode.VALUES.length; i++) {
            final int mode = i;
            buttons.add(new PaintButton(rx + i * 70, top + 40, 66, 18,
                    () -> Component.translatable(modeKey(mode)),
                    () -> true,
                    () -> draftMode == mode,
                    () -> draftMode = mode));
        }

        // 攻击目标
        buttons.add(new PaintButton(rx, top + 76, 104, 18,
                () -> Component.translatable("epca.hive_command_gui.target_crosshair"),
                () -> true, () -> draftTargetId >= 0, this::pickTargetCrosshair));
        buttons.add(new PaintButton(rx + 110, top + 76, 100, 18,
                () -> Component.translatable("epca.hive_command_gui.target_clear"),
                () -> draftTargetId >= 0, () -> false, () -> draftTargetId = HiveCommandHighlight.NO_ENTITY));

        // 编辑中路线：节点操作
        buttons.add(new PaintButton(rx, top + 110, 104, 18,
                () -> Component.translatable("epca.hive_command_gui.route.add"),
                () -> true, () -> false, this::addRoutePoint));
        buttons.add(new PaintButton(rx + 110, top + 110, 100, 18,
                () -> Component.translatable("epca.hive_command_gui.route.remove"),
                () -> editingRoute() != null && !editingRoute().points().isEmpty(),
                () -> false, this::removeRoutePoint));
        buttons.add(new PaintButton(rx, top + 132, 104, 18,
                () -> Component.translatable("epca.hive_command_gui.route.clear"),
                () -> editingRoute() != null && !editingRoute().points().isEmpty(),
                () -> false, this::clearRoute));
        buttons.add(new PaintButton(rx + 110, top + 132, 100, 18,
                () -> Component.translatable(isEditingClosed()
                        ? "epca.hive_command_gui.route.closed_on" : "epca.hive_command_gui.route.closed_off"),
                () -> editingRoute() != null && editingRoute().points().size() > 2,
                this::isEditingClosed, this::toggleClosed));

        // 下达 / 取消
        buttons.add(new PaintButton(rx, top + 240, 104, 22,
                () -> Component.translatable("epca.hive_command_gui.apply"),
                this::canApply, () -> false, this::sendApply));
        buttons.add(new PaintButton(rx + 110, top + 240, 100, 22,
                () -> Component.translatable("epca.hive_command_gui.cancel"),
                () -> !selfSelected().isEmpty(), () -> false, this::sendClearCommand));

        // 附近寄生体
        buttons.add(new PaintButton(lx, top + 140, 96, 16,
                () -> Component.translatable("epca.hive_command_gui.select_crosshair"),
                () -> true, () -> false, this::pickParasiteCrosshair));
        buttons.add(new PaintButton(lx + 104, top + 140, 96, 16,
                () -> Component.translatable("epca.hive_command_gui.clear_selection"),
                () -> !selfSelected().isEmpty(), () -> false, this::clearSelection));

        // 路线库
        buttons.add(new PaintButton(lx, top + 243, 62, 16,
                () -> Component.translatable("epca.hive_command_gui.route.new"),
                () -> true, () -> false, this::createRoute));
        buttons.add(new PaintButton(lx + 66, top + 243, 62, 16,
                () -> Component.translatable("epca.hive_command_gui.route.rename"),
                () -> editingRoute() != null, () -> false, this::startRename));
        buttons.add(new PaintButton(lx + 132, top + 243, 62, 16,
                () -> Component.translatable("epca.hive_command_gui.route.delete"),
                () -> editingRoute() != null, () -> false, this::deleteRoute));

        // 重命名浮层
        if (renaming) {
            buttons.add(new PaintButton(left + 136, top + 192, 86, 18,
                    () -> Component.translatable("epca.hive_command_gui.rename.confirm"),
                    () -> true, () -> false, () -> finishRename(true)));
            buttons.add(new PaintButton(left + 226, top + 192, 86, 18,
                    () -> Component.translatable("epca.hive_command_gui.rename.cancel"),
                    () -> true, () -> false, () -> finishRename(false)));
        }
    }

    private void drawPanel(GuiGraphics guiGraphics) {
        guiGraphics.fill(left, top, left + PANEL_W, top + PANEL_H, PANEL_FILL);
        guiGraphics.fill(left, top, left + PANEL_W, top + 1, PANEL_BORDER);
        guiGraphics.fill(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, PANEL_BORDER);
        guiGraphics.fill(left, top, left + 1, top + PANEL_H, PANEL_BORDER);
        guiGraphics.fill(left + PANEL_W - 1, top, left + PANEL_W, top + PANEL_H, PANEL_BORDER);
        // 中缝 + 左列两条分隔线
        guiGraphics.fill(left + 222, top + 26, left + 223, top + PANEL_H - 6, 0x805A2A7A);
        guiGraphics.fill(left + LIST_X, top + 160, left + LIST_X + LIST_W, top + 161, 0x505A2A7A);
    }

    private void drawHeader(GuiGraphics guiGraphics) {
        Component title = Component.translatable("epca.hive_command_gui.title");
        guiGraphics.drawString(font, title, left + (PANEL_W - font.width(title)) / 2, top + 12,
                COLOR_TITLE, false);
        guiGraphics.drawString(font, Component.translatable("epca.hive_command_gui.nearby"),
                left + LIST_X, top + 28, COLOR_SECTION, false);
        Component count = Component.translatable("epca.hive_command_gui.selected_count", selfSelected().size());
        guiGraphics.drawString(font, count, left + LIST_X + LIST_W - font.width(count),
                top + 28, COLOR_HINT, false);
    }

    private void drawNearbyList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        UUID self = selfId();
        for (int i = 0; i < nearby.size() && i < NEARBY_ROWS; i++) {
            Mob mob = nearby.get(i);
            int rowY = top + NEARBY_TOP + i * ROW_HEIGHT;
            boolean selected = HiveCommandClientData.isSelectedBy(self, mob.getUUID());
            boolean hovered = isInside(mouseX, mouseY, left + LIST_X, rowY - 2, LIST_W, ROW_HEIGHT - 1);
            if (selected) {
                guiGraphics.fill(left + LIST_X, rowY - 2, left + LIST_X + LIST_W, rowY + 12, 0x503E1B52);
            } else if (hovered) {
                guiGraphics.fill(left + LIST_X, rowY - 2, left + LIST_X + LIST_W, rowY + 12, 0x20FFFFFF);
            }
            String name = trimToWidth(mob.getDisplayName().getString(), 130);
            guiGraphics.drawString(font, name, left + LIST_X + 4, rowY,
                    selected ? COLOR_SELECTED : COLOR_TEXT, false);
            String distance = String.format("%.1f", minecraft != null && minecraft.player != null
                    ? minecraft.player.distanceTo(mob) : 0.0F);
            guiGraphics.drawString(font, distance,
                    left + LIST_X + LIST_W - font.width(distance) - 4, rowY, COLOR_HINT, false);
        }
        if (nearby.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("epca.hive_command_gui.none"),
                    left + LIST_X + 4, top + NEARBY_TOP, COLOR_HINT, false);
        }
    }

    private void drawRouteLibrary(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<HiveRoute> routes = HiveCommandClientData.routes();
        Component label = Component.translatable("epca.hive_command_gui.routes", routes.size());
        guiGraphics.drawString(font, label, left + LIST_X, top + 166, COLOR_SECTION, false);
        if (routes.size() > ROUTE_LIB_ROWS) {
            Component scroll = Component.translatable("epca.hive_command_gui.route.scroll");
            guiGraphics.drawString(font, scroll, left + LIST_X + LIST_W - font.width(scroll),
                    top + 166, COLOR_HINT, false);
        }
        if (routes.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("epca.hive_command_gui.route.empty"),
                    left + LIST_X + 2, top + ROUTE_LIB_TOP, COLOR_HINT, false);
            return;
        }
        String editingId = editingRouteId();
        for (int row = 0; row < ROUTE_LIB_ROWS; row++) {
            int index = routeScroll + row;
            if (index >= routes.size()) {
                break;
            }
            HiveRoute route = routes.get(index);
            int rowY = top + ROUTE_LIB_TOP + row * ROW_HEIGHT;
            boolean editing = route.id().equals(editingId);
            boolean hovered = isInside(mouseX, mouseY, left + LIST_X, rowY - 2, LIST_W, ROW_HEIGHT - 1);
            if (editing) {
                guiGraphics.fill(left + LIST_X, rowY - 2, left + LIST_X + LIST_W, rowY + 12, 0x604A1E6E);
            } else if (hovered) {
                guiGraphics.fill(left + LIST_X, rowY - 2, left + LIST_X + LIST_W, rowY + 12, 0x20FFFFFF);
            }
            String name = routeLabel(route, index);
            String closedTag = route.closed()
                    ? Component.translatable("epca.hive_command_gui.route.closed_tag").getString() : "";
            Component line = Component.translatable("epca.hive_command_gui.route.row",
                    editing ? "> " : "  ", trimToWidth(name, 84), route.size(),
                    HiveCommandClientData.bindCount(route.id()), closedTag);
            guiGraphics.drawString(font, trimToWidth(line.getString(), LIST_W - 4), left + LIST_X + 2, rowY,
                    editing ? COLOR_EDITING : COLOR_TEXT, false);
        }
    }

    private void drawRightColumn(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int rx = left + RX;
        guiGraphics.drawString(font, Component.translatable("epca.hive_command_gui.mode"), rx, top + 28,
                COLOR_SECTION, false);

        guiGraphics.drawString(font, Component.translatable("epca.hive_command_gui.target"), rx, top + 64,
                COLOR_SECTION, false);
        Component target = targetLabel();
        guiGraphics.drawString(font, target, rx + 50, top + 64,
                draftTargetId >= 0 ? COLOR_TEXT : COLOR_HINT, false);

        HiveRoute editing = editingRoute();
        Component routeLabel = Component.translatable("epca.hive_command_gui.route",
                editing == null ? 0 : editing.points().size());
        guiGraphics.drawString(font, routeLabel, rx, top + 96, COLOR_SECTION, false);
        if (editing != null) {
            String name = routeLabel(editing, Math.max(0, indexOfRoute(editing.id())));
            Component editingLabel = Component.translatable("epca.hive_command_gui.route.editing",
                    trimToWidth(name, 90));
            guiGraphics.drawString(font, editingLabel,
                    rx + RW - font.width(editingLabel), top + 96, COLOR_EDITING, false);
        } else {
            Component none = Component.translatable("epca.hive_command_gui.route.none_selected");
            guiGraphics.drawString(font, none, rx + RW - font.width(none), top + 96, COLOR_HINT, false);
        }

        drawPointList(guiGraphics, editing);

        if (message != null && messageTicks > 0) {
            guiGraphics.drawString(font, message, rx, top + 200,
                    messageWarning ? COLOR_WARN : COLOR_OK, false);
        }

        if (editing == null) {
            guiGraphics.drawString(font, Component.translatable("epca.hive_command_gui.hint.route_missing"),
                    rx, top + 212, COLOR_WARN, false);
        } else if (draftMode == HiveCommandMode.PATROL.ordinal() && editing.points().size() < 2) {
            guiGraphics.drawString(font, Component.translatable("epca.hive_command_gui.hint.route_short"),
                    rx, top + 212, COLOR_WARN, false);
        }

        Component hint = Component.translatable("epca.hive_command_gui.hint");
        guiGraphics.drawWordWrap(font, hint, rx, top + 266, RW, COLOR_HINT);
    }

    /** 编辑中路线的节点坐标列表：两列、最多 8 个，超出部分显示剩余数量。 */
    private void drawPointList(GuiGraphics guiGraphics, @Nullable HiveRoute route) {
        int rx = left + RX;
        if (route == null || route.points().isEmpty()) {
            return;
        }
        List<BlockPos> points = route.points();
        int shown = Math.min(points.size(), 8);
        boolean truncated = points.size() > shown;
        if (truncated) {
            shown = 7;
        }
        for (int i = 0; i < shown; i++) {
            BlockPos pos = points.get(i);
            int column = i % 2;
            int row = i / 2;
            String text = "#" + (i + 1) + " " + pos.getX() + "," + pos.getY() + "," + pos.getZ();
            guiGraphics.drawString(font, text, rx + column * 108, top + 156 + row * 10, COLOR_TEXT, false);
        }
        if (truncated) {
            Component more = Component.translatable("epca.hive_command_gui.route.more",
                    points.size() - shown);
            guiGraphics.drawString(font, more, rx + 108, top + 156 + 3 * 10, COLOR_HINT, false);
        }
    }

    private void drawRenameOverlay(GuiGraphics guiGraphics) {
        int x = left + 126;
        int y = top + 150;
        guiGraphics.fill(x, y, x + 208, y + 70, 0xF0140620);
        guiGraphics.fill(x, y, x + 208, y + 1, COLOR_BUTTON_BORDER_ACTIVE);
        guiGraphics.fill(x, y + 69, x + 208, y + 70, COLOR_BUTTON_BORDER_ACTIVE);
        guiGraphics.fill(x, y, x + 1, y + 70, COLOR_BUTTON_BORDER_ACTIVE);
        guiGraphics.fill(x + 207, y, x + 208, y + 70, COLOR_BUTTON_BORDER_ACTIVE);
        guiGraphics.drawString(font, Component.translatable("epca.hive_command_gui.rename.title"),
                x + 10, y + 8, COLOR_TITLE, false);
        // 输入框本身由 super.render() 统一绘制（它是通过 addRenderableWidget 注册的）
    }

    // ------------------------------------------------------------------
    // 交互：路线库
    // ------------------------------------------------------------------

    private void createRoute() {
        if (HiveCommandClientData.routes().size() >= MAX_ROUTES) {
            postMessage(true, Component.translatable("epca.hive_command_gui.route.limit", MAX_ROUTES));
            return;
        }
        ModNetwork.sendToServer(HiveCommandActionPacket.routeCreate());
        // 新路线追加在末尾：把列表滚到能看见它
        routeScroll = Math.max(0, HiveCommandClientData.routes().size() + 1 - ROUTE_LIB_ROWS);
        postMessage(false, Component.translatable("epca.hive_command_gui.route.created"));
    }

    private void startRename() {
        HiveRoute route = editingRoute();
        if (route == null) {
            return;
        }
        renaming = true;
        nameBox = new EditBox(font, left + 136, top + 168, 184, 18,
                Component.translatable("epca.hive_command_gui.rename.title"));
        nameBox.setMaxLength(HiveRoute.MAX_NAME_LENGTH);
        nameBox.setValue(route.name());
        nameBox.setFocused(true);
        addRenderableWidget(nameBox);
        buildButtons();
    }

    private void finishRename(boolean apply) {
        String value = nameBox == null ? "" : nameBox.getValue().trim();
        String routeId = editingRouteId();
        renaming = false;
        if (nameBox != null) {
            removeWidget(nameBox);
            nameBox = null;
            setFocused(null);
        }
        buildButtons();
        if (apply && routeId != null) {
            ModNetwork.sendToServer(HiveCommandActionPacket.routeRename(routeId, value));
            postMessage(false, Component.translatable("epca.hive_command_gui.route.renamed"));
        }
    }

    private void deleteRoute() {
        String routeId = editingRouteId();
        if (routeId == null) {
            return;
        }
        ModNetwork.sendToServer(HiveCommandActionPacket.routeDelete(routeId));
        postMessage(false, Component.translatable("epca.hive_command_gui.route.deleted"));
    }

    // ------------------------------------------------------------------
    // 交互：编辑中的路线节点
    // ------------------------------------------------------------------

    private void addRoutePoint() {
        BlockPos pos = HiveCommandClientInput.aimPoint();
        if (pos == null) {
            return;
        }
        HiveRoute route = editingRoute();
        if (route != null) {
            if (route.points().size() >= HiveRoute.MAX_POINTS) {
                postMessage(true, Component.translatable("epca.hive_command_gui.route.point_full",
                        HiveRoute.MAX_POINTS));
                return;
            }
            double distance = route.distanceToLast(pos);
            if (distance < HiveRoute.MIN_POINT_GAP) {
                // 关键修复：不再“静默忽略”，而是明确告诉玩家为什么加不上
                postMessage(true, Component.translatable("epca.hive_command_gui.route.too_close",
                        String.format("%.1f", distance)));
                return;
            }
        }
        String routeId = route == null ? "" : route.id();
        ModNetwork.sendToServer(HiveCommandActionPacket.routeAddPoint(routeId, pos));
    }

    private void removeRoutePoint() {
        String routeId = editingRouteId();
        if (routeId == null) {
            return;
        }
        ModNetwork.sendToServer(HiveCommandActionPacket.routeRemoveLast(routeId));
    }

    private void clearRoute() {
        String routeId = editingRouteId();
        if (routeId == null) {
            return;
        }
        ModNetwork.sendToServer(HiveCommandActionPacket.routeClear(routeId));
    }

    private void toggleClosed() {
        String routeId = editingRouteId();
        if (routeId == null) {
            return;
        }
        ModNetwork.sendToServer(HiveCommandActionPacket.routeToggleClosed(routeId));
    }

    // ------------------------------------------------------------------
    // 交互：寄生体
    // ------------------------------------------------------------------

    private void pickParasiteCrosshair() {
        Minecraft minecraft = Minecraft.getInstance();
        HitResult hit = minecraft.hitResult;
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof Mob mob
                && ParasiteEvents.isParasiteEntity(mob)) {
            ModNetwork.sendToServer(HiveCommandActionPacket.select(mob.getId()));
        }
    }

    private void pickTargetCrosshair() {
        Minecraft minecraft = Minecraft.getInstance();
        HitResult hit = minecraft.hitResult;
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living
                && minecraft.player != null && living != minecraft.player
                && !ParasiteEvents.isParasiteEntity(living)) {
            draftTargetId = living.getId();
        }
    }

    private void clearSelection() {
        ModNetwork.sendToServer(HiveCommandActionPacket.clearSelection());
    }

    private boolean canApply() {
        if (selfSelected().isEmpty()) {
            return false;
        }
        if (draftMode != HiveCommandMode.PATROL.ordinal()) {
            return true;
        }
        HiveRoute route = editingRoute();
        return route != null && route.points().size() >= 2;
    }

    private void sendApply() {
        if (!canApply()) {
            return;
        }
        String routeId = editingRouteId();
        ModNetwork.sendToServer(HiveCommandActionPacket.apply(draftMode, draftTargetId,
                routeId == null ? "" : routeId));
    }

    private void sendClearCommand() {
        ModNetwork.sendToServer(HiveCommandActionPacket.clearCommand());
    }

    // ------------------------------------------------------------------
    // 数据
    // ------------------------------------------------------------------

    private void refreshNearby() {
        nearby.clear();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        List<Mob> found = minecraft.level.getEntitiesOfClass(Mob.class,
                minecraft.player.getBoundingBox().inflate(32.0D),
                mob -> mob.isAlive() && ParasiteEvents.isParasiteEntity(mob));
        found.sort(Comparator.comparingDouble(mob -> minecraft.player.distanceToSqr(mob)));
        for (int i = 0; i < found.size() && i < NEARBY_ROWS; i++) {
            nearby.add(found.get(i));
        }
    }

    private void initDraftFromServer() {
        UUID self = selfId();
        if (self == null) {
            return;
        }
        for (HiveCommandHighlight highlight : HiveCommandClientData.entries()) {
            if (!self.equals(highlight.owner()) || !highlight.hasCommand()) {
                continue;
            }
            draftMode = highlight.mode();
            draftTargetId = highlight.targetId();
            break;
        }
        draftInitialized = true;
    }

    private void syncRouteScroll() {
        List<HiveRoute> routes = HiveCommandClientData.routes();
        int max = Math.max(0, routes.size() - ROUTE_LIB_ROWS);
        routeScroll = Math.max(0, Math.min(routeScroll, max));
        String editingId = editingRouteId();
        if (editingId == null) {
            return;
        }
        int index = indexOfRoute(editingId);
        if (index < 0) {
            return;
        }
        if (index < routeScroll) {
            routeScroll = index;
        } else if (index >= routeScroll + ROUTE_LIB_ROWS) {
            routeScroll = Math.min(index - ROUTE_LIB_ROWS + 1, max);
        }
    }

    @Nullable
    private HiveRoute editingRoute() {
        return HiveCommandClientData.editingRoute(selfId());
    }

    @Nullable
    private String editingRouteId() {
        return HiveCommandClientData.editingRouteId(selfId());
    }

    private boolean isEditingClosed() {
        HiveRoute route = editingRoute();
        return route != null && route.closed();
    }

    private int indexOfRoute(String routeId) {
        List<HiveRoute> routes = HiveCommandClientData.routes();
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).id().equals(routeId)) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    private HiveRoute routeAt(int index) {
        List<HiveRoute> routes = HiveCommandClientData.routes();
        return index >= 0 && index < routes.size() ? routes.get(index) : null;
    }

    /** 鼠标在路线库列表里的第几行（没命中返回 -1）。 */
    private int routeRowAt(double mouseX, double mouseY) {
        for (int row = 0; row < ROUTE_LIB_ROWS; row++) {
            int rowY = top + ROUTE_LIB_TOP + row * ROW_HEIGHT;
            if (isInside(mouseX, mouseY, left + LIST_X, rowY - 2, LIST_W, ROW_HEIGHT - 1)) {
                return routeScroll + row;
            }
        }
        return -1;
    }

    private String routeLabel(HiveRoute route, int index) {
        if (!route.name().isEmpty()) {
            return route.name();
        }
        return Component.translatable("epca.hive_command_gui.route.default_name", index + 1).getString();
    }

    private Component targetLabel() {
        if (draftTargetId < 0 || minecraft == null || minecraft.level == null) {
            return Component.translatable("epca.hive_command_gui.none");
        }
        Entity entity = minecraft.level.getEntity(draftTargetId);
        if (entity == null) {
            return Component.translatable("epca.hive_command_gui.none");
        }
        return Component.literal(trimToWidth(entity.getDisplayName().getString(), 110));
    }

    private void postMessage(boolean warning, Component text) {
        message = text;
        messageWarning = warning;
        messageTicks = MESSAGE_TICKS;
    }

    private UUID selfId() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null : minecraft.player.getUUID();
    }

    private List<HiveCommandHighlight> selfSelected() {
        UUID self = selfId();
        List<HiveCommandHighlight> out = new ArrayList<>();
        if (self == null) {
            return out;
        }
        for (HiveCommandHighlight entry : HiveCommandClientData.entries()) {
            if (self.equals(entry.owner()) && entry.selected()) {
                out.add(entry);
            }
        }
        return out;
    }

    private Mob nearbyAt(double mouseX, double mouseY) {
        for (int i = 0; i < nearby.size() && i < NEARBY_ROWS; i++) {
            int rowY = top + NEARBY_TOP + i * ROW_HEIGHT;
            if (isInside(mouseX, mouseY, left + LIST_X, rowY - 2, LIST_W, ROW_HEIGHT - 1)) {
                return nearby.get(i);
            }
        }
        return null;
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String trimmed = text;
        while (trimmed.length() > 1 && font.width(trimmed + "...") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "...";
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static String modeKey(int mode) {
        return switch (mode) {
            case 1 -> "epca.hive_command_gui.mode.guard";
            case 2 -> "epca.hive_command_gui.mode.patrol";
            default -> "epca.hive_command_gui.mode.follow";
        };
    }

    /** 菜单只知道“服务端上限”，避免在客户端引用服务端管理器。 */
    // ------------------------------------------------------------------
    // 自绘按钮
    // ------------------------------------------------------------------

    private final class PaintButton {
        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private final Supplier<Component> label;
        private final Supplier<Boolean> enabled;
        private final Supplier<Boolean> active;
        private final Runnable action;

        private PaintButton(int x, int y, int w, int h, Supplier<Component> label,
                            Supplier<Boolean> enabled, Supplier<Boolean> active, Runnable action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.label = label;
            this.enabled = enabled;
            this.active = active;
            this.action = action;
        }

        private void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            boolean on = enabled.get();
            boolean hovered = on && isInside(mouseX, mouseY, x, y, w, h);
            boolean lit = active.get();
            int fill = !on ? COLOR_BUTTON_OFF
                    : lit ? COLOR_BUTTON_ACTIVE
                    : hovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON;
            int border = !on ? 0xFF4A2A5A : lit ? COLOR_BUTTON_BORDER_ACTIVE : COLOR_BUTTON_BORDER;
            guiGraphics.fill(x, y, x + w, y + h, fill);
            guiGraphics.fill(x, y, x + w, y + 1, border);
            guiGraphics.fill(x, y + h - 1, x + w, y + h, border);
            guiGraphics.fill(x, y, x + 1, y + h, border);
            guiGraphics.fill(x + w - 1, y, x + w, y + h, border);
            Component text = label.get();
            int color = on ? (lit ? COLOR_TITLE : COLOR_TEXT) : COLOR_BUTTON_TEXT_OFF;
            guiGraphics.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, color);
        }

        private boolean clicked(double mouseX, double mouseY) {
            return enabled.get() && isInside(mouseX, mouseY, x, y, w, h);
        }
    }
}
