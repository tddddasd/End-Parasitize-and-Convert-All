package org.tdddd.epca.impl.overworld.registry.gui.menus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.tdddd.epca.impl.overworld.data.EPCANoteBookmarkColors;
import org.tdddd.epca.impl.overworld.data.EPCANoteTabData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.tdddd.epca.impl.epca;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 「虫咒笔记」界面（26.1.2 版）。
 *
 * 本文件是 1.20.1 分支 epca 工程 EPCANoteScreen 的等价移植：书体 / 页签几何、颜色、亮度、
 * 绘制顺序、分页规则全部逐行照搬 1.20.1 的权威实现，只在渲染 API 上做 26.1.2 的等价替换
 * （详见下方 “26.1.2 渲染 API 差异” 注释块）。页面内容解析（$[page]$、${img:...}$）保持
 * 26 侧原有实现不变。
 */
@OnlyIn(Dist.CLIENT)
public class EPCANoteScreen extends Screen {

    // ======================= 贴图测量（para_curse_note.png, 256x256, 程序逐像素扫描）=======================
    //
    //  书体（书皮 + 左右两页已经合并为整张图）非透明外框:
    //      x 45..197, y 35..141   ->  (45, 35, 153, 107)
    //  左页纸面: x 55..117, y 40..124   ->  内容框四周内缩 4px: (59, 44, 55, 78)
    //  右页纸面: x 125..187, y 40..124  ->  内容框四周内缩 4px: (129, 44, 55, 78)
    //  中间书脊（装订环）: x 118..124，正好落在左右内容框之间 15px 的空白里
    //  上方竖书签: x 68..77, y 1..18    ->  (68, 1, 10, 18)
    //  左侧横书签: x 1..26,  y 45..57   ->  (1, 45, 26, 13)
    //
    //  书体 / 两个内容框使用同一个整数像素比例（3x）。
    //  两个书签不再是装饰，而是真正的页签底板，按「整数倍、保持宽高比」单独放大到能容纳文字：
    //      左侧横书签 (1,45,26,13)  -> 父级页签，4x = 104x52，尖角朝左
    //      上方竖书签 (68,1,10,18)  -> 子级页签，3x = 30x54 起，按字数继续等比加高
    //
    //  绘制顺序（自下而上）：上方子级页签 -> 书体 -> 左侧父级页签 -> 页签文字 -> 页内内容 / 页脚。
    //      * 子级页签画在书体之下：下端压进书体的 6px 被书体盖住，像夹在书页里；
    //      * 父级页签画在书体之上：整块书签形状完整可见（右端压在书皮上的一小段也露出来），
    //        但右端止步于书皮，绝不进入左页内容区。
    //  布局：
    //      * 父级页签：左侧一列，右端压进书皮，整列再右移 PARENT_TAB_SHIFT_RIGHT 靠近书体；
    //      * 子级页签：书体上方**永远只有一排** 4+4 两簇，左簇对齐左页纸面、右簇对齐右页纸面，
    //                  簇内水平间隔 3px，簇整体在纸面内容框里水平居中；
    //                  一页固定 8 个位置（第 1 页索引 0~7、第 2 页 8~15 ……），
    //                  第 9 个及以后翻页显示在**同样的 1~8 位置**上，绝不往上叠第二排；
    //      * 子级翻页按钮（page_btn_left/right，18x18）：子页签 > 8 个时必现在子页签行两端，
    //                  垂直居中于这一排；≤8 个（单排放得下）时隐藏；首页左键禁用、末页右键禁用。
    //
    // ======================= 26.1.2 渲染 API 差异（相对 1.20.1）=======================
    //
    //  1) GuiGraphics -> GuiGraphicsExtractor：26 的界面不在 render() 里直接下发绘制，
    //     而是把绘制指令录进 GuiRenderState（extractRenderState），所以本类覆写
    //     extractRenderState(GuiGraphicsExtractor, int, int, float) 而非 render(GuiGraphics, ...)。
    //     fill / text / blit / enableScissor 的语义与 1.20.1 相同，只是签名变了。
    //  2) ResourceLocation -> Identifier：Identifier.fromNamespaceAndPath(ns, path)，
    //     以及 ${img:...}$ 里的裸字符串用 Identifier.parse(path)。
    //  3) RenderSystem.setShaderColor 已在新渲染管线里移除（RenderSystem 只剩 setShaderFog/Lights）。
    //     26 的等价做法是给 blit 传顶点色 ARGB：blit(..., int color) 会把 color 直接写进
    //     position_tex_color 顶点（BlitRenderState#buildVertices 里 setColor(color)），
    //     GUI_TEXTURED 管线本身就是 BlendFunction.TRANSLUCENT，所以「颜色乘算 + alpha 半透明」
    //     与 1.20.1 的 setShaderColor(r,g,b,alpha) 完全等价，且不再需要手动 enableBlend/
    //     defaultBlendFunc/setShaderColor(1,1,1,1) 复位（顶点色天然局部生效，不会污染后续绘制）。
    //  4) pose：26 的 guiGraphics.pose() 返回 JOML 的 Matrix3x2fStack（不是 PoseStack），
    //     所以 pushPose/popPose -> pushMatrix/popMatrix，translate(x,y,z) -> translate(x,y)，
    //     scale(s,s,1) -> scale(s,s)。
    //  5) enableScissor：26 的 GuiGraphicsExtractor#enableScissor 内部会自己做
    //     new ScreenRectangle(x0,y0,x1-x0,y1-y0).transformAxisAligned(this.pose)，
    //     也就是说裁剪框会自动跟随 pose 的缩放/平移。1.20.1 需要手动把裁剪框换算到屏幕坐标，
    //     在 26 上反而必须传「布局坐标」，否则会被缩放两次（这是最容易移植错的一处）。
    //  6) drawString/centeredString -> text/centeredText（默认都带阴影，与 1.20.1 默认一致）；
    //     font.width / font.lineHeight / font.split / font.plainSubstrByWidth 未变。
    //  7) Component.translatable / FormattedText.of / FormattedCharSequence 未变。
    private static final Identifier NOTE_TEXTURE =
            Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/epca_note/para_curse_note.png");
    private static final int TEX_SIZE = 256;

    // 书体外框（贴图坐标）
    private static final int BOOK_TEX_X = 45;
    private static final int BOOK_TEX_Y = 35;
    private static final int BOOK_TEX_W = 153;
    private static final int BOOK_TEX_H = 107;

    // 左页内容框（贴图坐标）
    private static final int LEFT_PAGE_TEX_X = 59;
    private static final int LEFT_PAGE_TEX_Y = 44;
    private static final int LEFT_PAGE_TEX_W = 55;
    private static final int LEFT_PAGE_TEX_H = 78;

    // 右页内容框（贴图坐标）
    private static final int RIGHT_PAGE_TEX_X = 129;
    private static final int RIGHT_PAGE_TEX_Y = 44;

    // 统一像素比例
    private static final int PIXEL_SCALE = 3;

    private static final int BOOK_W = BOOK_TEX_W * PIXEL_SCALE;                        // 459
    private static final int BOOK_H = BOOK_TEX_H * PIXEL_SCALE;                        // 321
    private static final int PAGE_W = LEFT_PAGE_TEX_W * PIXEL_SCALE;                   // 165
    private static final int PAGE_H = LEFT_PAGE_TEX_H * PIXEL_SCALE;                   // 234
    private static final int PAGE_OFF_X = (LEFT_PAGE_TEX_X - BOOK_TEX_X) * PIXEL_SCALE;        // 42
    private static final int PAGE_OFF_Y = (LEFT_PAGE_TEX_Y - BOOK_TEX_Y) * PIXEL_SCALE;        // 27
    private static final int RIGHT_PAGE_OFF_X = (RIGHT_PAGE_TEX_X - BOOK_TEX_X) * PIXEL_SCALE; // 252

    // ======================= 页签（书签底板）=======================
    //
    // 父级页签（左侧一列）：贴图左侧横书签 (1,45,26,13)，4x 等比放大 -> 104x52。
    //   * 26:13 = 2:1 的宽高比保持不变（w = 26*4, h = 13*4），不做任何拉伸；
    //   * 高 52px >= 24px，足够容纳 3~4 个汉字（文字横排）；
    //   * 尖角在左（指向远离书体的一侧），右端压在书皮上：画在书体**之后**，
    //     所以整块书签完整可见；整列再向右移 PARENT_TAB_SHIFT_RIGHT(=18)，
    //     让左侧尖端不要离书体太远。
    //   * 右端位置 = bookX + TUCK + SHIFT_RIGHT = bookX + 24，
    //     而左页内容框从 bookX + 42 才开始，两者之间还剩 18px，绝不会压住书页正文。
    //   * 同一列所有父级页签共用 PARENT_TAB_SCALE 这一个缩放。
    //
    // 子级页签（书体上方）：贴图上方竖书签 (68,1,10,18)，基准 3x -> 30x54。
    //   * w:h = 10:18 保持不变，按字数用整数倍继续等比加高（文字竖排，逐字符一行）；
    //   * V 形缺口朝上（指向远离书体的一侧），下端 6px 压进书体顶部（画在书体之下，被压住）；
    //   * 从左到右按数据顺序排布：这一排里左簇 4 个对齐左页纸面、右簇 4 个对齐右页纸面，
    //     簇内水平间隔 3px，簇整体在纸面内容框里水平居中；
    //   * 永远只有一排 8 个位置（不再往上叠第二排），第 9 个及以后靠左右按钮整页切换；
    //     一页 8 个：第 1 页索引 0~7、第 2 页 8~15 ……，所以第 9 个（索引 8）
    //     出现在第 1 页第 1 个的位置（左簇第 1 格），而不是上方第二排。
    //   * 可见位置只由「绝对索引 - childScrollOffset」决定：
    //     cluster = (local % 8) / 4、slot = local % 4、row 恒为 0；
    //     簇宽 childClusterWidth 只统计当前页，因此每页都严格对齐左右纸面。
    private static final int PARENT_TAB_TEX_X = 1;
    private static final int PARENT_TAB_TEX_Y = 45;
    private static final int PARENT_TAB_TEX_W = 26;
    private static final int PARENT_TAB_TEX_H = 13;
    private static final int PARENT_TAB_SCALE = 4;                                   // 4x 等比放大
    private static final int PARENT_TAB_W = PARENT_TAB_TEX_W * PARENT_TAB_SCALE;     // 104
    private static final int PARENT_TAB_H = PARENT_TAB_TEX_H * PARENT_TAB_SCALE;     // 52
    private static final int PARENT_TAB_GAP = 4;                                     // 同一列页签间距
    private static final int PARENT_TAB_TUCK = 6;                                    // 右端压进书皮的像素
    private static final int PARENT_TAB_SHIFT_RIGHT = 18;                            // 整列整体右移的像素
    /** 父级书签右端到左页内容框左边界的净间距（书签在书体之上，必须保持为正） */
    private static final int PARENT_TAB_RIGHT_END_TO_PAGE =
            PAGE_OFF_X - (PARENT_TAB_TUCK + PARENT_TAB_SHIFT_RIGHT);                 // 42 - 24 = 18
    // 父级书签是「左右两端都收尖」的形状（贴图 26x13 逐像素扫描：左尖 6px、右尖 5px），
    // 文字必须让开两端的尖角，否则 5 个汉字（45px）会被顶到右端的斜边上，看起来又挤又越界。
    // 4x 放大后左让 24、右让 20，正文区 60px 居中落在书签实体部分（24..84）上，
    // 一行放得下 5~6 个汉字；更长的名字由 font.split 折行，绝不画到书签外面。
    private static final int PARENT_TAB_TEXT_INSET_L = 24;                           // 让开左侧尖角 (6*4)
    private static final int PARENT_TAB_TEXT_INSET_R = 20;                           // 让开右侧尖角 (5*4)
    // 正文区宽度 = 104 - 24 - 20 = 60px（5 个汉字 45px、6 个汉字 54px 都能一行放下）

    private static final int CHILD_TAB_TEX_X = 68;
    private static final int CHILD_TAB_TEX_Y = 1;
    private static final int CHILD_TAB_TEX_W = 10;
    private static final int CHILD_TAB_TEX_H = 18;
    private static final int CHILD_TAB_MIN_SCALE = 3;                                // 基准 3x -> 30x54
    private static final int CHILD_TAB_MAX_SCALE = 8;                                // 超长名称也保持完整可见
    private static final int CHILD_TAB_PER_CLUSTER = 4;                              // 每簇最多 4 个
    private static final int CHILD_TAB_CLUSTERS = 2;                                 // 左页上方 / 右页上方
    private static final int CHILD_TAB_PER_ROW = CHILD_TAB_PER_CLUSTER * CHILD_TAB_CLUSTERS; // 一排 8 个
    /** 一页就是这一排的 8 个位置：第 9 个及以后翻页显示在同样的 1~8 位置上（不再叠第二排） */
    private static final int CHILD_TAB_PAGE_SIZE = CHILD_TAB_PER_ROW;                  // 8
    private static final int CHILD_TAB_GAP = 3;                                      // 同一簇内水平间隔 3px
    private static final int CHILD_TAB_TUCK = 6;                                     // 下端压进书体的像素
    private static final int CHILD_PAGE_BTN_SIZE = 18;                               // 与 page_btn_left/right.png 贴图同尺寸（18x18，1:1）
    private static final int CHILD_PAGE_BTN_GAP = 5;                                 // 翻页按钮与最近一簇书签的水平间隔
    private static final int CHILD_TAB_TEXT_PAD_V = 4;                               // 竖排文字上下留白
    private static final int CHILD_TAB_NOTCH_TEX = 3;                                // 顶部 V 形缺口深度（贴图像素）

    // GUI 整体尺寸（书体在 OUTER 内居中）
    private static final int BOOK_MARGIN_X = 60;
    private static final int BOOK_MARGIN_TOP = 58;                                    // 容纳 54px 高的子级页签（更长名称靠 childOverhangTop 计入自适应缩放）
    private static final int BOOK_MARGIN_BOTTOM = 36;
    private static final int OUTER_W = BOOK_W + BOOK_MARGIN_X * 2;                    // 579
    private static final int OUTER_H = BOOK_MARGIN_TOP + BOOK_H + BOOK_MARGIN_BOTTOM; // 415

    private static final int BUTTON_W = 24;
    private static final int BUTTON_H = 24;
    private static final int PAGE_BTN_SIZE = 18;

    // 父级页签从书体左侧探出的量（自适应缩放时算进整幅界面，见 getScaleFactor）
    // 右移 PARENT_TAB_SHIFT_RIGHT 后探出量随之减少，否则缩放过早触发
    private static final int PARENT_OVERHANG_LEFT =
            Math.max(0, PARENT_TAB_W - PARENT_TAB_TUCK - PARENT_TAB_SHIFT_RIGHT - BOOK_MARGIN_X); // 20

    private static final Identifier BTN_UP = Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/epca_note/button_up.png");
    private static final Identifier BTN_DOWN = Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/epca_note/button_down.png");
    private static final Identifier PAGE_BTN_LEFT = Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/epca_note/page_btn_left.png");
    private static final Identifier PAGE_BTN_RIGHT = Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/epca_note/page_btn_right.png");

    private static final int BTN_TEX_W = 24;
    private static final int BTN_TEX_H = 24;
    private static final int PAGE_BTN_TEX_SIZE = 18;

    // 页签文字：底色亮用深字、底色暗用浅字（按配置颜色自动选择，深浅书签上都清晰）
    private static final int TAB_TEXT_ON_LIGHT = 0xFF1A0D05;
    private static final int TAB_TEXT_ON_DARK = 0xFFFFF4DC;

    // 页签明暗：未选中为基准亮度、悬停略亮、选中最亮（选中态与位置无关，不能只靠位置区分）
    private static final float TAB_BRIGHTNESS_NORMAL = 1.00F;
    private static final float TAB_BRIGHTNESS_HOVER = 1.12F;
    private static final float TAB_BRIGHTNESS_SELECTED = 1.34F;

    /**
     * 书签整体亮度增益：只作用于书签贴图的顶点色乘数（书体 / 文字 / 页面内容都不受影响）。
     *
     * 叠加公式：最终顶点色 = clamp01(配置色 × 本增益 × 状态亮度(1.00 / 1.12 / 1.34))，
     * 逐通道 clamp 到 [0,1]（等价于 clamp 到 255 再 /255），所以配置色再亮也不会溢出。
     * 注意不去改 EPCANoteBookmarkColors 里的 16 色表，对 bookmarks.left / bookmarks.top
     * 以及用户自定义颜色一律生效。
     */
    private static final float BOOKMARK_BRIGHTNESS_GAIN = 1.30F;

    // 半透明白色高光（沿书签形状叠加，不是矩形色块）
    // 提亮 30% 后底色本身更亮，高光略微上调（各 +0.02 / +0.03）以保持选中 / 悬停的辨识度
    private static final float TAB_SHEEN_HOVER = 0.08F;
    private static final float TAB_SHEEN_SELECTED = 0.18F;

    private static final int PAGE_TEXT = 0xFF3A2412;
    private static final int PAGE_TEXT_DIM = 0xFF6B4A2E;
    private static final int PAGE_TEXT_WARN = 0xFF8C2F12;
    private static final int TITLE_TEXT = 0xFFE7D3A6;

    private static final int MAX_PARENT_VISIBLE = 4;
    /** 子级页签一页只显示一排 8 个位置（翻页复用这 8 个位置，不存在第二排） */
    private static final int MAX_CHILD_VISIBLE = CHILD_TAB_PAGE_SIZE;                 // 8

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
    private int bookX, bookY;
    private int parentListStartX, parentListStartY;
    private int childTabsBottomY;
    private int leftAreaX, leftAreaY, leftAreaW, leftAreaH;
    private int rightAreaX, rightAreaY, rightAreaW, rightAreaH;

    private float getScaleFactor() {
        int guiScale = (int) Minecraft.getInstance().getWindow().getGuiScale();

        // 书体 + 页签整体（含左侧/上方探出部分）放不下时等比缩小，避免被屏幕裁掉
        double halfW = OUTER_W / 2.0 + PARENT_OVERHANG_LEFT;
        double halfH = OUTER_H / 2.0 + childOverhangTop();
        float fitW = (width / 2.0f - 4.0f) / (float) halfW;
        float fitH = (height / 2.0f - 4.0f) / (float) halfH;
        float fit = Math.min(1.0f, Math.min(fitW, fitH));

        if (guiScale == 3) {
            fit = Math.min(fit, 0.65f);
        }

        // 5% 粒度取整，避免出现过于奇怪的缩放比
        fit = (float) (Math.floor(fit * 20.0) / 20.0);
        return Math.max(0.4f, fit);
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

        // 书签颜色配置：每次打开界面检查一次文件修改时间（改完配置重新打开笔记即可生效）
        EPCANoteBookmarkColors.loadIfChanged();

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
        bookX = outerX + BOOK_MARGIN_X;
        bookY = outerY + BOOK_MARGIN_TOP;

        // 内容区 = 贴图里左右两页的纸面矩形 * PIXEL_SCALE（文字只画在这里面）
        leftAreaX = bookX + PAGE_OFF_X;
        leftAreaY = bookY + PAGE_OFF_Y;
        leftAreaW = PAGE_W;
        leftAreaH = PAGE_H;
        rightAreaX = bookX + RIGHT_PAGE_OFF_X;
        rightAreaY = leftAreaY;
        rightAreaW = PAGE_W;
        rightAreaH = PAGE_H;

        // 父级页签：右端压进书皮 PARENT_TAB_TUCK 像素，整列再右移 PARENT_TAB_SHIFT_RIGHT
        // （让左侧尖端更靠近书体），整列在书体高度内居中
        parentListStartX = bookX + PARENT_TAB_TUCK + PARENT_TAB_SHIFT_RIGHT - PARENT_TAB_W;
        parentListStartY = bookY + (BOOK_H - parentColumnHeight()) / 2;

        // 子级页签：只有一排，底边压进书体顶部 CHILD_TAB_TUCK 像素，高度由字数决定（向上生长）；
        // 顶部不越出 GUI，超出部分由 childOverhangTop() 计入自适应缩放
        childTabsBottomY = bookY + CHILD_TAB_TUCK;
    }

    private void rebuildUIComponents() {

        if (btnParentUp != null) removeWidget(btnParentUp);
        if (btnParentDown != null) removeWidget(btnParentDown);
        if (btnChildLeft != null) {
            removeWidget(btnChildLeft);
            btnChildLeft = null;
        }
        if (btnChildRight != null) {
            removeWidget(btnChildRight);
            btnChildRight = null;
        }
        if (btnPrevPage != null) removeWidget(btnPrevPage);
        if (btnNextPage != null) removeWidget(btnNextPage);

        if (parentTabs.size() > MAX_PARENT_VISIBLE) {
            int upY = parentListStartY - BUTTON_H - 5;
            int downY = parentListStartY + parentColumnHeight() + 5;
            int btnX = parentListStartX + PARENT_TAB_W / 2 - BUTTON_W / 2;
            btnParentUp = new ImageButton(btnX, upY, BUTTON_W, BUTTON_H, BTN_UP, BTN_TEX_W, BTN_TEX_H, b -> scrollParent(-1));
            btnParentDown = new ImageButton(btnX, downY, BUTTON_W, BUTTON_H, BTN_DOWN, BTN_TEX_W, BTN_TEX_H, b -> scrollParent(1));
            addRenderableWidget(btnParentUp);
            addRenderableWidget(btnParentDown);
        }

        // 翻页按钮：放在书体底部 21px 高的深棕色书脊带上（贴图 y=135..141 -> 书体内部 y=300..321）
        int btnPosXLeft = bookX + 10;
        int btnPosXRight = bookX + BOOK_W - 10 - PAGE_BTN_SIZE;
        int btnPosY = bookY + BOOK_H - PAGE_BTN_SIZE - 2;
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

    /**
     * 子级页签的整页翻页按钮（与 1.20.1 权威实现一致）。
     *
     * 显示条件：当前父级下的子页签总数 > 8（一排只放得下 8 个，多出来的只能翻页看），
     * 一排就放得下（≤8 个）时按钮隐藏。
     * 位置：书体上方子页签行的两端 —— 左簇左侧、右簇右侧，垂直居中于这一排书签；
     * 尺寸：直接用 page_btn_left/right.png 贴图的 18x18，1:1 不拉伸。
     * 分页：每页 8 个，offset 始终是 8 的倍数；offset == 0 时左键禁用、
     * offset + 8 >= 总数 时右键禁用，由 ImageButton 画成暗色。
     */
    private void updateChildButtons() {
        if (!showChildPaging()) {
            if (btnChildLeft != null) {
                removeWidget(btnChildLeft);
                btnChildLeft = null;
            }
            if (btnChildRight != null) {
                removeWidget(btnChildRight);
                btnChildRight = null;
            }
            return;
        }

        int btnY = childPageButtonY();
        int leftX = childPageButtonLeftX();
        int rightX = childPageButtonRightX();

        if (btnChildLeft == null || btnChildRight == null) {
            btnChildLeft = new ImageButton(leftX, btnY,
                    CHILD_PAGE_BTN_SIZE, CHILD_PAGE_BTN_SIZE,
                    PAGE_BTN_LEFT, PAGE_BTN_TEX_SIZE, PAGE_BTN_TEX_SIZE,
                    b -> scrollChild(-1));
            btnChildRight = new ImageButton(rightX, btnY,
                    CHILD_PAGE_BTN_SIZE, CHILD_PAGE_BTN_SIZE,
                    PAGE_BTN_RIGHT, PAGE_BTN_TEX_SIZE, PAGE_BTN_TEX_SIZE,
                    b -> scrollChild(1));
            addRenderableWidget(btnChildLeft);
            addRenderableWidget(btnChildRight);
        } else {
            // 翻页只移动 / 改禁用态，不动 widget 列表（避免在点击回调里增删组件）
            btnChildLeft.setX(leftX);
            btnChildLeft.setY(btnY);
            btnChildRight.setX(rightX);
            btnChildRight.setY(btnY);
        }

        refreshChildButtonState();
    }

    /** 子页签总数超过一排（8 个）就必须能看到翻页按钮，否则用户无从翻页 */
    private boolean showChildPaging() {
        return currentChildTabs.size() > CHILD_TAB_PAGE_SIZE;
    }

    private boolean canChildPagePrev() {
        return childScrollOffset > 0;
    }

    /** 按 8 分页：offset + 8 >= 总数 时已在最后一页，右键禁用 */
    private boolean canChildPageNext() {
        return childScrollOffset + CHILD_TAB_PAGE_SIZE < currentChildTabs.size();
    }

    /** 第一页 / 最后一页时禁用对应按钮（禁用态由 ImageButton 画成暗色） */
    private void refreshChildButtonState() {
        if (btnChildLeft != null) btnChildLeft.active = canChildPagePrev();
        if (btnChildRight != null) btnChildRight.active = canChildPageNext();
    }

    /** 翻页按钮的 Y：与这一排书签垂直居中对齐（排顶边 + (排高 - 按钮高) / 2） */
    private int childPageButtonY() {
        int rowH = childMaxHeight();
        if (rowH <= 0) rowH = CHILD_TAB_TEX_H * CHILD_TAB_MIN_SCALE;
        int rowTop = childTabsBottomY - rowH;
        return rowTop + (rowH - CHILD_PAGE_BTN_SIZE) / 2;
    }

    /** 左按钮：贴在左簇的左侧 */
    private int childPageButtonLeftX() {
        return childClusterStartX(0) - CHILD_PAGE_BTN_GAP - CHILD_PAGE_BTN_SIZE;
    }

    /** 右按钮：贴在右簇的右侧（右簇为空时贴住右页纸面右边界，保证仍在书体上方的右端） */
    private int childPageButtonRightX() {
        int columnW = childClusterWidth(1);
        int clusterRight = columnW > 0
                ? childClusterStartX(1) + columnW
                : rightAreaX + rightAreaW;
        return clusterRight + CHILD_PAGE_BTN_GAP;
    }

    private void scrollParent(int delta) {
        int maxOffset = Math.max(0, parentTabs.size() - MAX_PARENT_VISIBLE);
        parentScrollOffset = Math.max(0, Math.min(maxOffset, parentScrollOffset + delta));
    }

    private void scrollChild(int delta) {
        // 整页（8 个）翻动：偏移量始终是 CHILD_TAB_PAGE_SIZE 的整数倍，
        // 于是「哪一簇、簇内第几个」的规则每一页都一样；
        // 第 9 个及以后复用同样的 1~8 位置，绝不会再往上叠第二排
        int maxOffset = maxChildScrollOffset();
        int next = childScrollOffset + delta * CHILD_TAB_PAGE_SIZE;
        next = Math.max(0, Math.min(maxOffset, next));
        next = Math.min(next, Math.max(0, currentChildTabs.size() - 1));   // 夹在 [0, size-1] 内
        childScrollOffset = next;
        // 翻页后这一排的内容可能变了（末页不满 8 个），按钮位置与禁用态都要跟着刷新
        updateChildButtons();
    }

    /** 最后一个 8 的整数倍偏移（末页不满 8 个也仍然从该页第 1 个位置开始显示） */
    private int maxChildScrollOffset() {
        if (currentChildTabs.isEmpty()) return 0;
        return ((currentChildTabs.size() - 1) / CHILD_TAB_PAGE_SIZE) * CHILD_TAB_PAGE_SIZE;
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

    // ======================= 页签几何（绘制与点击判定共用的唯一来源）=======================

    /** 当前可见的父级页签数量 */
    private int visibleParentCount() {
        return Math.max(0, Math.min(parentTabs.size() - parentScrollOffset, MAX_PARENT_VISIBLE));
    }

    /** 父级页签整列高度（含间距），用于整列居中 */
    private int parentColumnHeight() {
        int n = Math.max(0, Math.min(parentTabs.size(), MAX_PARENT_VISIBLE));
        if (n == 0) return 0;
        return n * PARENT_TAB_H + (n - 1) * PARENT_TAB_GAP;
    }

    /** 父级页签的 y：同一列同一尺寸（缩放固定 PARENT_TAB_SCALE），只按行号偏移 */
    private int parentTabY(int absIndex) {
        return parentListStartY + (absIndex - parentScrollOffset) * (PARENT_TAB_H + PARENT_TAB_GAP);
    }

    /** 当前可见的子级页签数量 */
    private int visibleChildCount() {
        if (currentChildTabs.isEmpty()) return 0;
        return Math.max(0, Math.min(currentChildTabs.size() - childScrollOffset, MAX_CHILD_VISIBLE));
    }

    /**
     * 子级页签的缩放倍数：按竖排字数决定所需高度（宽高比 10:18 保持不变），
     * 并预留顶部 V 形缺口与下端压进书体的部分，保证文字既不压缺口也不被书体吃掉。
     * 2~3 个字用基准 3x（30x54），4~5 个字 4x，6 个字 5x …… 上限 8x。
     * 全部取整数倍：贴图像素保持锐利，且只等比放大、绝不拉伸变形。
     */
    private int childTabScale(int absIndex) {
        if (absIndex < 0 || absIndex >= currentChildTabs.size()) return CHILD_TAB_MIN_SCALE;
        int chars = Math.max(1, translateName(currentChildTabs.get(absIndex).name).length());
        int needVisible = chars * lineHeight() + CHILD_TAB_TEXT_PAD_V * 2;
        // 可见高度 = 18*scale - TUCK，扣掉缺口 3*scale 后仍要容得下文字：15*scale >= needVisible + TUCK
        int span = CHILD_TAB_TEX_H - CHILD_TAB_NOTCH_TEX; // 15
        int scale = (needVisible + CHILD_TAB_TUCK + span - 1) / span;
        return Math.max(CHILD_TAB_MIN_SCALE, Math.min(CHILD_TAB_MAX_SCALE, scale));
    }

    private int childTabWidth(int absIndex) {
        return CHILD_TAB_TEX_W * childTabScale(absIndex);
    }

    private int childTabHeight(int absIndex) {
        return CHILD_TAB_TEX_H * childTabScale(absIndex);
    }

    /** 子级页签在当前页里的可见槽位（0..7）：由绝对索引减去 childScrollOffset 得到 */
    private int childLocalIndex(int absIndex) {
        return absIndex - childScrollOffset;
    }

    /** 哪一簇：0 = 左页纸面上方，1 = 右页纸面上方（每页 8 个位置：前 4 个左簇、后 4 个右簇） */
    private int childClusterOf(int absIndex) {
        return (childLocalIndex(absIndex) % CHILD_TAB_PAGE_SIZE) / CHILD_TAB_PER_CLUSTER;
    }

    /** 簇内第几个（0 起）：同一簇里从左到右的先后 */
    private int childSlotOf(int absIndex) {
        return childLocalIndex(absIndex) % CHILD_TAB_PER_CLUSTER;
    }

    /** 某簇在当前页里实际有几个：不足 8 个时左簇先占满，剩下的进右簇 */
    private int childClusterCount(int cluster) {
        int count = 0;
        int visible = visibleChildCount();
        for (int i = 0; i < visible; i++) {
            if (childClusterOf(childScrollOffset + i) == cluster) count++;
        }
        return count;
    }

    /** 某簇的整簇宽度（含簇内 CHILD_TAB_GAP=3px 的水平间隔） */
    private int childClusterWidth(int cluster) {
        int count = childClusterCount(cluster);
        if (count == 0) return 0;
        int total = (count - 1) * CHILD_TAB_GAP;
        int visible = visibleChildCount();
        for (int i = 0; i < visible; i++) {
            int idx = childScrollOffset + i;
            if (childClusterOf(idx) == cluster) total += childTabWidth(idx);
        }
        return total;
    }

    /**
     * 某簇的起始 X：整簇宽度在左右页纸面内容框（leftAreaX/leftAreaW、
     * rightAreaX/rightAreaW）里水平居中。左簇压着左页、右簇压着右页，与书页纸面严格对应。
     * 只有一排 8 个位置，所以不再需要「多排共用列网格」的折中：每页都严格按当前页居中。
     */
    private int childClusterStartX(int cluster) {
        int areaX = cluster == 0 ? leftAreaX : rightAreaX;
        int areaW = cluster == 0 ? leftAreaW : rightAreaW;
        return areaX + (areaW - childClusterWidth(cluster)) / 2;
    }

    /** 子级页签的 X：先定位所在簇的起点，再按簇内序号累加「前面各书签宽度 + 3px 间隔」 */
    private int childTabX(int absIndex) {
        int cluster = childClusterOf(absIndex);
        int slot = childSlotOf(absIndex);
        int x = childClusterStartX(cluster);
        int visible = visibleChildCount();
        for (int i = 0; i < visible; i++) {
            int idx = childScrollOffset + i;
            if (childClusterOf(idx) != cluster) continue;
            if (childSlotOf(idx) < slot) {
                x += childTabWidth(idx) + CHILD_TAB_GAP;
            }
        }
        return x;
    }

    /** 这一排里最高的那个书签（决定该排基线 / 上探量 / 翻页按钮位置） */
    private int childMaxHeight() {
        int max = 0;
        int visible = visibleChildCount();
        for (int i = 0; i < visible; i++) {
            max = Math.max(max, childTabHeight(childScrollOffset + i));
        }
        return max;
    }

    /**
     * 子级页签的屏幕矩形（布局坐标，未缩放）—— 绘制、文字排版、悬停 / 点击命中
     * 全部只走这一个函数，几何永远只有一份，不会出现"画在这里、点在那里"。
     *
     * absIndex 是**绝对索引**，这里先减去 childScrollOffset 映射到这一页的 8 个可见位置：
     * 索引 8（第 9 个）在第 2 页（offset=8）落到 local=0，也就是左簇第 1 格，
     * 与索引 0 是同一块位置 —— 翻页复用位置，绝不往上叠第二排。
     * 一排内底边对齐（统一压进书体 CHILD_TAB_TUCK），按各自高度向上生长。
     */
    private TabRect childTabRect(int absIndex) {
        int h = childTabHeight(absIndex);
        return new TabRect(
                childTabX(absIndex),
                childTabsBottomY - h,
                childTabWidth(absIndex),
                h);
    }

    /** 这一排书签向上伸展的总高度（单排，不再累加第二排），用于自适应缩放 */
    private int maxChildTabExtent() {
        return childMaxHeight();
    }

    /** 子级页签越过 OUTER 上边距继续探出的量（自适应缩放时算进整幅界面，保证顶部不出屏） */
    private int childOverhangTop() {
        return Math.max(0, maxChildTabExtent() - CHILD_TAB_TUCK - BOOK_MARGIN_TOP + 2);
    }

    private int lineHeight() {
        return font == null ? 9 : font.lineHeight;
    }

    private int getClickedParentIndex(int mouseX, int mouseY) {
        for (int i = 0; i < visibleParentCount(); i++) {
            int idx = parentScrollOffset + i;
            int y = parentTabY(idx);
            if (mouseX >= parentListStartX && mouseX <= parentListStartX + PARENT_TAB_W
                    && mouseY >= y && mouseY <= y + PARENT_TAB_H) {
                return idx;
            }
        }
        return -1;
    }

    /**
     * 子级页签命中判定：直接用 childTabRect()，与绘制 / 文字排版是同一份几何。
     * 只遍历当前页可见的 8 个位置（绝对索引 childScrollOffset ~ +7），
     * 返回的也是**绝对索引**，点击后 rebuildPagesForCurrentChild() 直接按绝对索引取内容。
     */
    private int getClickedChildIndex(int mouseX, int mouseY) {
        int visible = visibleChildCount();
        for (int i = 0; i < visible; i++) {
            int idx = childScrollOffset + i;
            if (childTabRect(idx).contains(mouseX, mouseY)) {
                return idx;
            }
        }
        return -1;
    }

    /** 子级页签矩形：绘制、文字、命中判定共用的唯一几何载体 */
    private static final class TabRect {
        final int x, y, w, h;

        TabRect(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean contains(int px, int py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
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

        double layoutX = convertMouseX(mouseX);
        double layoutY = convertMouseY(mouseY);

        // 26 的 Matrix3x2fStack 只有 2D translate / scale（1.20.1 的 PoseStack 需要 z 与 1.0f 分量）
        if (scale != 1.0f) {
            pose.translate(width / 2.0f, height / 2.0f);
            pose.scale(scale, scale);
            pose.translate(-width / 2.0f, -height / 2.0f);
        }

        int hoveredParent = getClickedParentIndex((int) layoutX, (int) layoutY);
        int hoveredChild = getClickedChildIndex((int) layoutX, (int) layoutY);

        // 绘制顺序（自下而上）：
        //   1) 上方子级页签底板 —— 在书体之下，下端压进书体的部分被盖住；
        //   2) 书体；
        //   3) 左侧父级页签底板 + 高光 —— 在书体之上，完整形状可见；
        //   4) 页签文字（父级横排、子级竖排）；
        //   5) 左右页内容、翻页按钮、页码、标题。
        drawChildTabPlates(guiGraphics, hoveredChild);

        drawBook(guiGraphics);

        drawParentTabPlates(guiGraphics, hoveredParent);

        drawTabLabels(guiGraphics);

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

    /**
     * 书体（书皮 + 左右两页）来自 para_curse_note.png，按 PIXEL_SCALE 放大。
     * 贴图里的两个书签不再当装饰用：它们已经被真正的页签（drawChildTabPlates / drawParentTabPlates）
     * 取代，所以这里只画书体，避免页签与装饰书签重复出现。
     */
    private void drawBook(GuiGraphicsExtractor guiGraphics) {
        // 26 的 stretched blit：dest(w,h) 与 src(srcW,srcH) 分开给，等价于 1.20.1 的
        // blit(tex, x, y, w, h, u, v, uw, vh, texW, texH)
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, NOTE_TEXTURE,
                bookX, bookY,
                BOOK_TEX_X, BOOK_TEX_Y,
                BOOK_W, BOOK_H,
                BOOK_TEX_W, BOOK_TEX_H,
                TEX_SIZE, TEX_SIZE);
    }

    // ======================= 页签绘制（贴图书签底板 + 文字）=======================

    /** 选中 / 悬停时页签的明暗系数（与位置无关的选中标识） */
    private static float tabBrightness(boolean selected, boolean hovered) {
        if (selected) return TAB_BRIGHTNESS_SELECTED;
        return hovered ? TAB_BRIGHTNESS_HOVER : TAB_BRIGHTNESS_NORMAL;
    }

    /** 半透明白色高光强度：选中 > 悬停 > 无 */
    private static float tabSheen(boolean selected, boolean hovered) {
        if (selected) return TAB_SHEEN_SELECTED;
        return hovered ? TAB_SHEEN_HOVER : 0.0F;
    }

    /**
     * 子级页签底板（书体上方）：用上方竖书签，画在书体**之前**，
     * 于是下端压进书体的那 6px 被书体盖住（像夹在书页里的书签）。
     *
     * 只有一排 8 个位置（左簇 4 + 右簇 4）：当前页显示 childScrollOffset..+7，
     * 第 9 个及以后翻页复用这些位置，所以这里既不需要按排分层、也不需要逆序绘制。
     */
    private void drawChildTabPlates(GuiGraphicsExtractor guiGraphics, int hoveredChild) {
        int topColor = EPCANoteBookmarkColors.getTopColor();

        int visible = visibleChildCount();
        for (int i = 0; i < visible; i++) {
            int idx = childScrollOffset + i;
            boolean selected = selectedChildIndex == idx;
            boolean hovered = hoveredChild == idx;
            TabRect r = childTabRect(idx);
            drawBookmarkPlate(guiGraphics, r.x, r.y, r.w, r.h,
                    CHILD_TAB_TEX_X, CHILD_TAB_TEX_Y, CHILD_TAB_TEX_W, CHILD_TAB_TEX_H,
                    topColor, tabBrightness(selected, hovered));
            float sheen = tabSheen(selected, hovered);
            if (sheen > 0.0F) {
                drawBookmarkSheen(guiGraphics, r.x, r.y, r.w, r.h,
                        CHILD_TAB_TEX_X, CHILD_TAB_TEX_Y, CHILD_TAB_TEX_W, CHILD_TAB_TEX_H, sheen);
            }
        }
    }

    /**
     * 父级页签底板（左侧一列）：用左侧横书签，画在书体**之后**，
     * 所以整块书签形状完整可见（连同右端压在书皮上的一小段），不会被书体吃掉；
     * 选中 / 悬停的高光与底板同层，紧跟着底板画。
     */
    private void drawParentTabPlates(GuiGraphicsExtractor guiGraphics, int hoveredParent) {
        int leftColor = EPCANoteBookmarkColors.getLeftColor();

        // 左侧一列：父级页签（横书签底板，文字横排），同一列共用 PARENT_TAB_SCALE
        for (int i = 0; i < visibleParentCount(); i++) {
            int idx = parentScrollOffset + i;
            boolean selected = selectedParentIndex == idx;
            boolean hovered = hoveredParent == idx;
            int y = parentTabY(idx);
            drawBookmarkPlate(guiGraphics, parentListStartX, y, PARENT_TAB_W, PARENT_TAB_H,
                    PARENT_TAB_TEX_X, PARENT_TAB_TEX_Y, PARENT_TAB_TEX_W, PARENT_TAB_TEX_H,
                    leftColor, tabBrightness(selected, hovered));
            float sheen = tabSheen(selected, hovered);
            if (sheen > 0.0F) {
                drawBookmarkSheen(guiGraphics, parentListStartX, y, PARENT_TAB_W, PARENT_TAB_H,
                        PARENT_TAB_TEX_X, PARENT_TAB_TEX_Y, PARENT_TAB_TEX_W, PARENT_TAB_TEX_H, sheen);
            }
        }
    }

    /** 页签文字：内容全部来自 EPCANoteTabData（parent.name / child.name），这里只负责排版 */
    private void drawTabLabels(GuiGraphicsExtractor guiGraphics) {
        int parentColor = labelColorFor(EPCANoteBookmarkColors.getLeftColor());
        for (int i = 0; i < visibleParentCount(); i++) {
            int idx = parentScrollOffset + i;
            EPCANoteTabData.ParentTab tab = parentTabs.get(idx);
            // 父级书签整块都在书体之上，文字可以用满整条书签（左侧尖角用 inset 让开）
            drawHorizontalTabLabel(guiGraphics, translateName(tab.name),
                    parentListStartX, parentTabY(idx), PARENT_TAB_W, PARENT_TAB_H,
                    PARENT_TAB_TEXT_INSET_L, PARENT_TAB_TEXT_INSET_R, parentColor);
        }

        int childColor = labelColorFor(EPCANoteBookmarkColors.getTopColor());
        for (int i = 0; i < visibleChildCount(); i++) {
            int idx = childScrollOffset + i;
            EPCANoteTabData.ChildTab child = currentChildTabs.get(idx);
            TabRect r = childTabRect(idx);
            drawVerticalTabLabel(guiGraphics, translateName(child.name),
                    r.x, r.y, r.w, r.h, childColor);
        }
    }

    /**
     * 从 NOTE_TEXTURE 截取一块书签，用顶点色乘算着色（灰色书签 -> 配置颜色），
     * 再叠乘「全局亮度增益 BOOKMARK_BRIGHTNESS_GAIN」与「状态亮度系数」，逐通道 clamp 到 [0,1]。
     *
     * 26 用 blit 的顶点色 ARGB 取代 1.20.1 的 RenderSystem.setShaderColor：
     * 顶点色只作用于本次 blit 生成的 4 个顶点，天然局部生效，
     * 所以不需要（也没有可用的）setShaderColor(1,1,1,1) 复位，不会影响书体 / 文字 / 页面内容。
     */
    private void drawBookmarkPlate(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h,
                                   int u, int v, int texW, int texH,
                                   int rgb, float brightness) {
        float gain = BOOKMARK_BRIGHTNESS_GAIN * brightness;
        float r = clamp01(((rgb >> 16) & 0xFF) / 255.0F * gain);
        float g = clamp01(((rgb >> 8) & 0xFF) / 255.0F * gain);
        float b = clamp01((rgb & 0xFF) / 255.0F * gain);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, NOTE_TEXTURE,
                x, y, u, v, w, h, texW, texH, TEX_SIZE, TEX_SIZE,
                ARGB.colorFromFloat(1.0F, r, g, b));
    }

    /**
     * 半透明白色高光：同样按书签形状 blit 一次，所以不会出现矩形色块。
     * GUI_TEXTURED 管线本身是 BlendFunction.TRANSLUCENT，顶点色的 alpha 直接喂给混合，
     * 等价于 1.20.1 的 enableBlend + defaultBlendFunc + setShaderColor(1,1,1,alpha)。
     */
    private void drawBookmarkSheen(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h,
                                   int u, int v, int texW, int texH, float alpha) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, NOTE_TEXTURE,
                x, y, u, v, w, h, texW, texH, TEX_SIZE, TEX_SIZE,
                ARGB.colorFromFloat(clamp01(alpha), 1.0F, 1.0F, 1.0F));
    }

    private static float clamp01(float v) {
        return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
    }

    /**
     * 文字颜色：书签贴图本体是灰色 (134,135,134)，实际渲染颜色 ≈ 配置色 × 全局增益 × 0.53，
     * 据此估算底色亮度，浅底用深字、深底用浅字。只看配置色（不看选中明暗），
     * 保证选中 / 未选中切换时文字颜色稳定。
     */
    private static int labelColorFor(int baseRgb) {
        int r = (baseRgb >> 16) & 0xFF;
        int g = (baseRgb >> 8) & 0xFF;
        int b = baseRgb & 0xFF;
        int lum = (int) ((r * 299 + g * 587 + b * 114) / 1000 * 0.53F * BOOKMARK_BRIGHTNESS_GAIN);
        return lum >= 100 ? TAB_TEXT_ON_LIGHT : TAB_TEXT_ON_DARK;
    }

    /**
     * 父级页签：横排文字，居中在书签的**正文区**里（左右两端的尖角都让开，inset 由贴图实测得来）。
     *
     * 父级书签是 104x52，正文区 = 104 - 24 - 20 = 60px：
     *   * 一行放得下 6 个汉字（54px），"寄生体图鉴" 这种 5 字章节名（45px）居中后左右各留 7px，
     *     不会再被顶到右端斜边上（之前 inset 左 22 / 右 6，文字整体右偏 8px，看起来又挤又越界）；
     *   * 更长的名字用 font.split 按 60px 折行，行数不超过书签高度能放下的行数（52/9 = 5 行），
     *     并且逐行在正文区里居中，保证完整可读且绝不会画出书签。
     */
    private void drawHorizontalTabLabel(GuiGraphicsExtractor guiGraphics, String text,
                                        int x, int y, int w, int h,
                                        int insetL, int insetR, int color) {
        if (text == null || text.isEmpty()) return;
        int areaX = x + insetL;
        int areaW = Math.max(8, w - insetL - insetR);
        int lineH = font.lineHeight;
        int maxLines = Math.max(1, h / lineH);
        List<FormattedCharSequence> lines = font.split(FormattedText.of(text), areaW);
        if (lines.isEmpty()) return;
        int lineCount = Math.min(lines.size(), maxLines);
        int blockH = lineCount * lineH;
        int textY = y + (h - blockH) / 2;
        for (int i = 0; i < lineCount; i++) {
            FormattedCharSequence line = lines.get(i);
            int lineW = font.width(line);
            guiGraphics.text(font, line, areaX + (areaW - lineW) / 2, textY + i * lineH, color, true);
        }
    }

    /** 子级页签：竖排文字，每个字符单独一行并水平居中；名称超长时压缩行距，保证完整可见 */
    private void drawVerticalTabLabel(GuiGraphicsExtractor guiGraphics, String text,
                                      int x, int y, int w, int h, int color) {
        if (text == null || text.isEmpty()) return;
        int count = text.length();
        int scale = Math.max(1, h / CHILD_TAB_TEX_H);
        int notch = CHILD_TAB_NOTCH_TEX * scale;                 // 顶部 V 形缺口，文字要避开
        int areaTop = y + notch;
        int areaH = Math.max(1, h - CHILD_TAB_TUCK - notch);     // 缺口以下、书体以上
        int spacing = lineHeight();
        if (count * spacing > areaH) {
            spacing = Math.max(4, (areaH - 2) / count);
        }
        int blockH = count * spacing;
        int textY = areaTop + Math.max(0, (areaH - blockH) / 2);
        int centerX = x + w / 2;
        for (int i = 0; i < count; i++) {
            String ch = text.substring(i, i + 1);
            int chW = font.width(ch);
            guiGraphics.text(font, ch, centerX - chW / 2, textY + i * spacing, color, true);
        }
    }

    /**
     * 内容区严格限制在贴图里左右两页的纸面矩形内。
     *
     * 26 的 enableScissor 会自己把矩形按当前 pose 做 transformAxisAligned，
     * 所以这里必须传「布局坐标」（与 1.20.1 相反：1.20.1 的 enableScissor 不跟随 pose，
     * 需要手动换算成屏幕坐标；在 26 上再换算一次就会被缩放两次）。
     */
    private void enableAreaScissor(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h) {
        guiGraphics.enableScissor(x, y, x + w, y + h);
    }

    private void drawPages(GuiGraphicsExtractor guiGraphics) {
        if (pages.isEmpty()) return;
        PageContent page = pages.get(currentPage);
        if (!page.leftElements.isEmpty()) {
            enableAreaScissor(guiGraphics, leftAreaX, leftAreaY, leftAreaW, leftAreaH);
            renderElements(guiGraphics, page.leftElements, leftAreaX, leftAreaY, leftAreaW);
            guiGraphics.disableScissor();
        }
        if (!page.rightElements.isEmpty()) {
            enableAreaScissor(guiGraphics, rightAreaX, rightAreaY, rightAreaW, rightAreaH);
            renderElements(guiGraphics, page.rightElements, rightAreaX, rightAreaY, rightAreaW);
            guiGraphics.disableScissor();
        }
    }

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

        // 标题：画在书体下方，带阴影，保证任何背景下都能看清（1.20.1 版页脚没有横线）
        String drawnTitle = fit(font, titleText, BOOK_W - 40);
        int titleX = bookX + BOOK_W / 2 - font.width(drawnTitle) / 2;
        int titleY = bookY + BOOK_H + 6;
        guiGraphics.text(font, drawnTitle, titleX, titleY, TITLE_TEXT, true);

        // 页码：书体底部深棕色书脊带的正中央
        if (!pages.isEmpty()) {
            String pageStr = (currentPage + 1) + "/" + pages.size();
            guiGraphics.centeredText(font, pageStr, bookX + BOOK_W / 2,
                    bookY + BOOK_H - font.lineHeight - 4, TITLE_TEXT);
        }
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty()) return "";
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - 6)) + "…";
    }

    private void renderElements(GuiGraphicsExtractor guiGraphics, List<RenderElement> elements, int baseX, int baseY, int areaWidth) {
        int yOffset = 0;
        for (RenderElement e : elements) {
            if (e instanceof TextLine text) {

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

        @Override
        protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            // 禁用态（首页的左键 / 末页的右键）画成暗色，视觉上能直接看出「这一侧不能再翻」；
            // 悬停时略提亮，强调可点击。26 侧用 blit 的顶点色替代 RenderSystem.setShaderColor。
            boolean hovered = this.active
                    && mouseX >= getX() && mouseX < getX() + width
                    && mouseY >= getY() && mouseY < getY() + height;
            float tint = !this.active ? 0.35F : (hovered ? 1.15F : 1.0F);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), 0.0F, 0.0F,
                    width, height, texWidth, texHeight,
                    ARGB.colorFromFloat(1.0F, tint, tint, tint));
        }
    }
}
