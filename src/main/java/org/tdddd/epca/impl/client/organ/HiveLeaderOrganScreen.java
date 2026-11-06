package org.tdddd.epca.impl.client.organ;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.c2s.HiveLeaderOrganActionPacket;
import org.tdddd.epca.impl.overworld.data.BiomassClientData;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganGate;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotKind;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatReadout;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatReadout.ReadoutLabels;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class HiveLeaderOrganScreen extends Screen {

    private static final int PANEL_WIDTH = 600;

    private static final int PANEL_HEIGHT = 430;

    private static final int PANEL_COLOR = 0xE0100618;

    private static final int PANEL_BORDER_COLOR = 0xFF5A2A7A;

    private static final int DESIGN_WIDTH = PANEL_WIDTH;

    private static final int DESIGN_HEIGHT = PANEL_HEIGHT;

    private static final int VIEWPORT_MARGIN = 6;

    private static final float LAYOUT_SCALE_MIN = 0.40F;

    private static final int COMPACT_HEAD_INNER_DROP = 4;

    private static final int COMPACT_READOUT_INSET = 8;

    public static final int COLOR_STATS = 0xFFCC66FF;

    public static final int COLOR_DAMAGE_INFO = 0xFF55FF7A;
    private static final int COLOR_TITLE = 0xFFE9C7FF;
    private static final int COLOR_HINT = 0xFF9A8AA8;
    private static final int COLOR_WARN = 0xFFFFAA55;
    private static final int COLOR_DIVIDER = 0x66FF5555;

    private static final int READOUT_FADE = 0xCC100618;

    private static final int COLOR_GROUP_LABEL = 0x66E0B0FF;

    private static final int SLOT_LOCKED_FILL = 0xFF1E0A2A;

    private static final int SLOT_LOCKED_BORDER = 0xFF4A2A5A;

    private static final int SLOT_PURPLE_FILL = 0xFF3E1B52;

    private static final int SLOT_PURPLE_BORDER = 0xFFB060E0;

    private static final int SLOT_RED_FILL = 0xFF4A1420;

    private static final int SLOT_RED_BORDER = 0xFFE04040;

    private static final int SLOT_GATED_OVERLAY = 0x99000000;

    private static final int SLOT_HOVER_OVERLAY = 0x40FFFFFF;

    private static final int LOCK_COLOR = 0xFFD8C0F0;

    private static final int LOCK_SHADOW = 0xFF7A5A90;

    private static final int SLOT_INVENTORY_FILL = 0xFF20182C;

    private static final int SLOT_INVENTORY_BORDER = 0xFF6A5A80;

    private static final String KEY_TITLE = "epca.organ_gui.title";

    private static final String KEY_HINT_DRAG = "epca.organ_gui.hint.drag";

    private static final String KEY_HINT_QUICK_MOVE = "epca.organ_gui.hint.quick_move";

    private static final String KEY_HINT_TRANSFER = "epca.organ_gui.hint.transfer";

    private static final String KEY_HINT_CLOSE = "epca.organ_gui.hint.close";

    private static final String KEY_HINT_SEPARATOR = "epca.organ_gui.hint.separator";

    private static final String KEY_WAITING = "epca.organ_gui.waiting_data";

    private static final String KEY_INVENTORY = "epca.organ_gui.inventory";

    private static final String KEY_HOTBAR = "epca.organ_gui.hotbar";

    private static final String KEY_NO_MODEL = "epca.organ_gui.model.none";

    private static final String KEY_GROUP_HEAD = "epca.organ_gui.group.head";

    private static final String KEY_GROUP_TORSO = "epca.organ_gui.group.torso";

    private static final String KEY_GROUP_LEFT_ARM = "epca.organ_gui.group.left_arm";

    private static final String KEY_GROUP_RIGHT_ARM = "epca.organ_gui.group.right_arm";

    private static final String KEY_GROUP_LEFT_LEG = "epca.organ_gui.group.left_leg";

    private static final String KEY_GROUP_RIGHT_LEG = "epca.organ_gui.group.right_leg";

    private static final String KEY_GROUP_TORSO_INNER = "epca.organ_gui.group.torso_inner";

    private static final String KEY_GROUP_HEAD_INNER = "epca.organ_gui.group.head_inner";

    private static final String KEY_READOUT_ADAPT_CHANCE = "epca.organ_gui.readout.adaptation_chance";

    private static final String KEY_READOUT_ADAPT_REDUCTION = "epca.organ_gui.readout.adaptation_reduction";

    private static final String KEY_READOUT_PERCENT = "epca.organ_gui.readout.percent";

    private static final String KEY_SOURCE_PURPLE = "epca.organ_gui.source.purple";

    private static final String KEY_SOURCE_TORSO_INNER = "epca.organ_gui.source.torso_inner";

    private static final String KEY_SOURCE_HEAD_INNER = "epca.organ_gui.source.head_inner";

    private static final String KEY_ATTRIBUTE_PREFIX = "epca.organ_gui.attribute.";

    private static final String KEY_BIOMASS = "epca.organ_gui.biomass";

    private static final Map<String, Component> ATTRIBUTE_LABELS = new ConcurrentHashMap<>();

    private static final int SLOT_SIZE = 12;

    private static final float ITEM_RENDER_SCALE = Math.min(1.0F, SLOT_SIZE / 16.0F);

    private static final int SLOT_STRIDE = 12;

    private static final int TORSO_INNER_STRIDE = 12;

    private static final int INVENTORY_STRIDE = 12;

    private static final int MODEL_CENTER_X = 300;

    private static final int MODEL_CENTER_Y = 214;

    private static final int TORSO_INNER_LEFT = MODEL_CENTER_X - (8 * TORSO_INNER_STRIDE + SLOT_SIZE) / 2;

    private static final int TORSO_INNER_TOP = 96;

    private static final int HEAD_INNER_LEFT = MODEL_CENTER_X - (2 * TORSO_INNER_STRIDE + SLOT_SIZE) / 2;

    private static final int HEAD_INNER_TOP = 48;

    private static final int PURPLE_ARM_LEFT_X = 132;

    private static final int PURPLE_ARM_RIGHT_X = 444;

    private static final int PURPLE_CENTER_LEFT_X = 276;

    private static final int PURPLE_CENTER_RIGHT_X = 300;

    private static final int PURPLE_HEAD_TOP = 148;

    private static final int PURPLE_ARM_TOP = 192;

    private static final int PURPLE_TORSO_TOP = 262;

    private static final int PURPLE_LEG_TOP = 286;

    private static final int INVENTORY_LEFT = 480;

    private static final int INVENTORY_TOP = 228;

    private static final int HOTBAR_TOP = INVENTORY_TOP + 3 * INVENTORY_STRIDE + 14;

    private static final int READOUT_TOP_OFFSET = 308;

    private static final int READOUT_LINE_HEIGHT = 10;

    private static final int STATS_MAX_LINES = 12;

    private static final int READOUT_LEFT_INSET = 6;

    private static final int READOUT_GREEN_X = 384;

    private static final int READOUT_GREEN_TOP = 308;

    private static final int BIOMASS_LEFT = READOUT_LEFT_INSET;

    private static final int BIOMASS_TOP = 8;

    private static final int BIOMASS_LINE_HEIGHT = READOUT_LINE_HEIGHT;

    private static final int COLOR_BIOMASS = COLOR_STATS;

    private static final int MODEL_SIZE = 44;

    private static final float PLAYER_MODEL_SCALE = 0.9375F;

    private static final double MODEL_ANCHOR_OFFSET_PER_SIZE =
            (ModelBody.MODEL_LOCAL_HALF_HEIGHT / (double) ModelBody.MODEL_LOCAL_HEIGHT)
                    * (ModelBody.MODEL_LOCAL_HEIGHT / (double) ModelBody.MODEL_UNITS_PER_BLOCK)
                    * PLAYER_MODEL_SCALE;

    private final HiveLeaderOrganModelRotation rotation = new HiveLeaderOrganModelRotation();
    private boolean draggingModel;
    private double dragAnchorX;
    private double dragAnchorY;

    private final int[][] slotGeometry = new int[OrganSlotGroup.totalSlots()][2];

    private final int[][] inventoryGeometry =
            new int[HiveLeaderOrganActionPacket.INVENTORY_SLOT_COUNT][2];

    @Nullable
    private LivingEntity hostEntity;

    private float layoutScale = 1.0F;

    private int panelLeft;

    private int panelTop;

    private int modelSize = MODEL_SIZE;

    private boolean compactLayout;

    private Rect statsBlock = new Rect(READOUT_LEFT_INSET, READOUT_TOP_OFFSET, DESIGN_WIDTH, DESIGN_HEIGHT);

    private Rect damageBlock = new Rect(READOUT_GREEN_X, READOUT_GREEN_TOP, DESIGN_WIDTH, DESIGN_HEIGHT);

    private double layoutMouseX;

    private double layoutMouseY;

    public HiveLeaderOrganScreen() {

        super(Component.translatable(KEY_TITLE));
    }

    protected String hintText() {
        String separator = Component.translatable(KEY_HINT_SEPARATOR).getString();
        return Component.translatable(KEY_HINT_DRAG).getString()
                + separator + Component.translatable(KEY_HINT_QUICK_MOVE).getString()
                + separator + Component.translatable(KEY_HINT_TRANSFER).getString()
                + separator + Component.translatable(KEY_HINT_CLOSE).getString();
    }

    @Override
    protected void init() {
        super.init();
        this.hostEntity = this.minecraft == null ? null : this.minecraft.player;

        computeResponsiveLayout();
        computeSlotGeometry();
        computeInventoryGeometry();
    }

    protected ModelBody modelBody() {
        return ModelBody.of(MODEL_CENTER_X, MODEL_CENTER_Y, modelSize());
    }

    protected int modelSize() {
        return this.modelSize;
    }

    protected record ModelBody(Rect head, Rect torso, Rect leftArm, Rect rightArm,
                               Rect leftLeg, Rect rightLeg, int feetY, int topY) {

        static final int MODEL_LOCAL_HEIGHT = 32;

        static final int MODEL_LOCAL_HALF_HEIGHT = MODEL_LOCAL_HEIGHT / 2;

        static final int MODEL_UNITS_PER_BLOCK = 16;

        static ModelBody of(int centerX, int centerY, int size) {
            double k = size * PLAYER_MODEL_SCALE / MODEL_UNITS_PER_BLOCK;
            int halfHeight = (int) Math.round(MODEL_LOCAL_HALF_HEIGHT * k);
            int topY = centerY - halfHeight;
            int feetY = centerY + halfHeight;

            int bodyHalf = (int) Math.round(4.0D * k);
            int shoulderHalf = (int) Math.round(8.0D * k);
            int limbHalf = (int) Math.round(2.0D * k);

            int torsoTop = topY + (int) Math.round(8.0D * k);
            int armTop = torsoTop - (int) Math.round(2.0D * k);
            int armBottom = topY + (int) Math.round(18.0D * k);
            int legTop = topY + (int) Math.round(20.0D * k);
            int legBottom = feetY;

            Rect head = new Rect(centerX - bodyHalf, topY, centerX + bodyHalf, torsoTop);
            Rect torso = new Rect(centerX - bodyHalf, torsoTop, centerX + bodyHalf, legTop);
            Rect leftArm = new Rect(centerX - shoulderHalf, armTop,
                    centerX - shoulderHalf + 2 * limbHalf, armBottom);
            Rect rightArm = new Rect(centerX + shoulderHalf - 2 * limbHalf, armTop,
                    centerX + shoulderHalf, armBottom);
            Rect leftLeg = new Rect(centerX - bodyHalf, legTop, centerX, legBottom);
            Rect rightLeg = new Rect(centerX, legTop, centerX + bodyHalf, legBottom);
            return new ModelBody(head, torso, leftArm, rightArm, leftLeg, rightLeg, feetY, topY);
        }
    }

    protected record Rect(int left, int top, int right, int bottom) {
        int width() {
            return right - left;
        }

        int height() {
            return bottom - top;
        }

        int centerX() {
            return (left + right) / 2;
        }

        int centerY() {
            return (top + bottom) / 2;
        }
    }

    private void computeSlotGeometry() {

        placeRow(OrganSlotGroup.HEAD, PURPLE_HEAD_TOP, PURPLE_CENTER_LEFT_X, PURPLE_CENTER_RIGHT_X);

        placeRow(OrganSlotGroup.TORSO, PURPLE_TORSO_TOP, PURPLE_CENTER_LEFT_X, PURPLE_CENTER_RIGHT_X);

        placeColumn(OrganSlotGroup.LEFT_ARM, PURPLE_ARM_LEFT_X, PURPLE_ARM_TOP);
        placeColumn(OrganSlotGroup.RIGHT_ARM, PURPLE_ARM_RIGHT_X, PURPLE_ARM_TOP);

        placeColumn(OrganSlotGroup.LEFT_LEG, PURPLE_ARM_LEFT_X, PURPLE_LEG_TOP);
        placeColumn(OrganSlotGroup.RIGHT_LEG, PURPLE_ARM_RIGHT_X, PURPLE_LEG_TOP);

        for (int local = 0; local < OrganSlotGroup.TORSO_INNER.size(); local++) {
            int global = OrganSlotGroup.TORSO_INNER.globalIndex(local);
            int row = local / OrganSlotGroup.TORSO_INNER_COLUMNS;
            int column = local % OrganSlotGroup.TORSO_INNER_COLUMNS;
            slotGeometry[global][0] = TORSO_INNER_LEFT + column * TORSO_INNER_STRIDE;
            slotGeometry[global][1] = TORSO_INNER_TOP + row * TORSO_INNER_STRIDE;
        }

        int headInnerTop = headInnerTop();
        for (int local = 0; local < OrganSlotGroup.HEAD_INNER.size(); local++) {
            int global = OrganSlotGroup.HEAD_INNER.globalIndex(local);
            int row = local / OrganSlotGroup.HEAD_INNER_COLUMNS;
            int column = local % OrganSlotGroup.HEAD_INNER_COLUMNS;
            slotGeometry[global][0] = HEAD_INNER_LEFT + column * TORSO_INNER_STRIDE;
            slotGeometry[global][1] = headInnerTop + row * TORSO_INNER_STRIDE;
        }
    }

    protected int headInnerTop() {
        return HEAD_INNER_TOP + (this.compactLayout ? COMPACT_HEAD_INNER_DROP : 0);
    }

    private void placeRow(OrganSlotGroup group, int top, int leftX, int rightX) {
        for (int local = 0; local < group.size(); local++) {
            slotGeometry[group.globalIndex(local)][0] = (local % 2 == 0) ? leftX : rightX;
            slotGeometry[group.globalIndex(local)][1] = top;
        }
    }

    private void placeColumn(OrganSlotGroup group, int leftX, int top) {
        for (int local = 0; local < group.size(); local++) {
            slotGeometry[group.globalIndex(local)][0] = leftX;
            slotGeometry[group.globalIndex(local)][1] = top + local * SLOT_STRIDE;
        }
    }

    private void computeInventoryGeometry() {
        for (int index = 0; index < inventoryGeometry.length; index++) {
            int slotColumn = index % 9;
            int slotRow = index / 9;
            inventoryGeometry[index][0] = INVENTORY_LEFT + slotColumn * INVENTORY_STRIDE;
            inventoryGeometry[index][1] = (slotRow == 0)
                    ? HOTBAR_TOP
                    : INVENTORY_TOP + (slotRow - 1) * INVENTORY_STRIDE;
        }
    }

    private void computeResponsiveLayout() {
        int availableWidth = usableWidth();
        int availableHeight = usableHeight();

        double naturalScale = Math.min(1.0D, Math.min(
                availableWidth / (double) DESIGN_WIDTH,
                availableHeight / (double) DESIGN_HEIGHT));
        this.layoutScale = (float) Math.max(LAYOUT_SCALE_MIN, naturalScale);

        int panelWidth = Math.round(DESIGN_WIDTH * this.layoutScale);
        int panelHeight = Math.round(DESIGN_HEIGHT * this.layoutScale);
        this.panelLeft = Math.round((this.width - panelWidth) / 2.0F);
        this.panelTop = Math.round((this.height - panelHeight) / 2.0F);

        boolean floored = naturalScale < LAYOUT_SCALE_MIN;

        this.modelSize = MODEL_SIZE;

        int panelWidthInWindow = Math.min(panelWidth, this.width);
        int panelViewportRight = this.panelLeft + panelWidthInWindow;
        int panelHeightInWindow = Math.min(panelHeight, this.height);
        int panelViewportBottom = this.panelTop + panelHeightInWindow;
        int statsMaxWidth = READOUT_GREEN_X - READOUT_LEFT_INSET;
        int statsTop = READOUT_TOP_OFFSET;

        boolean readoutsCollide = (READOUT_TOP_OFFSET + READOUT_LINE_HEIGHT) * this.layoutScale
                > panelHeightInWindow;
        this.compactLayout = floored && readoutsCollide;
        int designRightLimit = Math.max(0, Math.min(DESIGN_WIDTH, panelViewportRight - this.panelLeft));
        if (this.compactLayout) {

            int left = INVENTORY_LEFT + COMPACT_READOUT_INSET;
            int right = Math.min(DESIGN_WIDTH - COMPACT_READOUT_INSET, designRightLimit);
            statsTop = HOTBAR_TOP + SLOT_SIZE + 4;
            statsMaxWidth = Math.max(40, right - left);
            this.statsBlock = new Rect(left, statsTop, right, panelViewportBottom - this.panelTop);
        } else {
            this.statsBlock = new Rect(READOUT_LEFT_INSET, statsTop,
                    Math.min(READOUT_LEFT_INSET + statsMaxWidth, designRightLimit),
                    panelViewportBottom - this.panelTop);
        }
        this.damageBlock = readoutDamageBlock(designRightLimit, panelViewportBottom);
    }

    private Rect readoutDamageBlock(int designRightLimit, int panelViewportBottom) {
        if (!this.compactLayout) {
            return new Rect(READOUT_GREEN_X, READOUT_GREEN_TOP,
                    designRightLimit, panelViewportBottom - this.panelTop);
        }
        int right = Math.max(this.statsBlock.left() + 40, designRightLimit);
        return new Rect(this.statsBlock.left(), this.statsBlock.bottom() + 8, right,
                panelViewportBottom - this.panelTop);
    }

    protected int usableWidth() {
        return Math.max(1, this.width - VIEWPORT_MARGIN * 2);
    }

    protected int usableHeight() {
        return Math.max(1, this.height - VIEWPORT_MARGIN * 2);
    }

    protected double designToScreenX(double designX) {
        return this.panelLeft + designX * this.layoutScale;
    }

    protected double designToScreenY(double designY) {
        return this.panelTop + designY * this.layoutScale;
    }

    protected double toLayoutX(double screenX) {
        return (screenX - this.panelLeft) / this.layoutScale;
    }

    protected double toLayoutY(double screenY) {
        return (screenY - this.panelTop) / this.layoutScale;
    }

    protected double layoutMouseX() {
        return this.layoutMouseX;
    }

    protected double layoutMouseY() {
        return this.layoutMouseY;
    }

    protected int panelLeft() {
        return this.panelLeft;
    }

    protected int panelTop() {
        return this.panelTop;
    }

    protected int panelScreenWidth() {
        return Math.round(DESIGN_WIDTH * this.layoutScale);
    }

    protected int panelScreenHeight() {
        return Math.round(DESIGN_HEIGHT * this.layoutScale);
    }

    protected float layoutScale() {
        return this.layoutScale;
    }

    protected boolean compactLayout() {
        return this.compactLayout;
    }

    protected int modelCenterY() {
        return MODEL_CENTER_Y;
    }

    protected int modelScreenAnchorY() {
        return MODEL_CENTER_Y + (int) Math.round(MODEL_ANCHOR_OFFSET_PER_SIZE * this.modelSize);
    }

    protected Rect statsBlock() {
        return this.statsBlock;
    }

    protected Rect damageBlock() {
        return this.damageBlock;
    }

    protected int readoutTop() {
        return this.statsBlock.top();
    }

    protected int readoutDamageTop() {
        return this.damageBlock.top();
    }

    protected int readoutLineLimit() {
        if (statsBlock.right() <= statsBlock.left()) return 0;
        int bottom = this.statsBlock.bottom() - this.statsBlock.top();
        if (bottom <= 0) return 0;
        return Math.max(0, Math.min(STATS_MAX_LINES, bottom / READOUT_LINE_HEIGHT));
    }

    protected int damageLineLimit() {
        if (damageBlock.right() <= damageBlock.left()) return 0;
        int available = damageBlock.bottom() - damageBlock.top();
        if (available <= 0) return 0;
        return Math.max(0, Math.min(OrganStatReadout.DAMAGE_LINE_COUNT,
                available / READOUT_LINE_HEIGHT));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        this.rotation.update(deltaTick());

        this.layoutMouseX = toLayoutX(mouseX);
        this.layoutMouseY = toLayoutY(mouseY);

        this.renderBackground(graphics);

        int panelX = this.panelLeft;
        int panelY = this.panelTop;
        int panelW = panelScreenWidth();
        int panelH = panelScreenHeight();

        int clipLeft = Math.max(0, panelX);
        int clipTop = Math.max(0, panelY);
        int clipRight = Math.min(this.width - 1, panelX + panelW);
        int clipBottom = Math.min(this.height - 1, panelY + panelH);
        boolean scissor = clipRight > clipLeft && clipBottom > clipTop;
        if (scissor) {
            graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        }

        graphics.pose().pushPose();
        graphics.pose().translate(panelX, panelY, 0.0F);
        graphics.pose().scale(this.layoutScale, this.layoutScale, 1.0F);

        graphics.fill(0, 0, DESIGN_WIDTH, DESIGN_HEIGHT, PANEL_COLOR);
        graphics.renderOutline(0, 0, DESIGN_WIDTH, DESIGN_HEIGHT, PANEL_BORDER_COLOR);

        graphics.drawCenteredString(this.font, this.title,
                DESIGN_WIDTH / 2, 8, COLOR_TITLE);
        renderBiomassReadout(graphics);
        graphics.drawCenteredString(this.font, hintText(),
                DESIGN_WIDTH / 2, 20, COLOR_HINT);
        if (!HiveLeaderOrganClientData.hasData()) {
            graphics.drawCenteredString(this.font, Component.translatable(KEY_WAITING),
                    DESIGN_WIDTH / 2, 32, COLOR_WARN);
        }

        renderPlayerModel(graphics);
        renderSlots(graphics, (int) Math.floor(this.layoutMouseX), (int) Math.floor(this.layoutMouseY));
        renderInventory(graphics, (int) Math.floor(this.layoutMouseX), (int) Math.floor(this.layoutMouseY));
        renderReadouts(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.pose().popPose();
        if (scissor) {
            graphics.disableScissor();
        }

        renderSlotTooltip(graphics, mouseX, mouseY);
        renderCarriedStack(graphics, mouseX, mouseY);
    }

    private float deltaTick() {
        Minecraft minecraft = this.minecraft;
        return minecraft == null ? 1.0F : minecraft.getDeltaFrameTime();
    }

    protected void renderSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        HiveLeaderOrganData data = HiveLeaderOrganClientData.data();
        HiveLeaderOrganGate.Gates gates = HiveLeaderOrganClientData.gates();

        for (OrganSlotGroup group : OrganSlotGroup.ALL) {
            for (int local = 0; local < group.size(); local++) {
                int index = group.globalIndex(local);

                int x = slotGeometry[index][0];
                int y = slotGeometry[index][1];
                boolean locked = group.kind().needsUnlock() && !data.isUnlocked(index);
                boolean gated = group.kind().isInnerGrid() && !gates.openFor(group);
                boolean hovered = isInside(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE);

                renderSlotBackground(graphics, x, y, group.kind(), locked, gated, hovered);

                ItemStack stack = data.getItem(index);
                if (!stack.isEmpty()) {
                    renderItemInCell(graphics, stack, x, y);
                }
                if (locked) {
                    renderLockIcon(graphics, x, y);
                }
            }
        }

        int dividerY = headInnerTop() - 4;
        graphics.fill(HEAD_INNER_LEFT, dividerY,
                HEAD_INNER_LEFT + OrganSlotGroup.HEAD_INNER.columns() * TORSO_INNER_STRIDE,
                dividerY + 1, COLOR_DIVIDER);

        renderGroupLabel(graphics, OrganSlotGroup.HEAD, KEY_GROUP_HEAD);
        renderGroupLabel(graphics, OrganSlotGroup.TORSO, KEY_GROUP_TORSO);
        renderGroupLabel(graphics, OrganSlotGroup.LEFT_ARM, KEY_GROUP_LEFT_ARM);
        renderGroupLabel(graphics, OrganSlotGroup.RIGHT_ARM, KEY_GROUP_RIGHT_ARM);
        renderGroupLabel(graphics, OrganSlotGroup.LEFT_LEG, KEY_GROUP_LEFT_LEG);
        renderGroupLabel(graphics, OrganSlotGroup.RIGHT_LEG, KEY_GROUP_RIGHT_LEG);

        renderRegionLabelLeft(graphics, slotGroupBounds(OrganSlotGroup.HEAD_INNER),
                KEY_GROUP_HEAD_INNER);
        renderRegionLabelLeft(graphics, slotGroupBounds(OrganSlotGroup.TORSO_INNER),
                KEY_GROUP_TORSO_INNER);
    }

    @Nullable
    private Rect slotGroupBounds(OrganSlotGroup group) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int local = 0; local < group.size(); local++) {
            int[] geometry = slotGeometry[group.globalIndex(local)];
            minX = Math.min(minX, geometry[0]);
            maxX = Math.max(maxX, geometry[0] + SLOT_SIZE);
            minY = Math.min(minY, geometry[1]);
            maxY = Math.max(maxY, geometry[1] + SLOT_SIZE);
        }
        if (minX == Integer.MAX_VALUE) return null;
        return new Rect(minX, minY, maxX, maxY);
    }

    private void renderGroupLabel(GuiGraphics graphics, OrganSlotGroup group, String langKey) {
        Rect bounds = slotGroupBounds(group);
        if (bounds == null) return;
        Component label = Component.translatable(langKey);
        int width = this.font.width(label);
        boolean leftSide = bounds.centerX() < DESIGN_WIDTH / 2;
        int labelX = leftSide
                ? Math.max(6, bounds.left() - 4 - width / 2)
                : Math.min(DESIGN_WIDTH - 6, bounds.right() + 4 + width / 2);
        int labelY = bounds.centerY() - 4;
        drawLabel(graphics, label, labelX, labelY);
    }

    private void renderRegionLabelLeft(GuiGraphics graphics, @Nullable Rect bounds, String langKey) {
        if (bounds == null) return;
        Component label = Component.translatable(langKey);
        drawLabel(graphics, label, 6 + this.font.width(label) / 2, bounds.centerY() - 4);
    }

    private void drawLabel(GuiGraphics graphics, Component label, int centeredX, int y) {
        int width = this.font.width(label);
        if (rectsOverlapPx(centeredX - width / 2, y, width, this.font.lineHeight, this.statsBlock)
                || rectsOverlapPx(centeredX - width / 2, y, width, this.font.lineHeight,
                        this.damageBlock)) {
            return;
        }
        graphics.drawCenteredString(this.font, label, centeredX, y, COLOR_GROUP_LABEL);
    }

    private void renderSlotBackground(GuiGraphics graphics, int x, int y, OrganSlotKind kind,
                                      boolean locked, boolean gated, boolean hovered) {
        int fill;
        int border;
        if (kind.needsUnlock()) {
            fill = locked ? SLOT_LOCKED_FILL : SLOT_PURPLE_FILL;
            border = locked ? SLOT_LOCKED_BORDER : SLOT_PURPLE_BORDER;
        } else {
            fill = SLOT_RED_FILL;
            border = SLOT_RED_BORDER;
        }
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, fill);
        graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, border);
        if (gated) {
            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_GATED_OVERLAY);
        }
        if (hovered) {
            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_HOVER_OVERLAY);
        }
    }

    protected void renderItemInCell(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(ITEM_RENDER_SCALE, ITEM_RENDER_SCALE, 1.0F);

        graphics.renderItem(stack, 0, 0);
        graphics.renderItemDecorations(this.font, stack, 0, 0);
        graphics.pose().popPose();
    }

    protected void renderInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        Inventory inventory = clientInventory();
        graphics.drawString(this.font, Component.translatable(KEY_INVENTORY), INVENTORY_LEFT,
                INVENTORY_TOP - 12, COLOR_HINT, false);
        graphics.drawString(this.font, Component.translatable(KEY_HOTBAR), INVENTORY_LEFT,
                HOTBAR_TOP - 10, COLOR_HINT, false);

        for (int index = 0; index < inventoryGeometry.length; index++) {
            int x = inventoryGeometry[index][0];
            int y = inventoryGeometry[index][1];
            boolean hovered = isInside(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE);
            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_INVENTORY_FILL);
            graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, SLOT_INVENTORY_BORDER);
            if (hovered) {
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_HOVER_OVERLAY);
            }
            if (inventory == null) continue;
            ItemStack stack = inventoryItem(inventory, index);
            if (!stack.isEmpty()) {
                renderItemInCell(graphics, stack, x, y);
            }
        }
    }

    @Nullable
    protected Inventory clientInventory() {
        AbstractClientPlayer player = localPlayer();
        return player == null ? null : player.getInventory();
    }

    private static ItemStack inventoryItem(Inventory inventory, int index) {
        if (index < 0 || index >= inventory.items.size()) return ItemStack.EMPTY;
        return inventory.items.get(index);
    }

    private void renderLockIcon(GuiGraphics graphics, int slotX, int slotY) {
        int baseX = slotX + 2;
        int baseY = slotY + 2;
        graphics.fill(baseX + 1, baseY, baseX + 2, baseY + 3, LOCK_COLOR);
        graphics.fill(baseX + 6, baseY, baseX + 7, baseY + 3, LOCK_COLOR);
        graphics.fill(baseX + 2, baseY, baseX + 6, baseY + 1, LOCK_COLOR);
        graphics.fill(baseX + 1, baseY, baseX + 2, baseY + 1, LOCK_SHADOW);
        graphics.fill(baseX, baseY + 3, baseX + 8, baseY + 9, LOCK_SHADOW);
        graphics.fill(baseX, baseY + 3, baseX + 7, baseY + 8, LOCK_COLOR);
        graphics.fill(baseX + 3, baseY + 5, baseX + 5, baseY + 7, LOCK_SHADOW);
    }

    private void renderSlotTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!HiveLeaderOrganClientData.carried().isEmpty()) return;
        int layoutX = (int) Math.floor(this.layoutMouseX);
        int layoutY = (int) Math.floor(this.layoutMouseY);
        int hovered = slotAt(layoutX, layoutY);
        if (hovered >= 0) {
            ItemStack stack = HiveLeaderOrganClientData.data().getItem(hovered);
            if (!stack.isEmpty()) {
                graphics.renderTooltip(this.font, stack, mouseX, mouseY);
                return;
            }
        }
        int inventorySlot = inventorySlotAt(layoutX, layoutY);
        if (inventorySlot < 0) return;
        Inventory inventory = clientInventory();
        if (inventory == null) return;
        ItemStack stack = inventoryItem(inventory, inventorySlot);
        if (stack.isEmpty()) return;
        graphics.renderTooltip(this.font, stack, mouseX, mouseY);
    }

    protected void renderCarriedStack(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack carried = HiveLeaderOrganClientData.carried();
        if (carried.isEmpty()) return;
        int x = mouseX - 8;
        int y = mouseY - 8;
        graphics.pose().pushPose();

        graphics.pose().translate(0.0F, 0.0F, 232.0F);
        graphics.renderItem(carried, x, y);
        graphics.renderItemDecorations(this.font, carried, x, y);
        graphics.pose().popPose();
    }

    protected void renderBiomassReadout(GuiGraphics graphics) {
        Component label = Component.translatable(KEY_BIOMASS, BiomassClientData.getPoints());
        int width = this.font.width(label);
        if (rectsOverlapPx(BIOMASS_LEFT, BIOMASS_TOP, width, BIOMASS_LINE_HEIGHT, this.statsBlock)
                || rectsOverlapPx(BIOMASS_LEFT, BIOMASS_TOP, width, BIOMASS_LINE_HEIGHT,
                        this.damageBlock)) {
            return;
        }

        int limit = Math.min(DESIGN_WIDTH - READOUT_LEFT_INSET,
                DESIGN_WIDTH / 2 - this.font.width(this.title) / 2);
        if (BIOMASS_LEFT + width > limit) {
            return;
        }
        graphics.drawString(this.font, label.getString(), BIOMASS_LEFT, BIOMASS_TOP, COLOR_BIOMASS, true);
    }

    protected void renderReadouts(GuiGraphics graphics) {
        OrganStatSummary summary = OrganStatSummary.compute(HiveLeaderOrganClientData.data());

        List<String> statsLines = OrganStatReadout.statsLines(this.hostEntity, summary,
                readoutLineLimit(), READOUT_LABELS);
        List<String> damageLines = OrganStatReadout.damageLines(summary, damageTypeRegistry(),
                damageLineLimit(), READOUT_LABELS);

        Rect stats = this.statsBlock;
        int fadeLeft = Math.max(0, stats.left() - READOUT_LEFT_INSET);
        int fadeRight = Math.min(DESIGN_WIDTH - 1, Math.max(stats.right(), this.damageBlock.right()) + 1);
        int fadeTop = Math.max(0, Math.min(stats.top(), readoutDamageTop()) - 2);
        int fadeBottom = Math.min(DESIGN_HEIGHT - 1,
                Math.max(stats.bottom(), this.damageBlock.bottom()));
        if (fadeBottom > fadeTop && fadeRight > fadeLeft) {
            graphics.fill(fadeLeft, fadeTop, fadeRight, fadeBottom, READOUT_FADE);
        }

        int statsX = stats.left();
        for (int i = 0; i < statsLines.size(); i++) {
            graphics.drawString(this.font, statsLines.get(i), statsX,
                    stats.top() + i * READOUT_LINE_HEIGHT, COLOR_STATS, true);
        }

        int damageX = this.damageBlock.left();
        int damageY = readoutDamageTop();
        for (int i = 0; i < damageLines.size(); i++) {
            graphics.drawString(this.font, damageLines.get(i), damageX,
                    damageY + i * READOUT_LINE_HEIGHT, COLOR_DAMAGE_INFO, true);
        }
    }

    private static final ReadoutLabels READOUT_LABELS = new ReadoutLabels() {
        @Override
        public Component attributeName(String attributeKey) {
            return ATTRIBUTE_LABELS.computeIfAbsent(attributeKey,
                    key -> Component.translatable(KEY_ATTRIBUTE_PREFIX + key + ".name"));
        }

        @Override
        public Component sourcePurple() {
            return Component.translatable(KEY_SOURCE_PURPLE);
        }

        @Override
        public Component sourceTorsoInner() {
            return Component.translatable(KEY_SOURCE_TORSO_INNER);
        }

        @Override
        public Component sourceHeadInner() {
            return Component.translatable(KEY_SOURCE_HEAD_INNER);
        }

        @Override
        public Component adaptationChance() {
            return Component.translatable(KEY_READOUT_ADAPT_CHANCE);
        }

        @Override
        public Component adaptationReduction() {
            return Component.translatable(KEY_READOUT_ADAPT_REDUCTION);
        }

        @Override
        public Component percentUnit() {
            return Component.translatable(KEY_READOUT_PERCENT);
        }
    };

    @Nullable
    protected Registry<DamageType> damageTypeRegistry() {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null || minecraft.level == null) return null;
        return minecraft.level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
    }

    protected void renderPlayerModel(GuiGraphics graphics) {
        AbstractClientPlayer player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null) {

            graphics.drawCenteredString(this.font, Component.translatable(KEY_NO_MODEL),
                    MODEL_CENTER_X, modelCenterY() - 4, COLOR_HINT);
            return;
        }
        this.hostEntity = player;

        renderRotatableModel(graphics, MODEL_CENTER_X, modelScreenAnchorY(),
                this.modelSize, this.rotation.yaw(), this.rotation.pitch(), player);
    }

    public static void renderRotatableModel(GuiGraphics graphics, int anchorX, int anchorY,
                                            int size,
                                            float yawDegrees, float pitchDegrees, LivingEntity entity) {

        float yawComponent = (yawDegrees - 180.0F) / 40.0F;

        float pitchComponent = -pitchDegrees / 20.0F;
        InventoryScreen.renderEntityInInventoryFollowsAngle(
                graphics, anchorX, anchorY, size, yawComponent, pitchComponent, entity);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double layoutX = toLayoutX(mouseX);
        double layoutY = toLayoutY(mouseY);
        if (button == HiveLeaderOrganActionPacket.BUTTON_LEFT
                || button == HiveLeaderOrganActionPacket.BUTTON_RIGHT
                || button == HiveLeaderOrganActionPacket.BUTTON_MIDDLE) {
            int slot = slotAt(layoutX, layoutY);
            if (slot >= 0 && !this.draggingModel) {
                HiveLeaderOrganActionPacket.Action action;
                if (button == HiveLeaderOrganActionPacket.BUTTON_MIDDLE) {

                    action = HiveLeaderOrganActionPacket.Action.CLEAR;
                } else if (hasShiftDown()) {

                    action = HiveLeaderOrganActionPacket.Action.QUICK_MOVE;
                } else {
                    action = HiveLeaderOrganActionPacket.Action.CLICK;
                }
                sendSlotAction(action, slot, button);
                return true;
            }

            int inventorySlot = inventorySlotAt(layoutX, layoutY);
            if (inventorySlot >= 0 && !this.draggingModel) {
                sendInventoryAction(inventorySlot, button);
                return true;
            }
        }

        if (button == HiveLeaderOrganActionPacket.BUTTON_LEFT && !isInsidePanel(layoutX, layoutY)) {
            if (HiveLeaderOrganClientData.hasPendingLocalCarried()) {

                sendReturnCarried();
                return true;
            }
        }

        if (button == HiveLeaderOrganActionPacket.BUTTON_LEFT && isOverModel(layoutX, layoutY)) {
            this.draggingModel = true;
            this.dragAnchorX = mouseX;
            this.dragAnchorY = mouseY;
            this.rotation.beginDrag();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingModel && button == HiveLeaderOrganActionPacket.BUTTON_LEFT) {

            float deltaX = (float) (mouseX - this.dragAnchorX);
            float deltaY = (float) (mouseY - this.dragAnchorY);
            this.dragAnchorX = mouseX;
            this.dragAnchorY = mouseY;

            float sensitivity = this.layoutScale;
            this.rotation.dragged(deltaX * sensitivity, deltaY * sensitivity);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingModel && button == HiveLeaderOrganActionPacket.BUTTON_LEFT) {
            this.draggingModel = false;
            this.rotation.endDrag();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected boolean isOverModel(double mouseX, double mouseY) {
        ModelBody body = modelBody();
        int left = body.leftArm().left();
        int right = body.rightArm().right();
        int top = body.topY();
        int bottom = body.feetY();
        int pad = 6;
        return mouseX >= left - pad && mouseX <= right + pad
                && mouseY >= top - pad && mouseY <= bottom + pad;
    }

    protected boolean isInsidePanel(double mouseX, double mouseY) {
        return mouseX >= 0 && mouseX <= DESIGN_WIDTH && mouseY >= 0 && mouseY <= DESIGN_HEIGHT;
    }

    protected int slotAt(double mouseX, double mouseY) {
        for (int index = 0; index < slotGeometry.length; index++) {
            int x = slotGeometry[index][0];
            int y = slotGeometry[index][1];
            if (isInside(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE)) {
                return index;
            }
        }
        return -1;
    }

    protected int inventorySlotAt(double mouseX, double mouseY) {
        for (int index = 0; index < inventoryGeometry.length; index++) {
            int x = inventoryGeometry[index][0];
            int y = inventoryGeometry[index][1];
            if (isInside(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static boolean rectsOverlapPx(int x, int y, int width, int height, Rect other) {
        return x < other.right() && other.left() < x + width
                && y < other.bottom() && other.top() < y + height;
    }

    protected void sendSlotAction(HiveLeaderOrganActionPacket.Action action, int slot, int button) {
        switch (action) {
            case CLICK -> predictOrganSlotClick(slot, button);
            case QUICK_MOVE -> predictOrganSlotQuickMove(slot);
            case CLEAR -> predictOrganSlotClear(slot);
            default -> {
            }
        }

        ModNetwork.sendToServer(new HiveLeaderOrganActionPacket(action, slot, button));
    }

    private void predictOrganSlotClick(int slot, int button) {
        ItemStack carried = HiveLeaderOrganClientData.carried();
        ItemStack inSlot = organSlotStack(slot);
        boolean right = button == HiveLeaderOrganActionPacket.BUTTON_RIGHT;

        ItemStack carriedAfter = carried;
        ItemStack expectedSlot = inSlot;

        if (carried.isEmpty()) {
            if (!inSlot.isEmpty()) {
                int amount = right ? Math.max(1, inSlot.getCount() / 2) : inSlot.getCount();
                carriedAfter = inSlot.copyWithCount(amount);
                expectedSlot = amount >= inSlot.getCount()
                        ? ItemStack.EMPTY
                        : inSlot.copyWithCount(inSlot.getCount() - amount);
            }
        } else if (!HiveLeaderOrganData.acceptsItem(carried)) {

            expectedSlot = inSlot;
        } else if (inSlot.isEmpty()) {
            int amount = right ? 1 : carried.getCount();
            expectedSlot = carried.copyWithCount(amount);
            carriedAfter = takeFrom(carried, amount);
        } else if (ItemStack.isSameItemSameTags(inSlot, carried)
                && inSlot.getCount() < inSlot.getMaxStackSize()) {
            int space = inSlot.getMaxStackSize() - inSlot.getCount();
            int amount = right ? Math.min(1, space) : Math.min(space, carried.getCount());
            expectedSlot = inSlot.copyWithCount(inSlot.getCount() + amount);
            carriedAfter = takeFrom(carried, amount);
        } else if (HiveLeaderOrganData.acceptsItem(inSlot)) {
            expectedSlot = carried.copy();
            carriedAfter = inSlot.copy();
        }
        HiveLeaderOrganClientData.predictOrganSlotAction(slot, expectedSlot, carried, carriedAfter);
    }

    private void predictOrganSlotQuickMove(int slot) {
        ItemStack carried = HiveLeaderOrganClientData.carried();
        ItemStack inSlot = organSlotStack(slot);

        ItemStack carriedAfter = carried;
        ItemStack expectedSlot = inSlot;

        if (carried.isEmpty()) {
            if (!inSlot.isEmpty()) expectedSlot = ItemStack.EMPTY;
        } else if (!HiveLeaderOrganData.acceptsItem(carried)) {
            expectedSlot = inSlot;
        } else if (inSlot.isEmpty()) {
            expectedSlot = carried.copy();
            carriedAfter = ItemStack.EMPTY;
        } else if (ItemStack.isSameItemSameTags(inSlot, carried)) {
            int space = inSlot.getMaxStackSize() - inSlot.getCount();
            if (space > 0) {
                int amount = Math.min(space, carried.getCount());
                expectedSlot = inSlot.copyWithCount(inSlot.getCount() + amount);
                carriedAfter = takeFrom(carried, amount);
            }
        }
        HiveLeaderOrganClientData.predictOrganSlotAction(slot, expectedSlot, carried, carriedAfter);
    }

    private void predictOrganSlotClear(int slot) {
        ItemStack carried = HiveLeaderOrganClientData.carried();
        ItemStack inSlot = organSlotStack(slot);
        ItemStack expectedSlot = inSlot.isEmpty() ? inSlot : ItemStack.EMPTY;
        HiveLeaderOrganClientData.predictOrganSlotAction(slot, expectedSlot, carried, carried);
    }

    private static ItemStack organSlotStack(int slot) {
        HiveLeaderOrganData data = HiveLeaderOrganClientData.data();
        if (slot < 0 || slot >= data.size()) return ItemStack.EMPTY;
        return data.getItem(slot);
    }

    private static ItemStack takeFrom(ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return stack;
        int left = stack.getCount() - amount;
        return left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left);
    }

    protected void sendInventoryAction(int inventorySlot, int button) {
        if (button == HiveLeaderOrganActionPacket.BUTTON_MIDDLE
                || (hasShiftDown() && button == HiveLeaderOrganActionPacket.BUTTON_LEFT)) {

            HiveLeaderOrganClientData.predictCarried(ItemStack.EMPTY);
            ModNetwork.sendToServer(HiveLeaderOrganActionPacket.inventoryQuickMove(inventorySlot));
            return;
        }

        ItemStack carried = HiveLeaderOrganClientData.carried();
        Inventory inventory = clientInventory();
        ItemStack inSlot = inventory == null ? ItemStack.EMPTY : inventoryItem(inventory, inventorySlot);
        boolean right = button == HiveLeaderOrganActionPacket.BUTTON_RIGHT;

        ItemStack carriedAfter = carried;
        if (carried.isEmpty()) {
            if (!inSlot.isEmpty()) {

                int amount = right ? (inSlot.getCount() + 1) / 2 : inSlot.getCount();
                carriedAfter = inSlot.copyWithCount(amount);
            }
        } else if (inSlot.isEmpty()) {
            int amount = right ? 1 : carried.getCount();
            carriedAfter = takeFrom(carried, amount);
        } else if (ItemStack.isSameItemSameTags(inSlot, carried)) {
            int space = inSlot.getMaxStackSize() - inSlot.getCount();
            if (space > 0) {
                int amount = right ? Math.min(1, space) : Math.min(space, carried.getCount());
                if (amount > 0) carriedAfter = takeFrom(carried, amount);
            }
        } else {
            carriedAfter = inSlot.copy();
        }

        HiveLeaderOrganClientData.predictCarried(carriedAfter);
        ModNetwork.sendToServer(HiveLeaderOrganActionPacket.inventorySlot(inventorySlot, button));
    }

    protected void sendReturnCarried() {
        HiveLeaderOrganClientData.clearCarried();
        ModNetwork.sendToServer(HiveLeaderOrganActionPacket.returnCarried());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        this.draggingModel = false;
        sendReturnCarried();
        super.removed();
    }

    protected AbstractClientPlayer localPlayer() {
        Minecraft minecraft = this.minecraft;
        return minecraft == null ? null : minecraft.player;
    }

    protected int[] layoutSnapshot() {
        return new int[]{
                MODEL_SIZE, PANEL_WIDTH, PANEL_HEIGHT, MODEL_CENTER_X, MODEL_CENTER_Y,
                SLOT_SIZE, SLOT_STRIDE, TORSO_INNER_STRIDE, INVENTORY_STRIDE,
                TORSO_INNER_LEFT, TORSO_INNER_TOP, HEAD_INNER_LEFT, HEAD_INNER_TOP,
                INVENTORY_LEFT, INVENTORY_TOP, HOTBAR_TOP,

                this.width, this.height,
                Math.round(this.layoutScale * 1000.0F),
                panelScreenWidth(), panelScreenHeight(),
                this.modelSize,
                this.compactLayout ? 1 : 0,
                MODEL_SIZE - this.modelSize};
    }

    protected String layoutDebugInfo() {
        return "scale=" + this.layoutScale
                + " 面板=" + panelLeft() + "," + panelTop() + " " + panelScreenWidth() + "x" + panelScreenHeight()
                + " 窗口=" + this.width + "x" + this.height
                + " 模型=" + this.modelSize + " 密集=" + (this.compactLayout ? "是" : "否")
                + " 行数=" + readoutLineLimit() + "/" + damageLineLimit()
                + " 紫块=" + rectText(this.statsBlock) + " 绿块=" + rectText(this.damageBlock);
    }

    private static String rectText(Rect rect) {
        return rect.left() + "," + rect.top() + ".." + rect.right() + "," + rect.bottom();
    }

    protected int slotOverlapCount() {
        int overlaps = 0;
        for (int a = 0; a < slotGeometry.length; a++) {
            for (int b = a + 1; b < slotGeometry.length; b++) {
                if (rectsOverlap(slotGeometry[a], slotGeometry[b])) overlaps++;
            }
        }
        for (int a = 0; a < slotGeometry.length; a++) {
            for (int b = 0; b < inventoryGeometry.length; b++) {
                if (rectsOverlap(slotGeometry[a], inventoryGeometry[b])) overlaps++;
            }
        }
        for (int a = 0; a < inventoryGeometry.length; a++) {
            for (int b = a + 1; b < inventoryGeometry.length; b++) {
                if (rectsOverlap(inventoryGeometry[a], inventoryGeometry[b])) overlaps++;
            }
        }
        return overlaps;
    }

    protected int readoutOverlapCount() {
        int overlaps = 0;
        for (int[] slot : slotGeometry) {
            if (rectsOverlap(slot, this.statsBlock)) overlaps++;
            if (rectsOverlap(slot, this.damageBlock)) overlaps++;
        }
        for (int[] inv : inventoryGeometry) {
            if (rectsOverlap(inv, this.statsBlock)) overlaps++;
            if (rectsOverlap(inv, this.damageBlock)) overlaps++;
        }
        return overlaps;
    }

    protected int outOfPanelCount() {
        int out = 0;
        for (int[] slot : slotGeometry) {
            if (!insidePanel(slot[0], slot[1], SLOT_SIZE, SLOT_SIZE)) out++;
        }
        for (int[] inv : inventoryGeometry) {
            if (!insidePanel(inv[0], inv[1], SLOT_SIZE, SLOT_SIZE)) out++;
        }
        if (!insidePanel(this.statsBlock.left(), this.statsBlock.top(),
                this.statsBlock.width(), this.statsBlock.height())) out++;
        if (!insidePanel(this.damageBlock.left(), this.damageBlock.top(),
                this.damageBlock.width(), this.damageBlock.height())) out++;
        ModelBody body = modelBody();
        if (body.topY() < 0 || body.feetY() > DESIGN_HEIGHT
                || body.leftArm().left() < 0 || body.rightArm().right() > DESIGN_WIDTH) out++;
        return out;
    }

    private static boolean insidePanel(int x, int y, int width, int height) {
        return x >= 0 && y >= 0 && x + width <= DESIGN_WIDTH && y + height <= DESIGN_HEIGHT;
    }

    private static boolean rectsOverlap(int[] a, int[] b) {
        return a[0] < b[0] + SLOT_SIZE && b[0] < a[0] + SLOT_SIZE
                && a[1] < b[1] + SLOT_SIZE && b[1] < a[1] + SLOT_SIZE;
    }

    private static boolean rectsOverlap(int[] a, Rect b) {
        return a[0] < b.right() && b.left() < a[0] + SLOT_SIZE
                && a[1] < b.bottom() && b.top() < a[1] + SLOT_SIZE;
    }
}
