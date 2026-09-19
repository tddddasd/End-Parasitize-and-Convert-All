package org.tdddd.epca.impl.overworld.registry.gui.menus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import org.tdddd.epca.impl.overworld.data.EPCANoteTabData;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.tdddd.epca.impl.epca;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 26.1.2 渲染/GUI 迁移说明：
 * <ul>
 *   <li>{@code Screen#render(GuiGraphics, ...)} 改为 {@code extractRenderState(GuiGraphicsExtractor, ...)}
 *       （GUI 改成「抽取」模型，真正的绘制由 GuiRenderState 完成）；</li>
 *   <li>{@code GuiGraphics#drawString} → {@code GuiGraphicsExtractor#text}；</li>
 *   <li>{@code RenderSystem#setShaderTexture} 被删除：贴图由 blit 的 RenderPipeline 绑定；</li>
 *   <li>{@code PoseStack} → {@code Matrix3x2fStack}（2D：pushMatrix/popMatrix/translate(x,y)/scale(x,y)）；</li>
 *   <li>鼠标事件参数由 (double x, double y, int button) 改为 {@code MouseButtonEvent}；</li>
 *   <li>{@code AbstractButton#renderWidget} → {@code extractContents}。</li>
 * </ul>
 */
public class EPCANoteScreen extends Screen {

    // 只有内页贴图随模组一起提供；封面与选项卡改为用 inner_frame.png 的同色系程序化绘制，
    // 避免引用不存在的 outer_frame.png / parent_tab.png / child_tab.png 而每帧报警并画出空白。
    private static final Identifier INNER_FRAME = Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/epca_note/inner_frame.png");
    private static final Identifier BTN_UP = Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/epca_note/button_up.png");
    private static final Identifier BTN_DOWN = Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/epca_note/button_down.png");
    private static final Identifier BTN_LEFT = Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/epca_note/button_left.png");
    private static final Identifier BTN_RIGHT = Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/epca_note/button_right.png");
    private static final Identifier PAGE_BTN_LEFT = Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/epca_note/page_btn_left.png");
    private static final Identifier PAGE_BTN_RIGHT = Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/epca_note/page_btn_right.png");

    private static final int OUTER_W = 540;
    private static final int OUTER_H = 360;
    private static final int INNER_W = 420;
    private static final int INNER_H = 270;
    private static final int PARENT_TAB_W = 90;
    private static final int PARENT_TAB_H = 60;
    private static final int CHILD_TAB_W = 45;
    private static final int CHILD_TAB_H = 75;
    private static final int BUTTON_W = 24;
    private static final int BUTTON_H = 24;
    private static final int PAGE_BTN_SIZE = 18;

    private static final int INNER_TEX_W = 420;
    private static final int INNER_TEX_H = 270;
    private static final int BTN_TEX_W = 24;
    private static final int BTN_TEX_H = 24;
    private static final int PAGE_BTN_TEX_SIZE = 18;

    // ---- 封面与选项卡配色（取自 inner_frame.png 的羊皮纸 / 皮革色调）----
    private static final int COVER_SHADOW = 0xFF1E0D05;
    private static final int COVER_DARK = 0xFF3C1C0E;
    private static final int COVER = 0xFF5C2E18;
    private static final int COVER_LIGHT = 0xFF7B4423;
    private static final int COVER_EDGE = 0xFFCCB998;
    private static final int COVER_LINE = 0xFF8A6A46;

    private static final int TAB_BG = 0xFF6E3B21;
    private static final int TAB_BG_HOVER = 0xFF83502F;
    private static final int TAB_BG_SELECTED = 0xFFC9A46B;
    private static final int TAB_HI = 0xFF9A6238;
    private static final int TAB_LO = 0xFF2A1308;
    private static final int TAB_HI_SELECTED = 0xFFE7D3A6;
    private static final int TAB_LO_SELECTED = 0xFF8A6A46;
    private static final int TAB_TEXT = 0xFFFFF4DC;
    private static final int TAB_TEXT_SELECTED = 0xFF2B1608;

    private static final int PAGE_TEXT = 0xFF3A2412;
    private static final int PAGE_TEXT_DIM = 0xFF6B4A2E;
    private static final int PAGE_TEXT_WARN = 0xFF8C2F12;
    private static final int TITLE_TEXT = 0xFFE7D3A6;
    private static final int PAGE_GROOVE = 7;

    private static final int MAX_PARENT_VISIBLE = 4;
    private static final int MAX_CHILD_VISIBLE = 8;
    private static final int CONTENT_PADDING = 8;
    private static final int LEFT_RIGHT_SPACING = 6;


    private int parentScrollOffset = 0;
    private int selectedParentIndex = 0;
    private int selectedChildIndex = 0;
    private int childScrollOffset = 0;

    private List<EPCANoteTabData.ParentTab> parentTabs;
    private List<EPCANoteTabData.ChildTab> currentChildTabs = List.of();

    private List<PageContent> pages = new ArrayList<>();
    private int currentPage = 0;
    private Button btnPrevPage, btnNextPage;
    private Button btnParentUp, btnParentDown;
    private Button btnChildLeft, btnChildRight;

    private int outerX, outerY;
    private int innerX, innerY;
    private int parentListStartX, parentListStartY;
    private int childListStartY;
    private int leftAreaX, leftAreaY, leftAreaW, leftAreaH;
    private int rightAreaX, rightAreaY, rightAreaW, rightAreaH;

    private float getScaleFactor() {
        int guiScale = (int) Minecraft.getInstance().getWindow().getGuiScale();
        return (guiScale == 3) ? 0.65f : 1.0f;
    }

    private final Object eventListener = new Object() {
        @SubscribeEvent
        public void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {

            Minecraft.getInstance().execute(() -> {
                if (EPCANoteScreen.this.minecraft != null && EPCANoteScreen.this.minecraft.screen == EPCANoteScreen.this) {
                    refreshTabsAndUI();
                }
            });
        }
    };

    public EPCANoteScreen() {
        super(Component.translatable("epca.note.title"));

        NeoForge.EVENT_BUS.register(eventListener);
        refreshTabData();
    }

    @Override
    protected void init() {
        super.init();

        recomputeLayout();

        rebuildUIComponents();

        updateCurrentChildTabs();
        rebuildPagesForCurrentChild();
    }


    private void refreshTabsAndUI() {
        refreshTabData();

        parentScrollOffset = 0;
        selectedParentIndex = 0;
        childScrollOffset = 0;
        selectedChildIndex = 0;

        recomputeLayout();

        rebuildUIComponents();

        updateCurrentChildTabs();
        rebuildPagesForCurrentChild();
    }


    private void refreshTabData() {
        this.parentTabs = EPCANoteTabData.getVisibleTabsForPlayer();
        if (this.parentTabs == null) {
            this.parentTabs = List.of();
        }

        if (selectedParentIndex >= parentTabs.size()) {
            selectedParentIndex = parentTabs.isEmpty() ? 0 : parentTabs.size() - 1;
        }
    }


    private void recomputeLayout() {
        outerX = (width - OUTER_W) / 2;
        outerY = (height - OUTER_H) / 2;
        innerX = outerX + (OUTER_W - INNER_W) / 2;
        innerY = outerY + (OUTER_H - INNER_H) / 2;

        leftAreaW = (INNER_W - LEFT_RIGHT_SPACING - 2 * CONTENT_PADDING) / 2;
        rightAreaW = leftAreaW;
        leftAreaH = rightAreaH = INNER_H - 2 * CONTENT_PADDING;
        leftAreaX = innerX + CONTENT_PADDING;
        leftAreaY = innerY + CONTENT_PADDING;
        rightAreaX = leftAreaX + leftAreaW + LEFT_RIGHT_SPACING;
        rightAreaY = leftAreaY;

        parentListStartX = outerX - 45;
        int parentTotalHeight = Math.min(parentTabs.size(), MAX_PARENT_VISIBLE) * PARENT_TAB_H;
        parentListStartY = outerY + (OUTER_H - parentTotalHeight) / 2;
        childListStartY = innerY - 75;
    }


    private void rebuildUIComponents() {

        if (btnParentUp != null) removeWidget(btnParentUp);
        if (btnParentDown != null) removeWidget(btnParentDown);
        if (btnChildLeft != null) removeWidget(btnChildLeft);
        if (btnChildRight != null) removeWidget(btnChildRight);
        if (btnPrevPage != null) removeWidget(btnPrevPage);
        if (btnNextPage != null) removeWidget(btnNextPage);


        if (parentTabs.size() > MAX_PARENT_VISIBLE) {
            int upY = parentListStartY - BUTTON_H - 5;
            int downY = parentListStartY + MAX_PARENT_VISIBLE * PARENT_TAB_H + 5;
            int btnX = parentListStartX + PARENT_TAB_W / 2 - BUTTON_W / 2;
            btnParentUp = new ImageButton(btnX, upY, BUTTON_W, BUTTON_H, BTN_UP, BTN_TEX_W, BTN_TEX_H, b -> scrollParent(-1));
            btnParentDown = new ImageButton(btnX, downY, BUTTON_W, BUTTON_H, BTN_DOWN, BTN_TEX_W, BTN_TEX_H, b -> scrollParent(1));
            addRenderableWidget(btnParentUp);
            addRenderableWidget(btnParentDown);
        }


        int btnPosXLeft = innerX + CONTENT_PADDING;
        int btnPosXRight = innerX + INNER_W - CONTENT_PADDING - PAGE_BTN_SIZE;
        int btnPosY = innerY + INNER_H - CONTENT_PADDING - PAGE_BTN_SIZE;
        btnPrevPage = new ImageButton(btnPosXLeft, btnPosY,
                PAGE_BTN_SIZE, PAGE_BTN_SIZE,
                PAGE_BTN_LEFT, PAGE_BTN_TEX_SIZE, PAGE_BTN_TEX_SIZE,
                b -> turnPage(-1));
        btnNextPage = new ImageButton(btnPosXRight, btnPosY,
                PAGE_BTN_SIZE, PAGE_BTN_SIZE,
                PAGE_BTN_RIGHT, PAGE_BTN_TEX_SIZE, PAGE_BTN_TEX_SIZE,
                b -> turnPage(1));
        addRenderableWidget(btnPrevPage);
        addRenderableWidget(btnNextPage);


        updateChildButtons();

        updatePageButtons();
    }


    private void updateChildButtons() {
        if (btnChildLeft != null) removeWidget(btnChildLeft);
        if (btnChildRight != null) removeWidget(btnChildRight);
        if (currentChildTabs.size() > MAX_CHILD_VISIBLE) {
            int visibleWidth = MAX_CHILD_VISIBLE * CHILD_TAB_W;
            int startX = innerX + (INNER_W - visibleWidth) / 2;
            int btnY = childListStartY + CHILD_TAB_H / 2 - BUTTON_H / 2;
            btnChildLeft = new ImageButton(startX - BUTTON_W - 5, btnY, BUTTON_W, BUTTON_H, BTN_LEFT, BTN_TEX_W, BTN_TEX_H, b -> scrollChild(-1));
            btnChildRight = new ImageButton(startX + visibleWidth + 5, btnY, BUTTON_W, BUTTON_H, BTN_RIGHT, BTN_TEX_W, BTN_TEX_H, b -> scrollChild(1));
            addRenderableWidget(btnChildLeft);
            addRenderableWidget(btnChildRight);
        }
    }

    private void scrollParent(int delta) {
        int maxOffset = Math.max(0, parentTabs.size() - MAX_PARENT_VISIBLE);
        parentScrollOffset = Math.max(0, Math.min(maxOffset, parentScrollOffset + delta));
    }

    private void scrollChild(int delta) {
        int maxOffset = Math.max(0, currentChildTabs.size() - MAX_CHILD_VISIBLE);
        childScrollOffset = Math.max(0, Math.min(maxOffset, childScrollOffset + delta));
    }

    private void updateCurrentChildTabs() {
        if (selectedParentIndex >= 0 && selectedParentIndex < parentTabs.size()) {
            currentChildTabs = parentTabs.get(selectedParentIndex).getChildTabs();
        } else {
            currentChildTabs = List.of();
        }
        childScrollOffset = 0;
        selectedChildIndex = currentChildTabs.isEmpty() ? -1 : 0;
        updateChildButtons();
    }

    private void rebuildPagesForCurrentChild() {
        pages.clear();
        currentPage = 0;
        if (selectedChildIndex < 0 || selectedChildIndex >= currentChildTabs.size()) {
            pages.add(PageContent.empty());
            updatePageButtons();
            return;
        }
        String rawContent = currentChildTabs.get(selectedChildIndex).content;
        String translated = translateContent(rawContent);
        if (translated != null && !translated.isEmpty()) {
            pages = buildPages(translated, leftAreaW, rightAreaW, leftAreaH, font);
        }
        if (pages.isEmpty()) {
            pages.add(PageContent.empty());
        }
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (btnPrevPage != null) btnPrevPage.active = currentPage > 0;
        if (btnNextPage != null) btnNextPage.active = currentPage < pages.size() - 1;
    }

    private void turnPage(int delta) {
        int newPage = currentPage + delta;
        if (newPage >= 0 && newPage < pages.size()) {
            currentPage = newPage;
            updatePageButtons();
        }
    }


    private List<PageContent> buildPages(String rawContent, int leftWidth, int rightWidth, int areaHeight, Font font) {
        List<RenderElement> elements = parseElements(rawContent, leftWidth, font);
        return splitElementsIntoPages(elements, leftWidth, rightWidth, areaHeight, font);
    }

    private static final Pattern IMG_PATTERN = Pattern.compile("\\$\\{img:([^,}]+)(?:,(\\d+))?\\}\\$");
    private static final Pattern PAGE_BREAK_PATTERN = Pattern.compile("\\$\\[page\\]\\$");

    private List<RenderElement> parseElements(String raw, int maxWidth, Font font) {
        List<RenderElement> elements = new ArrayList<>();
        int lastIdx = 0;
        Matcher imgMatcher = IMG_PATTERN.matcher(raw);
        Matcher pageMatcher = PAGE_BREAK_PATTERN.matcher(raw);
        List<Object> matches = new ArrayList<>();
        while (imgMatcher.find()) {
            matches.add(new Match(imgMatcher.start(), imgMatcher.end(), true, imgMatcher.group(1), imgMatcher.group(2)));
        }
        while (pageMatcher.find()) {
            matches.add(new Match(pageMatcher.start(), pageMatcher.end(), false, null, null));
        }
        matches.sort(Comparator.comparingInt(m -> ((Match)m).start));

        int cursor = 0;
        for (Object obj : matches) {
            Match m = (Match) obj;
            if (m.start > cursor) {
                String textChunk = raw.substring(cursor, m.start);
                addTextElements(textChunk, maxWidth, font, elements);
            }
            if (m.isImage) {
                String path = m.group1;
                int size = 32;
                if (m.group2 != null) {
                    try { size = Integer.parseInt(m.group2); } catch (NumberFormatException ignored) {}
                }
                int displaySize;
                if (size == 256 || size == 512) {
                    displaySize = (int)(128 * 1.5);
                } else {

                    size = Math.min(128, Math.max(16, size));
                    displaySize = (int)(size * 1.5);
                }
                elements.add(new ImageElement(Identifier.parse(path), displaySize, displaySize));
            } else {
                elements.add(new PageBreakElement());
            }
            cursor = m.end;
        }
        if (cursor < raw.length()) {
            addTextElements(raw.substring(cursor), maxWidth, font, elements);
        }
        return elements;
    }

    private static class Match {
        int start, end;
        boolean isImage;
        String group1, group2;
        Match(int start, int end, boolean isImage, String g1, String g2) {
            this.start = start; this.end = end; this.isImage = isImage; this.group1 = g1; this.group2 = g2;
        }
    }

    private static class PageBreakElement implements RenderElement {
        @Override
        public int getHeight(Font font) { return 0; }
    }

    private void addTextElements(String text, int maxWidth, Font font, List<RenderElement> out) {
        if (text.isEmpty()) return;
        for (String line : text.split("\n")) {
            List<FormattedCharSequence> wrapped = font.split(FormattedText.of(line), maxWidth);
            for (FormattedCharSequence seq : wrapped) {
                out.add(new TextLine(seq));
            }
        }
    }

    private List<PageContent> splitElementsIntoPages(List<RenderElement> allElements,
                                                     int leftW, int rightW, int areaH, Font font) {
        List<PageContent> pages = new ArrayList<>();
        int idx = 0;
        while (idx < allElements.size()) {
            List<RenderElement> leftElems = new ArrayList<>();
            int leftUsedH = 0;
            while (idx < allElements.size()) {
                RenderElement e = allElements.get(idx);
                if (e instanceof PageBreakElement) {
                    idx++;
                    break;
                }
                int elemH = e.getHeight(font);
                if (leftUsedH + elemH <= areaH) {
                    leftElems.add(e);
                    leftUsedH += elemH;
                    idx++;
                } else {
                    break;
                }
            }
            List<RenderElement> rightElems = new ArrayList<>();
            int rightUsedH = 0;
            while (idx < allElements.size()) {
                RenderElement e = allElements.get(idx);
                if (e instanceof PageBreakElement) {
                    idx++;
                    break;
                }
                int elemH = e.getHeight(font);
                if (rightUsedH + elemH <= areaH) {
                    rightElems.add(e);
                    rightUsedH += elemH;
                    idx++;
                } else {
                    break;
                }
            }
            pages.add(new PageContent(leftElems, rightElems));
        }
        if (pages.isEmpty()) {
            pages.add(PageContent.empty());
        }
        return pages;
    }

    private int getClickedParentIndex(int mouseX, int mouseY) {
        if (mouseX < parentListStartX || mouseX > parentListStartX + PARENT_TAB_W) return -1;
        int startY = parentListStartY;
        for (int i = 0; i < MAX_PARENT_VISIBLE; i++) {
            int idx = parentScrollOffset + i;
            if (idx >= parentTabs.size()) break;
            int y = startY + i * PARENT_TAB_H;
            if (mouseY >= y && mouseY <= y + PARENT_TAB_H) return idx;
        }
        return -1;
    }

    private int getClickedChildIndex(int mouseX, int mouseY) {
        if (mouseY < childListStartY || mouseY > childListStartY + CHILD_TAB_H) return -1;
        int visibleCount = Math.min(currentChildTabs.size() - childScrollOffset, MAX_CHILD_VISIBLE);
        int totalWidth = visibleCount * CHILD_TAB_W;
        int startX = innerX + (INNER_W - totalWidth) / 2;
        for (int i = 0; i < visibleCount; i++) {
            int x = startX + i * CHILD_TAB_W;
            if (mouseX >= x && mouseX <= x + CHILD_TAB_W) return childScrollOffset + i;
        }
        return -1;
    }


    private String translateName(String raw) {
        if (raw == null) return "";
        if (raw.startsWith("lang:")) {
            return Component.translatable(raw.substring(5)).getString();
        }
        return raw;
    }

    private String translateContent(String raw) {
        if (raw == null) return "";
        if (raw.startsWith("lang:")) {
            return Component.translatable(raw.substring(5)).getString();
        }
        return raw;
    }


    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        float scale = getScaleFactor();
        var pose = guiGraphics.pose();
        pose.pushMatrix();

        // 计算转换后的鼠标坐标（用于传递给 super.extractRenderState 的 tooltip 等）
        double layoutX = convertMouseX(mouseX);
        double layoutY = convertMouseY(mouseY);

        if (scale != 1.0f) {
            pose.translate(width / 2.0f, height / 2.0f);
            pose.scale(scale, scale);
            pose.translate(-width / 2.0f, -height / 2.0f);
        }

        // 悬停检测（画选项卡高亮 + 底部显示完整名称）
        int hoveredParent = getClickedParentIndex((int) layoutX, (int) layoutY);
        int hoveredChild = getClickedChildIndex((int) layoutX, (int) layoutY);

        drawCover(guiGraphics);


        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, INNER_FRAME, innerX, innerY, 0.0F, 0.0F, INNER_W, INNER_H, INNER_TEX_W, INNER_TEX_H);


        for (int i = 0; i < MAX_PARENT_VISIBLE; i++) {
            int idx = parentScrollOffset + i;
            if (idx >= parentTabs.size()) break;
            EPCANoteTabData.ParentTab tab = parentTabs.get(idx);
            int y = parentListStartY + i * PARENT_TAB_H;
            boolean selected = selectedParentIndex == idx;
            drawTab(guiGraphics, parentListStartX, y, PARENT_TAB_W, PARENT_TAB_H, selected, hoveredParent == idx);
            String displayName = fit(font, translateName(tab.name), PARENT_TAB_W - 8);
            int textWidth = font.width(displayName);
            guiGraphics.text(font, displayName,
                    parentListStartX + (PARENT_TAB_W - textWidth) / 2,
                    y + (PARENT_TAB_H - font.lineHeight) / 2,
                    selected ? TAB_TEXT_SELECTED : TAB_TEXT);
        }


        int visibleCount = Math.min(currentChildTabs.size() - childScrollOffset, MAX_CHILD_VISIBLE);
        int totalWidth = visibleCount * CHILD_TAB_W;
        int startX = innerX + (INNER_W - totalWidth) / 2;
        for (int i = 0; i < visibleCount; i++) {
            int idx = childScrollOffset + i;
            EPCANoteTabData.ChildTab child = currentChildTabs.get(idx);
            int x = startX + i * CHILD_TAB_W;
            boolean selected = selectedChildIndex == idx;
            drawTab(guiGraphics, x, childListStartY, CHILD_TAB_W, CHILD_TAB_H, selected, hoveredChild == idx);
            String displayName = fit(font, translateName(child.name), CHILD_TAB_W - 4);
            int textWidth = font.width(displayName);
            guiGraphics.text(font, displayName,
                    x + (CHILD_TAB_W - textWidth) / 2,
                    childListStartY + 6,
                    selected ? TAB_TEXT_SELECTED : TAB_TEXT);
        }


        drawPages(guiGraphics);


        drawFooter(guiGraphics, hoveredParent, hoveredChild);

        super.extractRenderState(guiGraphics, (int) layoutX, (int) layoutY, partialTick);

        pose.popMatrix();
    }

    private double convertMouseX(double mouseX) {
        float scale = getScaleFactor();
        if (scale == 1.0f) return mouseX;
        return (mouseX - width / 2.0) / scale + width / 2.0;
    }

    private double convertMouseY(double mouseY) {
        float scale = getScaleFactor();
        if (scale == 1.0f) return mouseY;
        return (mouseY - height / 2.0) / scale + height / 2.0;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double layoutX = convertMouseX(event.x());
        double layoutY = convertMouseY(event.y());

        // 自己的点击检测（父标签/子标签）
        int clickedParent = getClickedParentIndex((int) layoutX, (int) layoutY);
        if (clickedParent != -1) {
            selectedParentIndex = clickedParent;
            updateCurrentChildTabs();
            rebuildPagesForCurrentChild();
            return true;
        }
        int clickedChild = getClickedChildIndex((int) layoutX, (int) layoutY);
        if (clickedChild != -1) {
            selectedChildIndex = clickedChild;
            rebuildPagesForCurrentChild();
            return true;
        }

        // 传递给父类以处理按钮等组件（26.1.2: 事件对象携带 x/y 与按键信息）
        return super.mouseClicked(new MouseButtonEvent(layoutX, layoutY, event.buttonInfo()), doubleClick);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        double layoutX = convertMouseX(mouseX);
        double layoutY = convertMouseY(mouseY);
        super.mouseMoved(layoutX, layoutY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double layoutX = convertMouseX(event.x());
        double layoutY = convertMouseY(event.y());
        return super.mouseDragged(new MouseButtonEvent(layoutX, layoutY, event.buttonInfo()), dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double layoutX = convertMouseX(event.x());
        double layoutY = convertMouseY(event.y());
        return super.mouseReleased(new MouseButtonEvent(layoutX, layoutY, event.buttonInfo()));
    }

    /** 封面：皮革底 + 米色描边 + 内页凹槽（书页像嵌在封面里） */
    private void drawCover(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.fill(outerX - 2, outerY - 2, outerX + OUTER_W + 2, outerY + OUTER_H + 2, COVER_SHADOW);
        guiGraphics.fill(outerX, outerY, outerX + OUTER_W, outerY + OUTER_H, COVER_DARK);
        guiGraphics.fill(outerX + 3, outerY + 3, outerX + OUTER_W - 3, outerY + OUTER_H - 3, COVER);
        guiGraphics.fill(outerX + 6, outerY + 6, outerX + OUTER_W - 6, outerY + OUTER_H - 6, COVER_LIGHT);
        guiGraphics.fill(outerX + 10, outerY + 10, outerX + OUTER_W - 10, outerY + OUTER_H - 10, COVER);
        guiGraphics.outline(outerX, outerY, OUTER_W, OUTER_H, COVER_EDGE);
        guiGraphics.outline(outerX + 6, outerY + 6, OUTER_W - 12, OUTER_H - 12, COVER_LINE);

        int g = PAGE_GROOVE;
        int left = innerX - g;
        int top = innerY - g;
        int right = innerX + INNER_W + g;
        int bottom = innerY + INNER_H + g;
        guiGraphics.fill(left, top, right, innerY - 2, COVER_SHADOW);
        guiGraphics.fill(left, innerY + INNER_H + 2, right, bottom, COVER_SHADOW);
        guiGraphics.fill(left, top, innerX - 2, bottom, COVER_SHADOW);
        guiGraphics.fill(innerX + INNER_W + 2, top, right, bottom, COVER_SHADOW);
        guiGraphics.fill(innerX - 2, innerY - 2, innerX + INNER_W + 2, innerY + INNER_H + 2, COVER_EDGE);
    }

    /** 选项卡：底色 + 高光/阴影立体边 + 米色描边；选中用羊皮纸色，悬停提亮 */
    private void drawTab(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h,
                         boolean selected, boolean hovered) {
        int bg = selected ? TAB_BG_SELECTED : (hovered ? TAB_BG_HOVER : TAB_BG);
        int hi = selected ? TAB_HI_SELECTED : TAB_HI;
        int lo = selected ? TAB_LO_SELECTED : TAB_LO;
        guiGraphics.fill(x, y, x + w, y + h, COVER_EDGE);
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + 3, hi);
        guiGraphics.fill(x + 1, y + 1, x + 3, y + h - 1, hi);
        guiGraphics.fill(x + 1, y + h - 3, x + w - 1, y + h - 1, lo);
        guiGraphics.fill(x + w - 3, y + 1, x + w - 1, y + h - 1, lo);
        if (selected) {
            guiGraphics.outline(x, y, w, h, COVER_EDGE);
        }
    }

    /** 书页内容：左右两页分别绘制，并用裁剪框住溢出内页的文字/图片 */
    private void drawPages(GuiGraphicsExtractor guiGraphics) {
        if (pages.isEmpty()) return;
        PageContent page = pages.get(currentPage);
        if (!page.leftElements.isEmpty()) {
            guiGraphics.enableScissor(leftAreaX, leftAreaY, leftAreaX + leftAreaW, leftAreaY + leftAreaH);
            renderElements(guiGraphics, page.leftElements, leftAreaX, leftAreaY, leftAreaW);
            guiGraphics.disableScissor();
        }
        if (!page.rightElements.isEmpty()) {
            guiGraphics.enableScissor(rightAreaX, rightAreaY, rightAreaX + rightAreaW, rightAreaY + rightAreaH);
            renderElements(guiGraphics, page.rightElements, rightAreaX, rightAreaY, rightAreaW);
            guiGraphics.disableScissor();
        }
    }

    /** 封面底部：默认显示笔记标题，悬停选项卡时显示该选项卡全名；内页底部居中显示页码 */
    private void drawFooter(GuiGraphicsExtractor guiGraphics, int hoveredParent, int hoveredChild) {
        String hovered = null;
        if (hoveredChild >= 0 && hoveredChild < currentChildTabs.size()) {
            hovered = translateName(currentChildTabs.get(hoveredChild).name);
        } else if (hoveredParent >= 0 && hoveredParent < parentTabs.size()) {
            hovered = translateName(parentTabs.get(hoveredParent).name);
        }
        String titleText = (hovered != null && !hovered.isEmpty())
                ? hovered
                : (this.title == null ? "" : this.title.getString());

        int lineY = innerY + INNER_H + 12;
        guiGraphics.fill(outerX + 24, lineY - 5, outerX + OUTER_W - 24, lineY - 4, COVER_LINE);
        guiGraphics.centeredText(font, fit(font, titleText, OUTER_W - 80), outerX + OUTER_W / 2, lineY, TITLE_TEXT);

        if (!pages.isEmpty()) {
            String pageStr = (currentPage + 1) + "/" + pages.size();
            guiGraphics.centeredText(font, pageStr, innerX + INNER_W / 2,
                    innerY + INNER_H - font.lineHeight - 3, PAGE_TEXT_DIM);
        }
    }

    /** 选项卡宽度有限：超宽的名称按像素截断并加省略号 */
    private static String fit(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty()) return "";
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - 6)) + "…";
    }

    private void renderElements(GuiGraphicsExtractor guiGraphics, List<RenderElement> elements, int baseX, int baseY, int areaWidth) {
        int yOffset = 0;
        for (RenderElement e : elements) {
            if (e instanceof TextLine text) {
                // 书页是浅色羊皮纸：正文用深色墨水，字符串里的 §0 等颜色代码仍然生效
                guiGraphics.text(font, text.formatted, baseX, baseY + yOffset, PAGE_TEXT);
                yOffset += font.lineHeight;
            } else if (e instanceof ImageElement img) {
                Identifier tex = img.texture;
                int drawW = Math.max(1, Math.min(img.width, areaWidth));
                int drawH = img.width <= 0 ? img.height : Math.max(1, img.height * drawW / img.width);
                boolean valid = false;
                try {
                    Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(tex);
                    if (resource.isPresent()) {
                        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex,
                                baseX + (areaWidth - drawW) / 2, baseY + yOffset, 0.0F, 0.0F,
                                drawW, drawH, img.width, img.height);
                        valid = true;
                    }
                } catch (Exception ignored) {}
                if (!valid) {
                    guiGraphics.text(font, "Texture missing", baseX, baseY + yOffset, PAGE_TEXT_WARN);
                }
                yOffset += drawH;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {

        NeoForge.EVENT_BUS.unregister(eventListener);
        super.removed();
    }


    private interface RenderElement {
        int getHeight(Font font);
    }

    private static class TextLine implements RenderElement {
        final FormattedCharSequence formatted;
        TextLine(FormattedCharSequence formatted) { this.formatted = formatted; }
        @Override
        public int getHeight(Font font) { return font.lineHeight; }
    }

    private static class ImageElement implements RenderElement {
        final Identifier texture;
        final int width, height;
        ImageElement(Identifier tex, int w, int h) { this.texture = tex; this.width = w; this.height = h; }
        @Override
        public int getHeight(Font font) { return height; }
    }

    private static class PageContent {
        final List<RenderElement> leftElements;
        final List<RenderElement> rightElements;
        PageContent(List<RenderElement> left, List<RenderElement> right) {
            this.leftElements = left;
            this.rightElements = right;
        }
        static PageContent empty() {
            return new PageContent(List.of(), List.of());
        }
    }

    private static class ImageButton extends Button {
        private final Identifier texture;
        private final int texWidth, texHeight;

        public ImageButton(int x, int y, int width, int height,
                           Identifier texture, int texWidth, int texHeight,
                           OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.texture = texture;
            this.texWidth = texWidth;
            this.texHeight = texHeight;
        }

        // 26.1.2: AbstractButton#renderWidget was replaced by the extraction hook extractContents.
        @Override
        protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), 0.0F, 0.0F, width, height, texWidth, texHeight);
        }
    }
}