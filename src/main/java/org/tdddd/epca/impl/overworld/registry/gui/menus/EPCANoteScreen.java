package org.tdddd.epca.impl.overworld.registry.gui.menus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import org.tdddd.epca.impl.overworld.data.EPCANoteTabData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.tdddd.epca.impl.epca;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public class EPCANoteScreen extends Screen {

    private static final ResourceLocation OUTER_FRAME = new ResourceLocation(epca.MODID, "textures/gui/epca_note/outer_frame.png");
    private static final ResourceLocation INNER_FRAME = new ResourceLocation(epca.MODID, "textures/gui/epca_note/inner_frame.png");
    private static final ResourceLocation PARENT_TAB = new ResourceLocation(epca.MODID, "textures/gui/epca_note/parent_tab.png");
    private static final ResourceLocation PARENT_TAB_SELECTED = new ResourceLocation(epca.MODID, "textures/gui/epca_note/parent_tab_selected.png");
    private static final ResourceLocation CHILD_TAB = new ResourceLocation(epca.MODID, "textures/gui/epca_note/child_tab.png");
    private static final ResourceLocation CHILD_TAB_SELECTED = new ResourceLocation(epca.MODID, "textures/gui/epca_note/child_tab_selected.png");
    private static final ResourceLocation BTN_UP = new ResourceLocation(epca.MODID, "textures/gui/epca_note/button_up.png");
    private static final ResourceLocation BTN_DOWN = new ResourceLocation(epca.MODID, "textures/gui/epca_note/button_down.png");
    private static final ResourceLocation BTN_LEFT = new ResourceLocation(epca.MODID, "textures/gui/epca_note/button_left.png");
    private static final ResourceLocation BTN_RIGHT = new ResourceLocation(epca.MODID, "textures/gui/epca_note/button_right.png");
    private static final ResourceLocation PAGE_BTN_LEFT = new ResourceLocation(epca.MODID, "textures/gui/epca_note/page_btn_left.png");
    private static final ResourceLocation PAGE_BTN_RIGHT = new ResourceLocation(epca.MODID, "textures/gui/epca_note/page_btn_right.png");

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

    private static final int OUTER_TEX_W = 540;
    private static final int OUTER_TEX_H = 360;
    private static final int INNER_TEX_W = 420;
    private static final int INNER_TEX_H = 270;
    private static final int PARENT_TAB_TEX_W = 90;
    private static final int PARENT_TAB_TEX_H = 60;
    private static final int CHILD_TAB_TEX_W = 45;
    private static final int CHILD_TAB_TEX_H = 75;
    private static final int BTN_TEX_W = 24;
    private static final int BTN_TEX_H = 24;
    private static final int PAGE_BTN_TEX_SIZE = 18;

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

        MinecraftForge.EVENT_BUS.register(eventListener);
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
                elements.add(new ImageElement(new ResourceLocation(path), displaySize, displaySize));
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        float scale = getScaleFactor();
        var pose = guiGraphics.pose();
        pose.pushPose();

        // 计算转换后的鼠标坐标（用于可能传递给 super.render 的 tooltip 等）
        double layoutX = convertMouseX(mouseX);
        double layoutY = convertMouseY(mouseY);

        if (scale != 1.0f) {
            pose.translate(width / 2.0f, height / 2.0f, 0);
            pose.scale(scale, scale, 1.0f);
            pose.translate(-width / 2.0f, -height / 2.0f, 0);
        }

        RenderSystem.setShaderTexture(0, OUTER_FRAME);
        guiGraphics.blit(OUTER_FRAME, outerX, outerY, 0, 0, OUTER_W, OUTER_H, OUTER_TEX_W, OUTER_TEX_H);


        RenderSystem.setShaderTexture(0, INNER_FRAME);
        guiGraphics.blit(INNER_FRAME, innerX, innerY, 0, 0, INNER_W, INNER_H, INNER_TEX_W, INNER_TEX_H);


        for (int i = 0; i < MAX_PARENT_VISIBLE; i++) {
            int idx = parentScrollOffset + i;
            if (idx >= parentTabs.size()) break;
            EPCANoteTabData.ParentTab tab = parentTabs.get(idx);
            int y = parentListStartY + i * PARENT_TAB_H;
            ResourceLocation tex = (selectedParentIndex == idx) ? PARENT_TAB_SELECTED : PARENT_TAB;
            RenderSystem.setShaderTexture(0, tex);
            guiGraphics.blit(tex, parentListStartX, y, 0, 0, PARENT_TAB_W, PARENT_TAB_H, PARENT_TAB_TEX_W, PARENT_TAB_TEX_H);
            String displayName = translateName(tab.name);
            int textWidth = font.width(displayName);
            guiGraphics.drawString(font, displayName,
                    parentListStartX + (PARENT_TAB_W - textWidth) / 2,
                    y + (PARENT_TAB_H - 8) / 2,
                    0xFFFFFF);
        }


        int visibleCount = Math.min(currentChildTabs.size() - childScrollOffset, MAX_CHILD_VISIBLE);
        int totalWidth = visibleCount * CHILD_TAB_W;
        int startX = innerX + (INNER_W - totalWidth) / 2;
        for (int i = 0; i < visibleCount; i++) {
            int idx = childScrollOffset + i;
            EPCANoteTabData.ChildTab child = currentChildTabs.get(idx);
            int x = startX + i * CHILD_TAB_W;
            ResourceLocation tex = (selectedChildIndex == idx) ? CHILD_TAB_SELECTED : CHILD_TAB;
            RenderSystem.setShaderTexture(0, tex);
            guiGraphics.blit(tex, x, childListStartY, 0, 0, CHILD_TAB_W, CHILD_TAB_H, CHILD_TAB_TEX_W, CHILD_TAB_TEX_H);
            String displayName = translateName(child.name);
            int textWidth = font.width(displayName);
            guiGraphics.drawString(font, displayName,
                    x + (CHILD_TAB_W - textWidth) / 2,
                    childListStartY + 10,
                    0xFFFFFF);
        }


        if (!pages.isEmpty()) {
            PageContent page = pages.get(currentPage);
            renderElements(guiGraphics, page.leftElements, leftAreaX, leftAreaY);
            renderElements(guiGraphics, page.rightElements, rightAreaX, rightAreaY);
        }


        String pageStr = (currentPage + 1) + "/" + pages.size();
        int pageStrWidth = font.width(pageStr);
        int pageX = innerX + (INNER_W - pageStrWidth) / 2;
        int pageY = innerY + INNER_H - font.lineHeight - 2;
        guiGraphics.drawString(font, pageStr, pageX, pageY, 0xCCCCCC);

        super.render(guiGraphics, (int) layoutX, (int) layoutY, partialTick);

        pose.popPose();
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double layoutX = convertMouseX(mouseX);
        double layoutY = convertMouseY(mouseY);

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

        // 传递给父类以处理按钮等组件
        return super.mouseClicked(layoutX, layoutY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        double layoutX = convertMouseX(mouseX);
        double layoutY = convertMouseY(mouseY);
        super.mouseMoved(layoutX, layoutY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double layoutX = convertMouseX(mouseX);
        double layoutY = convertMouseY(mouseY);
        return super.mouseDragged(layoutX, layoutY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double layoutX = convertMouseX(mouseX);
        double layoutY = convertMouseY(mouseY);
        return super.mouseReleased(layoutX, layoutY, button);
    }

    private void renderElements(GuiGraphics guiGraphics, List<RenderElement> elements, int baseX, int baseY) {
        int yOffset = 0;
        for (RenderElement e : elements) {
            if (e instanceof TextLine text) {
                guiGraphics.drawString(font, text.formatted, baseX, baseY + yOffset, 0xFFFFFF);
                yOffset += font.lineHeight;
            } else if (e instanceof ImageElement img) {
                ResourceLocation tex = img.texture;
                boolean valid = false;
                try {
                    Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(tex);
                    if (resource.isPresent()) {
                        RenderSystem.setShaderTexture(0, tex);
                        guiGraphics.blit(tex, baseX, baseY + yOffset, 0, 0, img.width, img.height, img.width, img.height);
                        valid = true;
                    }
                } catch (Exception ignored) {}
                if (!valid) {
                    guiGraphics.drawString(font, "Texture missing", baseX, baseY + yOffset, 0xFF5555);
                }
                yOffset += img.height;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {

        MinecraftForge.EVENT_BUS.unregister(eventListener);
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
        final ResourceLocation texture;
        final int width, height;
        ImageElement(ResourceLocation tex, int w, int h) { this.texture = tex; this.width = w; this.height = h; }
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
        private final ResourceLocation texture;
        private final int texWidth, texHeight;

        public ImageButton(int x, int y, int width, int height,
                           ResourceLocation texture, int texWidth, int texHeight,
                           OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.texture = texture;
            this.texWidth = texWidth;
            this.texHeight = texHeight;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            RenderSystem.setShaderTexture(0, texture);
            guiGraphics.blit(texture, getX(), getY(), 0, 0, width, height, texWidth, texHeight);
        }
    }
}