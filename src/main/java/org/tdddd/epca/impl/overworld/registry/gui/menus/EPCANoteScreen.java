package org.tdddd.epca.impl.overworld.registry.gui.menus;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.c2s.NoteDetailUnlockRequestPacket;
import org.tdddd.epca.impl.network.packet.c2s.NoteSyncRequestPacket;
import org.tdddd.epca.impl.overworld.data.EPCANoteBookmarkColors;
import org.tdddd.epca.impl.overworld.data.EPCANoteClientUnlocks;
import org.tdddd.epca.impl.overworld.data.EPCANoteDetailCosts;
import org.tdddd.epca.impl.overworld.data.EPCANoteEntryStats;
import org.tdddd.epca.impl.overworld.data.EPCANoteLang;
import org.tdddd.epca.impl.overworld.data.EPCANoteSpecies;
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
    //      左侧横书签 (1,45,26,13)  -> 父级页签，固定 3x = 78x39，尖角朝左（与书体同一像素比）
    //      上方竖书签 (68,1,10,18)  -> 子级页签，固定 3x = 30x54，尺寸不随名称字数变化
    //  原 inner_frame.png 为 140x90，绘制到 420x270 也是 3x，这里沿用 3x。
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
    private static final ResourceLocation NOTE_TEXTURE =
            new ResourceLocation(epca.MODID, "textures/gui/epca_note/para_curse_note.png");
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
    // 父级页签（左侧一列）：贴图左侧横书签 (1,45,26,13)，3x 等比放大 -> 78x39。
    //   * 26:13 = 2:1 的宽高比保持不变（w = 26*3, h = 13*3），不做任何拉伸；
    //     3x 与书体 / 内容框同一个像素比例，书签不再比书本"粗"一档；
    //   * 高 39px 可放 4 行 9px 行高的横排文字，3~4 个汉字一行仍然宽松；
    //   * 尖角在左（指向远离书体的一侧），右端压在书皮上：画在书体**之后**，
    //     所以整块书签完整可见。
    //   * 关键：整列采用「右端锚定」，左端位置由宽度反推 ——
    //     右端（书签与书皮的连接处）永远 = bookX + PARENT_TAB_RIGHT_END_X
    //     = bookX + TUCK(6) + SHIFT_RIGHT(18) = bookX + 24，
    //     所以缩放从 4x 改成 3x 时连接点 X 完全不动，只是左端向右收 26px。
    //     而左页内容框从 bookX + 42 才开始，连接处到正文区仍剩 18px，绝不会压住书页正文。
    //   * 同一列所有父级页签共用 PARENT_TAB_SCALE 这一个缩放。
    //
    // 子级页签（书体上方）：贴图上方竖书签 (68,1,10,18)，**固定** 3x -> 30x54。
    //   * w:h = 10:18 保持不变，一律等比放大 3x：**所有子书签尺寸完全相同**，不再按名称字数放大；
    //   * 尺寸固定后，长名称改为在书签**内部**自适应（见 drawVerticalTabLabel）：
    //     先压行距、再整段等比缩小，保证完整显示、不截断也不溢出；
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
    private static final int PARENT_TAB_SCALE = 3;                                   // 3x 等比放大（与书体一致）
    private static final int PARENT_TAB_W = PARENT_TAB_TEX_W * PARENT_TAB_SCALE;     // 78
    private static final int PARENT_TAB_H = PARENT_TAB_TEX_H * PARENT_TAB_SCALE;     // 39
    private static final int PARENT_TAB_GAP = 4;                                     // 同一列页签间距
    private static final int PARENT_TAB_TUCK = 6;                                    // 右端压进书皮的像素
    private static final int PARENT_TAB_SHIFT_RIGHT = 18;                            // 整列整体右移的像素
    /** 父级书签右端（= 与书皮的连接处）相对 bookX 的偏移，与缩放无关，永远是 24 */
    private static final int PARENT_TAB_RIGHT_END_X =
            PARENT_TAB_TUCK + PARENT_TAB_SHIFT_RIGHT;                                // 24
    /** 父级书签右端到左页内容框左边界的净间距（书签在书体之上，必须保持为正） */
    private static final int PARENT_TAB_RIGHT_END_TO_PAGE =
            PAGE_OFF_X - PARENT_TAB_RIGHT_END_X;                                     // 42 - 24 = 18
    // 父级书签是「左右两端都收尖」的形状（贴图 26x13 逐像素扫描：左尖 6px、右尖 5px），
    // 文字必须让开两端的尖角，否则汉字会被顶到两端的斜边上，看起来又挤又越界。
    // 3x 放大后左让 18、右让 15，正文区 45px 居中落在书签实体部分（18..63）上，
    // 一行放得下 5 个汉字（45px）；更长的名字由 font.split 折行，绝不画到书签外面。
    private static final int PARENT_TAB_TEXT_INSET_L = 18;                           // 让开左侧尖角 (6*3)
    private static final int PARENT_TAB_TEXT_INSET_R = 15;                           // 让开右侧尖角 (5*3)
    // 正文区宽度 = 78 - 18 - 15 = 45px（5 个汉字 45px 一行放得下；6 个汉字 54px 自动折成两行）

    private static final int CHILD_TAB_TEX_X = 68;
    private static final int CHILD_TAB_TEX_Y = 1;
    private static final int CHILD_TAB_TEX_W = 10;
    private static final int CHILD_TAB_TEX_H = 18;
    /** 子书签唯一的缩放倍数：所有子书签固定 3x -> 30x54，尺寸与名称字数无关 */
    private static final int CHILD_TAB_SCALE = 3;
    private static final int CHILD_TAB_W = CHILD_TAB_TEX_W * CHILD_TAB_SCALE;        // 30
    private static final int CHILD_TAB_H = CHILD_TAB_TEX_H * CHILD_TAB_SCALE;        // 54
    private static final int CHILD_TAB_PER_CLUSTER = 4;                              // 每簇最多 4 个
    private static final int CHILD_TAB_CLUSTERS = 2;                                 // 左页上方 / 右页上方
    private static final int CHILD_TAB_PER_ROW = CHILD_TAB_PER_CLUSTER * CHILD_TAB_CLUSTERS; // 一排 8 个
    /** 一页就是这一排的 8 个位置：第 9 个及以后翻页显示在同样的 1~8 位置上（不再叠第二排） */
    private static final int CHILD_TAB_PAGE_SIZE = CHILD_TAB_PER_ROW;                  // 8
    private static final int CHILD_TAB_GAP = 3;                                      // 同一簇内水平间隔 3px
    private static final int CHILD_TAB_TUCK = 6;                                     // 下端压进书体的像素
    private static final int CHILD_PAGE_BTN_SIZE = 18;                               // 与 page_btn_left/right.png 贴图同尺寸（18x18，1:1）
    private static final int CHILD_PAGE_BTN_GAP = 5;                                 // 翻页按钮与最近一簇书签的水平间隔
    private static final int CHILD_TAB_NOTCH_TEX = 3;                                // 顶部 V 形缺口深度（贴图像素）
    /** 竖排文字行距压缩下限：9px 行高最多压到 6px，再压字形就会互相啃 */
    private static final int CHILD_TAB_TEXT_MIN_SPACING = 6;
    /** 行距压到下限仍放不下时，整段竖排文字的等比缩小下限（保证还看得清） */
    private static final float CHILD_TAB_TEXT_MIN_SCALE = 0.6F;

    // GUI 整体尺寸（书体在 OUTER 内居中）
    private static final int BOOK_MARGIN_X = 60;
    private static final int BOOK_MARGIN_TOP = 58;                                    // 容纳固定 54px 高的子级页签（尺寸不再随字数变化）
    private static final int BOOK_MARGIN_BOTTOM = 36;
    private static final int OUTER_W = BOOK_W + BOOK_MARGIN_X * 2;                    // 579
    private static final int OUTER_H = BOOK_MARGIN_TOP + BOOK_H + BOOK_MARGIN_BOTTOM; // 415

    private static final int BUTTON_W = 24;
    private static final int BUTTON_H = 24;
    private static final int PAGE_BTN_SIZE = 18;

    // 父级页签从书体左侧探出的量（自适应缩放时算进整幅界面，见 getScaleFactor）
    // 3x 后整列从 bookX-54 开始，比左侧留白 BOOK_MARGIN_X(=60) 还窄 6px，所以探出量为 0：
    // 书签完全落在 OUTER 之内，小窗口缩小时不会被裁切。
    private static final int PARENT_OVERHANG_LEFT =
            Math.max(0, PARENT_TAB_W - PARENT_TAB_RIGHT_END_X - BOOK_MARGIN_X);       // 78-24-60 -> 0

    private static final ResourceLocation BTN_UP = new ResourceLocation(epca.MODID, "textures/gui/epca_note/button_up.png");
    private static final ResourceLocation BTN_DOWN = new ResourceLocation(epca.MODID, "textures/gui/epca_note/button_down.png");
    private static final ResourceLocation PAGE_BTN_LEFT = new ResourceLocation(epca.MODID, "textures/gui/epca_note/page_btn_left.png");
    private static final ResourceLocation PAGE_BTN_RIGHT = new ResourceLocation(epca.MODID, "textures/gui/epca_note/page_btn_right.png");

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
     * 书签整体亮度增益：只作用于书签贴图的 shader 颜色乘数（书体 / 文字 / 页面内容都不受影响）。
     *
     * 叠加公式：最终 shader 颜色 = clamp01(配置色 × 本增益 × 状态亮度(1.00 / 1.12 / 1.34))，
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

    /** 打开界面时只向服务端拉一次解锁集合（窗口 resize 会重新 init，不重复发） */
    private boolean syncRequested;

    /**
     * 悬停 tooltip 的暂存：页内内容的绘制都在缩放后的 pose 里，
     * 直接在那儿画 tooltip 会被一起缩小。所以绘制时只记下“要画什么”，
     * 等 {@link #render} 里 pose 弹回之后再按原始屏幕坐标画。
     */
    private ItemStack pendingItemTooltip;
    private List<Component> pendingTextTooltip;

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

        MinecraftForge.EVENT_BUS.register(eventListener);
        refreshTabData();
    }

    @Override
    protected void init() {
        super.init();

        // 书签颜色配置：每次打开界面检查一次文件修改时间（改完配置重新打开笔记即可生效）
        EPCANoteBookmarkColors.loadIfChanged();

        // 打开笔记先向服务端要一份解锁集合（登录时已发过一次，这里保证“打开这一刻”是最新的）
        requestUnlockSync();

        recomputeLayout();

        rebuildUIComponents();

        updateCurrentChildTabs();
        rebuildPagesForCurrentChild();
    }

    /** 向服务端请求同步本玩家的笔记解锁集合；同一个界面实例只发一次 */
    private void requestUnlockSync() {
        if (syncRequested) {
            return;
        }
        syncRequested = true;
        try {
            ModNetwork.sendToServer(new NoteSyncRequestPacket());
        } catch (Throwable ignored) {
            // 单机 / 连接异常时保持“未解锁”的保守状态即可
        }
    }

    /**
     * 服务端同步到了新的解锁集合（由 {@code EPCANoteClientUnlocks} 在主线程回调）。
     *
     * <p>只重排当前子书签的页，并把页码夹回合法范围：不改变选中的父/子书签，
     * 也不去动书签颜色、缩放这些跟解锁无关的状态。</p>
     */
    public void refreshUnlockState() {
        if (selectedChildIndex < 0 || selectedChildIndex >= currentChildTabs.size()) {
            return;
        }
        int page = currentPage;
        rebuildPagesForCurrentChild();
        if (pages.isEmpty()) {
            currentPage = 0;
        } else {
            currentPage = Math.max(0, Math.min(page, pages.size() - 1));
        }
        updatePageButtons();
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

        // 父级页签：右端锚定 —— 右端（与书皮的连接处）固定压在 bookX + PARENT_TAB_RIGHT_END_X(=24)，
        // 左端由宽度反推，所以缩放从 4x 变 3x 后连接点 X 不变；
        // 整列仍按书体高度垂直居中（列中心 = bookY + BOOK_H/2，与缩放无关），连接点 Y 也不变
        parentListStartX = bookX + PARENT_TAB_RIGHT_END_X - PARENT_TAB_W;
        parentListStartY = bookY + (BOOK_H - parentColumnHeight()) / 2;

        // 子级页签：只有一排，底边压进书体顶部 CHILD_TAB_TUCK 像素，高度恒为 CHILD_TAB_H(=54)；
        // 固定高度后上探量恒为 0（见 childOverhangTop），自适应缩放只由书体自身尺寸决定
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
     * 子级页签的整页翻页按钮。
     *
     * 显示条件：当前父级下的子页签总数 > 8（一排只放得下 8 个，多出来的只能翻页看），
     * 一排就放得下（≤8 个）时按钮隐藏，不占视觉空间。
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

    /** 翻页按钮的 Y：与这一排书签垂直居中对齐（排顶边 + (排高 - 按钮高) / 2）；排高固定为 54 */
    private int childPageButtonY() {
        int rowH = childMaxHeight();
        if (rowH <= 0) rowH = CHILD_TAB_H;
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

    /**
     * 为当前选中的子书签重建页列表。
     *
     * 两条路径：
     *   1) 这个 child_tab 命中一个种阶（EPCANoteSpecies）时，页由数据自动生成：
     *      先是「快捷跳转页」（该种阶下所有可列出的寄生体，一行一个，蓝色下划线可点击），
     *      然后每个寄生体各占一对页（左页黑色粗体名字，右页留空并封页）；
     *   2) 否则保持原有行为：读 child.content，按 $[page]$ / 自动折行分页。
     */
    private void rebuildPagesForCurrentChild() {
        pages.clear();
        currentPage = 0;
        if (selectedChildIndex < 0 || selectedChildIndex >= currentChildTabs.size()) {
            pages.add(PageContent.empty());
            updatePageButtons();
            return;
        }
        EPCANoteTabData.ChildTab child = currentChildTabs.get(selectedChildIndex);
        // 种阶目录是查表 + 读资源算出来的，任何异常都不该把笔记界面带崩：直接退回纯文本 content
        EPCANoteSpecies.Tier tier = null;
        try {
            tier = EPCANoteSpecies.tierForChildTab(child.id);
        } catch (Throwable ignored) {
            tier = null;
        }
        if (tier != null && !tier.entries.isEmpty()) {
            pages = buildTierPages(tier);
        } else {
            String translated = translateContent(child.content);
            if (translated != null && !translated.isEmpty()) {
                pages = buildPages(translated, font);
            }
        }
        if (pages.isEmpty()) {
            pages.add(PageContent.empty());
        }
        updatePageButtons();
    }

    // ======================= 种阶页（快捷跳转页 + 每个寄生体整对页）=======================

    /**
     * 生成一个种阶的全部页。
     *
     * 布局规则（对应需求 B / C）：
     *   * 快捷跳转页：把该种阶下所有可列出的寄生体名字一行一个排下来，每行蓝色下划线、可点击；
     *     一「对页」= 左页 + 右页，装不下就再开一对页 —— 也就是占用页数天然是 2×N 页（N 对）；
     *   * 每个寄生体：保底一对页（左页写名字，右页留空），内容超长时同样按 2×N 成对扩展；
     *   * 两者都是独立段落：段落结束在中途时，剩余的那一页被打上「封页」标记
     *     （rightSealed），后续段落不会再往这一页里塞内容，所以右页只会属于同一个段落或留空。
     *
     * 跳转目标只能在所有段落都排完之后才知道，所以先排正文段落、记下每个寄生体的起始页，
     * 再回填到快捷跳转页的条目里（element.targetPage）。
     */
    private List<PageContent> buildTierPages(EPCANoteSpecies.Tier tier) {
        List<PageContent> out = new ArrayList<>();

        // 1) 快捷跳转页：先建条目元素（目标页稍后回填），计入页数后正文段落自然排在它后面
        List<RenderElement> jumpElements = new ArrayList<>(tier.entries.size());
        List<JumpEntryElement> jumps = new ArrayList<>(tier.entries.size());
        for (int i = 0; i < tier.entries.size(); i++) {
            EPCANoteSpecies.Entry entry = tier.entries.get(i);
            JumpEntryElement element = new JumpEntryElement(i, entry.displayName(), leftAreaW,
                    isEntryUnlocked(entry));
            jumps.add(element);
            jumpElements.add(element);
        }
        appendSection(out, jumpElements);

        // 2) 每个寄生体一段：左页黑色粗体名字（+ 可选正文），右页留空并封页
        List<Integer> entryStartPages = new ArrayList<>(tier.entries.size());
        for (EPCANoteSpecies.Entry entry : tier.entries) {
            entryStartPages.add(appendSection(out, buildEntryElements(entry)));
        }

        // 3) 回填跳转目标：点击名字 -> 该寄生体正文的起始页（页码与书签选中态在跳转时一起刷新）
        //    未解锁的条目**不回填**，于是 hitJump 自动跳过它 —— 未解锁不可点击。
        for (JumpEntryElement jump : jumps) {
            if (!jump.unlocked) {
                continue;
            }
            if (jump.entryIndex >= 0 && jump.entryIndex < entryStartPages.size()) {
                jump.targetPage = entryStartPages.get(jump.entryIndex);
            }
        }
        return out;
    }

    /**
     * 一个寄生体的正文元素，共四种状态（由“条目是否已发现”×“详细数据是否已解锁”决定）：
     *
     * <ol>
     *   <li><b>条目未解锁</b>：名字、正文、（本应出现的）解锁行全部画成 §k§0 黑色乱码，
     *       没有任何可点击元素 —— 需求一“未解锁时名称与正文显示为黑色乱码”；</li>
     *   <li><b>已发现 / 详情未解锁</b>：名字黑色粗体，下面一行可点击的
     *       「消耗 N 物品 解锁」（蓝色下划线 + tooltip），点击只发 C2S 请求包；</li>
     *   <li><b>已发现 / 详情已解锁</b>：名字下面换成「详细数据：」整块（生命 / 护甲 / 攻击 /
     *       伤害适应性 / 变种 / 掉落物图标）；</li>
     *   <li>第 4 种（条目未解锁但详情已解锁）在服务端不可达（解锁详情要求先发现），
     *       这里仍按“条目未解锁”渲染，保证任何异常数据都不会泄露名字。</li>
     * </ol>
     */
    private List<RenderElement> buildEntryElements(EPCANoteSpecies.Entry entry) {
        List<RenderElement> elements = new ArrayList<>();
        boolean unlocked = isEntryUnlocked(entry);
        boolean detailUnlocked = unlocked && isDetailUnlocked(entry);

        String name = entry.displayName();
        if (name == null || name.isEmpty()) {
            name = entry.path();
        }
        // 黑色粗体：样式直接写在 Component 上，和 §0 这种手写颜色码无关，永远生效
        Component title = unlocked
                ? Component.literal(name).withStyle(ChatFormatting.BOLD, ChatFormatting.BLACK)
                : obfuscated(name);
        for (FormattedCharSequence line : font.split(title, leftAreaW)) {
            elements.add(new TextLine(line, PAGE_TEXT));
        }

        String bodyKey = entry.bodyKey();
        String body = EPCANoteLang.find(bodyKey);
        if (body == null) {
            body = EPCANoteLang.mainLangOrNull(bodyKey);
        }
        if (body != null && !body.isEmpty()) {
            if (unlocked) {
                elements.addAll(parseElements(body, leftAreaW, font));
            } else {
                elements.addAll(obfuscatedLines(body));
            }
        }

        if (!unlocked) {
            // 未解锁：连“要消耗什么”都不透露，直接一行同样长度的乱码
            String hint = t("epca.note.detail.locked_hint", "详细数据未解锁");
            for (FormattedCharSequence line : font.split(obfuscated(hint), leftAreaW)) {
                elements.add(new TextLine(line, PAGE_TEXT));
            }
        } else if (!detailUnlocked) {
            elements.add(new CostLineElement(entry.entityId, EPCANoteDetailCosts.costsFor(entry.entityId)));
        } else {
            elements.addAll(buildDetailElements(entry));
        }
        return elements;
    }

    /**
     * 「详细数据」整块，字段顺序严格固定（需求三）：
     * 基础生命值 / 基础护甲值 / 基础攻击伤害 / 伤害适应性（仅“有”）/ 变种（仅“有”）/ 掉落物。
     */
    private List<RenderElement> buildDetailElements(EPCANoteSpecies.Entry entry) {
        List<RenderElement> elements = new ArrayList<>();
        EPCANoteEntryStats.Detail detail = EPCANoteEntryStats.detailFor(entry.entityId);

        addWrappedText(elements, t("epca.note.detail.header", "详细数据："), PAGE_TEXT);
        addWrappedText(elements, t("epca.note.detail.health", "基础生命值：%s", detail.health), PAGE_TEXT);
        addWrappedText(elements, t("epca.note.detail.armor", "基础护甲值：%s", detail.armor), PAGE_TEXT);
        addWrappedText(elements, t("epca.note.detail.attack", "基础攻击伤害：%s", detail.attack), PAGE_TEXT);
        if (detail.hasAdaptation) {
            addWrappedText(elements, t("epca.note.detail.adaptation", "伤害适应性：有"), PAGE_TEXT);
        }
        if (!detail.variants.isEmpty()) {
            addWrappedText(elements,
                    t("epca.note.detail.variants", "变种：%s", String.join("/", detail.variants)), PAGE_TEXT);
        }
        elements.add(new LootRowElement(detail.drops));
        return elements;
    }

    /** 一段文本按页宽折行后加入元素列表（保留 § 颜色码样式） */
    private void addWrappedText(List<RenderElement> out, String text, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        for (FormattedCharSequence line : font.split(legacyStyledText(text), leftAreaW)) {
            out.add(new TextLine(line, color));
        }
    }

    /** 黑色乱码组件：§k + §0（对应原版 Obfuscated + BLACK） */
    private static Component obfuscated(String text) {
        String shown = text == null || text.isEmpty() ? "????" : text;
        return Component.literal(shown).withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.BLACK);
    }

    /**
     * 一整段正文都打成黑色乱码（未解锁时用）。
     * 先把 § 颜色码、图片标记、分页标记清掉，只保留“有多少字”，
     * 这样乱码的长度/行数与原正文一致，不会让人从排版猜出内容。
     */
    private List<RenderElement> obfuscatedLines(String raw) {
        List<RenderElement> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        String plain = raw
                .replaceAll("\u00A7.", "")
                .replaceAll("\\$\\{img:[^}]*\\}\\$", "")
                .replaceAll("\\$\\[page\\]\\$", "");
        for (String line : plain.split("\n", -1)) {
            if (line.isEmpty()) {
                out.add(new TextLine(FormattedCharSequence.EMPTY, PAGE_TEXT));
                continue;
            }
            for (FormattedCharSequence seq : font.split(obfuscated(line), leftAreaW)) {
                out.add(new TextLine(seq, PAGE_TEXT));
            }
        }
        return out;
    }

    /** 条目是否已被玩家发现（客户端缓存，数据只来自服务端同步） */
    private boolean isEntryUnlocked(EPCANoteSpecies.Entry entry) {
        try {
            return EPCANoteClientUnlocks.isUnlocked(entry.entityId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 该条目的详细数据是否已解锁 */
    private boolean isDetailUnlocked(EPCANoteSpecies.Entry entry) {
        try {
            return EPCANoteClientUnlocks.isDetailUnlocked(entry.entityId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * 笔记文案：先查拆分语言表（assets/epca/lang/para_curse_note/），再回退主语言文件，
     * 最后用调用方给的默认文案；带参数的用 {@link String#format} 填。
     */
    private static String t(String key, String fallback, Object... args) {
        String pattern = EPCANoteLang.resolveKey(key, fallback);
        if (args == null || args.length == 0) {
            return pattern == null ? "" : pattern;
        }
        try {
            return String.format(pattern, args);
        } catch (Throwable ignored) {
            return pattern == null ? "" : pattern;
        }
    }

    /**
     * 把一个段落（一串元素）追加成「整对页」。
     *
     * 每对页先填左页、再填右页；都填满或段落结束时收尾。段落最后一对页如果右页没用满，
     * 就标记 rightSealed = true（右页强制留空，不允许后续段落占用）。
     * 注意：调用方每次都是新段落，所以不会往已有页里续写；rightSealed 是这条约定的显式表达，
     * 也保护将来若改成「续写」时不会把别人的右页吃掉。
     *
     * @return 该段落第一页的页码（= 加入前的页数）
     */
    private int appendSection(List<PageContent> out, List<RenderElement> elements) {
        int startPage = out.size();
        if (elements.isEmpty()) {
            out.add(PageContent.empty());
            return startPage;
        }
        int index = 0;
        while (index < elements.size()) {
            // ---- 左页 ----
            List<RenderElement> left = new ArrayList<>();
            int used = 0;
            while (index < elements.size()) {
                RenderElement element = elements.get(index);
                if (element instanceof PageBreakElement) {
                    index++;
                    break;
                }
                int height = element.getHeight(font);
                if (!left.isEmpty() && used + height > leftAreaH) {
                    break;
                }
                layoutAt(element, used);
                left.add(element);
                used += height;
                index++;
            }

            // ---- 右页 ----
            List<RenderElement> right = new ArrayList<>();
            used = 0;
            while (index < elements.size()) {
                RenderElement element = elements.get(index);
                if (element instanceof PageBreakElement) {
                    index++;
                    break;
                }
                int height = element.getHeight(font);
                if (!right.isEmpty() && used + height > rightAreaH) {
                    break;
                }
                layoutAt(element, used);
                right.add(element);
                used += height;
                index++;
            }

            // 段落在本对页结束 -> 右页封页（右页只能属于这个段落，或者留空）
            boolean sealed = index >= elements.size();
            out.add(new PageContent(left, right, sealed));
        }
        return startPage;
    }

    /**
     * 记录元素在所在页里的纵向偏移（绘制、点击命中都直接用这个值）。
     * 只有可点击的元素（快捷跳转条目、解锁详情那一行）需要它，其它元素保持默认 -1。
     */
    private void layoutAt(RenderElement element, int offsetY) {
        if (element instanceof JumpEntryElement jump) {
            jump.layoutY = offsetY;
        } else if (element instanceof CostLineElement cost) {
            cost.layoutY = offsetY;
        }
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

    /** 纯文本 content 的分页：解析元素后按「整对页」排（与种阶页共用 appendSection） */
    private List<PageContent> buildPages(String rawContent, Font font) {
        List<PageContent> out = new ArrayList<>();
        appendSection(out, parseElements(rawContent, leftAreaW, font));
        return out;
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

    /**
     * 文本 -> 行元素。
     *
     * 这里额外支持原版 § 颜色码（{@code §0}~{@code §f}、{@code §l}、{@code §n}、{@code §r} 等），
     * 因为笔记正文（例如 {@code epca.note.entry.curbug.body}）历来就是这么写的；
     * 以前直接把带 § 的字符串丢给 {@code FormattedText.of} 会原样显示成「§0」这种乱码。
     * 没有 § 的文本行为与以前完全一致（一行拆一行，超宽由 font.split 折行）。
     */
    private void addTextElements(String text, int maxWidth, Font font, List<RenderElement> out) {
        if (text == null || text.isEmpty()) return;
        for (String line : text.split("\n", -1)) {
            if (line.isEmpty()) {
                // 保留空行，避免正文段落挤在一起（$[page]$ 附近经常有这种空行）
                out.add(new TextLine(FormattedCharSequence.EMPTY, PAGE_TEXT));
                continue;
            }
            List<FormattedCharSequence> wrapped = font.split(legacyStyledText(line), maxWidth);
            for (FormattedCharSequence seq : wrapped) {
                out.add(new TextLine(seq, PAGE_TEXT));
            }
        }
    }

    /**
     * 把带 § 颜色码的字符串解析成带 Style 的 Component。
     * 交由 Font.split 时样式会被保留（StringSplitter 走 FormattedText#visit），
     * 于是正文里的 {@code §l§0名字§r} 真的会变成粗体黑色名字。
     */
    private static Component legacyStyledText(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        if (raw.indexOf('\u00A7') < 0) {
            return Component.literal(raw);
        }
        MutableComponent root = Component.empty();
        StringBuilder buffer = new StringBuilder();
        Style style = Style.EMPTY;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\u00A7' && i + 1 < raw.length()) {
                ChatFormatting format = ChatFormatting.getByCode(raw.charAt(i + 1));
                if (format != null) {
                    if (buffer.length() > 0) {
                        root.append(Component.literal(buffer.toString()).withStyle(style));
                        buffer.setLength(0);
                    }
                    style = format == ChatFormatting.RESET ? Style.EMPTY : style.applyFormat(format);
                    i++;
                    continue;
                }
            }
            buffer.append(c);
        }
        if (buffer.length() > 0) {
            root.append(Component.literal(buffer.toString()).withStyle(style));
        }
        return root;
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
     * 子级页签的尺寸：**固定 30x54**（贴图 10x18 的 3x）。
     *
     * <p>过去这里按竖排字数把整块书签等比放大到 4x~8x（childTabScale / childTabWidth /
     * childTabHeight 三个按索引取值的函数），结果是「名字越长书签越宽越高」，同一排书签
     * 高矮不齐、宽窄不一。现在尺寸恒定：宽窄整齐，长名称改由
     * {@link #drawVerticalTabLabel} 在书签内部压行距 / 等比缩小来消化。</p>
     */
    private int childTabWidth() {
        return CHILD_TAB_W;
    }

    private int childTabHeight() {
        return CHILD_TAB_H;
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

    /** 某簇的整簇宽度（含簇内 CHILD_TAB_GAP=3px 的水平间隔）；尺寸固定后 = 个数 * 30 + 间隔 */
    private int childClusterWidth(int cluster) {
        int count = childClusterCount(cluster);
        if (count == 0) return 0;
        return count * CHILD_TAB_W + (count - 1) * CHILD_TAB_GAP;
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

    /** 子级页签的 X：簇起点 + 簇内序号 *（固定宽 30 + 3px 间隔），尺寸固定后可以直接算 */
    private int childTabX(int absIndex) {
        int cluster = childClusterOf(absIndex);
        int slot = childSlotOf(absIndex);
        return childClusterStartX(cluster) + slot * (CHILD_TAB_W + CHILD_TAB_GAP);
    }

    /**
     * 这一排书签向上伸展的高度（决定该排基线 / 上探量 / 翻页按钮位置）。
     * 尺寸固定后恒为 CHILD_TAB_H(=54)，不再随字数变化。
     */
    private int childMaxHeight() {
        return visibleChildCount() == 0 ? 0 : CHILD_TAB_H;
    }

    /**
     * 子级页签的屏幕矩形（布局坐标，未缩放）—— 绘制、文字排版、悬停 / 点击命中
     * 全部只走这一个函数，几何永远只有一份，不会出现"画在这里、点在那里"。
     *
     * absIndex 是**绝对索引**，这里先减去 childScrollOffset 映射到这一页的 8 个可见位置：
     * 索引 8（第 9 个）在第 2 页（offset=8）落到 local=0，也就是左簇第 1 格，
     * 与索引 0 是同一块位置 —— 翻页复用位置，绝不往上叠第二排。
     * 一排内底边对齐（统一压进书体 CHILD_TAB_TUCK），所有书签同高，上沿天然齐平。
     */
    private TabRect childTabRect(int absIndex) {
        int w = childTabWidth();
        int h = childTabHeight();
        return new TabRect(
                childTabX(absIndex),
                childTabsBottomY - h,
                w,
                h);
    }

    /** 这一排书签向上伸展的总高度：固定 54（单排，不再累加第二排），用于自适应缩放 */
    private int maxChildTabExtent() {
        return CHILD_TAB_H;
    }

    /**
     * 子级页签越过 OUTER 上边距继续探出的量（自适应缩放时算进整幅界面，保证顶部不出屏）。
     * 固定尺寸下 54 - 6 - 58 + 2 &lt; 0，恒为 0：书签整块落在 OUTER 之内，不会被屏幕裁掉。
     */
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

    // ======================= 快捷跳转页的点击命中 =======================

    /**
     * 当前这一对页里，鼠标命中了哪个快捷跳转条目？
     *
     * 命中矩形完全复用「排版时算好的 layoutY + font.width(seq)」，与绘制用的是同一份几何：
     *   x 从页内容区左边界开始（正文都是左对齐画的），宽 = 该条目的实际文字宽度，
     *   高 = 字体行高。所以点文字本体才有反应，不会点到空白也算。
     *
     * @return 目标页码；没命中返回 -1
     */
    private int getClickedJumpTarget(int mouseX, int mouseY) {
        if (pages.isEmpty() || currentPage < 0 || currentPage >= pages.size()) {
            return -1;
        }
        PageContent page = pages.get(currentPage);
        int target = hitJump(page.leftElements, leftAreaX, leftAreaY, mouseX, mouseY);
        if (target != -1) {
            return target;
        }
        return hitJump(page.rightElements, rightAreaX, rightAreaY, mouseX, mouseY);
    }

    private int hitJump(List<RenderElement> elements, int areaX, int areaY, int mouseX, int mouseY) {
        if (elements.isEmpty()) {
            return -1;
        }
        int lineH = lineHeight();
        for (RenderElement element : elements) {
            if (!(element instanceof JumpEntryElement jump) || jump.targetPage < 0) {
                continue;
            }
            int top = areaY + jump.layoutY;
            if (mouseX >= areaX && mouseX < areaX + jump.width
                    && mouseY >= top && mouseY < top + lineH) {
                return jump.targetPage;
            }
        }
        return -1;
    }

    // ======================= 「消耗 N 物品 解锁」的点击 / 悬停命中 =======================

    /**
     * 当前这一对页里，鼠标点中了哪一条「消耗 … 解锁」？
     *
     * <p>命中矩形与绘制共用同一份几何：x 从页内容区左边界开始、宽 = {@code font.width}、
     * 高 = 字体行高，纵向偏移用排版时写入的 {@link CostLineElement#layoutY}。
     * 点到文字本体才算，点空白不算。</p>
     *
     * @return 目标实体 id；没命中返回 {@code null}
     */
    private ResourceLocation getClickedDetailCost(int mouseX, int mouseY) {
        if (pages.isEmpty() || currentPage < 0 || currentPage >= pages.size()) {
            return null;
        }
        PageContent page = pages.get(currentPage);
        ResourceLocation id = hitCost(page.leftElements, leftAreaX, leftAreaY, mouseX, mouseY);
        if (id != null) {
            return id;
        }
        return hitCost(page.rightElements, rightAreaX, rightAreaY, mouseX, mouseY);
    }

    private ResourceLocation hitCost(List<RenderElement> elements, int areaX, int areaY,
                                     int mouseX, int mouseY) {
        if (elements.isEmpty()) {
            return null;
        }
        int lineH = lineHeight();
        for (RenderElement element : elements) {
            if (!(element instanceof CostLineElement cost)) {
                continue;
            }
            int top = areaY + cost.layoutY;
            if (mouseX >= areaX && mouseX < areaX + cost.width
                    && mouseY >= top && mouseY < top + lineH) {
                return cost.entityId;
            }
        }
        return null;
    }

    /** 跳到某一页（快捷跳转 / 翻页按钮共用）：页码、翻页按钮状态、书签选中态一起同步 */
    private void gotoPage(int page) {
        if (page < 0 || page >= pages.size()) {
            return;
        }
        currentPage = page;
        updatePageButtons();
        // 书签选中态：种阶书签本来就停在当前子页签上，这里再对齐一次，
        // 保证「跳转后页码 / 书签选中态」都指向同一个种阶（子书签页码也保持在包含它的那一屏）
        if (selectedChildIndex >= 0 && selectedChildIndex < currentChildTabs.size()) {
            int pageStart = (childScrollOffset / CHILD_TAB_PAGE_SIZE) * CHILD_TAB_PAGE_SIZE;
            if (selectedChildIndex < pageStart
                    || selectedChildIndex >= pageStart + CHILD_TAB_PAGE_SIZE) {
                childScrollOffset = (selectedChildIndex / CHILD_TAB_PAGE_SIZE) * CHILD_TAB_PAGE_SIZE;
                updateChildButtons();
            }
        }
    }

    /**
     * 书签名解析：{@code lang:xxx} 优先查笔记拆分语言表（assets/epca/lang/para_curse_note/），
     * 缺失再回退主语言文件，再不行返回键本身（见 {@link EPCANoteLang#resolveKey}）。
     * 非 {@code lang:} 前缀的名字原样显示。
     */
    private String translateName(String raw) {
        return EPCANoteLang.resolve(raw);
    }

    private String translateContent(String raw) {
        if (raw == null) return "";
        if (raw.startsWith(EPCANoteLang.LANG_PREFIX)) {
            return Component.translatable(raw.substring(EPCANoteLang.LANG_PREFIX.length())).getString();
        }
        return raw;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        float scale = getScaleFactor();
        var pose = guiGraphics.pose();
        pose.pushPose();

        double layoutX = convertMouseX(mouseX);
        double layoutY = convertMouseY(mouseY);

        if (scale != 1.0f) {
            pose.translate(width / 2.0f, height / 2.0f, 0);
            pose.scale(scale, scale, 1.0f);
            pose.translate(-width / 2.0f, -height / 2.0f, 0);
        }

        int hoveredParent = getClickedParentIndex((int) layoutX, (int) layoutY);
        int hoveredChild = getClickedChildIndex((int) layoutX, (int) layoutY);

        // 每次重画先清空 tooltip 暂存：只有这一帧真的悬停到了才会重新填
        pendingItemTooltip = null;
        pendingTextTooltip = null;

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

        drawPages(guiGraphics, (int) layoutX, (int) layoutY);

        drawFooter(guiGraphics, hoveredParent, hoveredChild);

        super.render(guiGraphics, (int) layoutX, (int) layoutY, partialTick);

        pose.popPose();

        // tooltip 必须在 pose 弹回之后再画：页内元素是在缩放后的 pose 里绘制的，
        // 在那里画 tooltip 会跟着一起缩小；这里用原始鼠标屏幕坐标画，位置与大小都正常。
        drawPendingTooltip(guiGraphics, mouseX, mouseY);
    }

    /** 画这一帧攒下的 tooltip（消耗提示优先；否则是掉落物图标的名字） */
    private void drawPendingTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (pendingTextTooltip != null && !pendingTextTooltip.isEmpty()) {
            guiGraphics.renderComponentTooltip(font, pendingTextTooltip, mouseX, mouseY);
            return;
        }
        if (pendingItemTooltip != null && !pendingItemTooltip.isEmpty()) {
            guiGraphics.renderTooltip(font, pendingItemTooltip, mouseX, mouseY);
        }
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

        // 0) 「消耗 N 物品 解锁」：只发请求包，成功与否由服务端同步决定
        ResourceLocation detailTarget = getClickedDetailCost((int) layoutX, (int) layoutY);
        if (detailTarget != null) {
            try {
                ModNetwork.sendToServer(new NoteDetailUnlockRequestPacket(detailTarget));
            } catch (Throwable ignored) {
            }
            return true;
        }

        // 1) 快捷跳转页上的寄生体名字：点一下直接跳到它的正文起始页（未解锁的条目没有目标页，点不动）
        int jumpTarget = getClickedJumpTarget((int) layoutX, (int) layoutY);
        if (jumpTarget != -1) {
            gotoPage(jumpTarget);
            return true;
        }

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

    /**
     * 书体（书皮 + 左右两页）来自 para_curse_note.png，按 PIXEL_SCALE 放大。
     * 贴图里的两个书签不再当装饰用：它们已经被真正的页签（drawTabPlates）取代，
     * 所以这里只画书体，避免页签与装饰书签重复出现。
     */
    private void drawBook(GuiGraphics guiGraphics) {
        guiGraphics.blit(NOTE_TEXTURE,
                bookX, bookY,
                BOOK_W, BOOK_H,
                BOOK_TEX_X, BOOK_TEX_Y, BOOK_TEX_W, BOOK_TEX_H,
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
    private void drawChildTabPlates(GuiGraphics guiGraphics, int hoveredChild) {
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
    private void drawParentTabPlates(GuiGraphics guiGraphics, int hoveredParent) {
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
    private void drawTabLabels(GuiGraphics guiGraphics) {
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
     * 从 NOTE_TEXTURE 截取一块书签，用 shader color 乘算着色（灰色书签 -> 配置颜色），
     * 再叠乘「全局亮度增益 BOOKMARK_BRIGHTNESS_GAIN」与「状态亮度系数」，逐通道 clamp 到 [0,1]。
     * 画完立刻把颜色恢复成 (1,1,1,1)，不会影响书体 / 文字 / 页面内容的颜色。
     */
    private void drawBookmarkPlate(GuiGraphics guiGraphics, int x, int y, int w, int h,
                                   int u, int v, int texW, int texH,
                                   int rgb, float brightness) {
        float gain = BOOKMARK_BRIGHTNESS_GAIN * brightness;
        float r = clamp01(((rgb >> 16) & 0xFF) / 255.0F * gain);
        float g = clamp01(((rgb >> 8) & 0xFF) / 255.0F * gain);
        float b = clamp01((rgb & 0xFF) / 255.0F * gain);
        RenderSystem.setShaderColor(r, g, b, 1.0F);
        try {
            guiGraphics.blit(NOTE_TEXTURE, x, y, w, h, u, v, texW, texH, TEX_SIZE, TEX_SIZE);
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /** 半透明白色高光：同样按书签形状 blit 一次，所以不会出现矩形色块 */
    private void drawBookmarkSheen(GuiGraphics guiGraphics, int x, int y, int w, int h,
                                   int u, int v, int texW, int texH, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, clamp01(alpha));
        try {
            guiGraphics.blit(NOTE_TEXTURE, x, y, w, h, u, v, texW, texH, TEX_SIZE, TEX_SIZE);
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
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
     * 父级书签是 78x39（3x），正文区 = 78 - 18 - 15 = 45px：
     *   * 一行放得下 5 个汉字（45px），"寄生体图鉴" 这种 5 字章节名正好占满并居中；
     *   * 6 字章节名（54px）用 font.split 按 45px 折成两行（39/9 = 4 行可用），仍居中不溢出；
     *   * 更长的名字同样按 45px 折行，行数不超过书签高度能放下的行数（39/9 = 4 行），
     *     并且逐行在正文区里居中，保证完整可读且绝不会画出书签。
     */
    private void drawHorizontalTabLabel(GuiGraphics guiGraphics, String text,
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
            guiGraphics.drawString(font, line, areaX + (areaW - lineW) / 2, textY + i * lineH, color, true);
        }
    }

    /**
     * 子级页签：竖排文字（一字一行、水平居中），在**固定 30x54** 的书签内部自适应。
     *
     * <p>可用高度 = 书签高 54 - 下端压进书体的 6 - 顶部 V 形缺口 3*3=9 = 39px。文字块
     * 始终在这个可用高度里垂直居中，宽度方向单字居中，所以任何字数都不会画到书签外面：</p>
     * <ol>
     *   <li>1~4 个字：9px 行距原样放得下（4*9=36 ≤ 39），不压缩、不缩放；</li>
     *   <li>5 个字往上：**先压行距** spacing = min(9, 39 / 字数)，下限
     *       {@link #CHILD_TAB_TEXT_MIN_SPACING}=6（再压字形就互相啃了）：
     *       5 个字 -> 7px（35）、6 个字 -> 6px（36）都还能一行不缩地读完；</li>
     *   <li>压完行距仍放不下（7 个字 42 &gt; 39、8 个字 48 &gt; 39 ……）：**整段文字
     *       poseStack.scale 等比缩小**，倍数 = 39 / (字数 * 行距)，下限
     *       {@link #CHILD_TAB_TEXT_MIN_SCALE}=0.6（7 个字 -> 0.92、8 个字 -> 0.81、
     *       10 个字 -> 0.65），完整显示、不截断、不溢出；</li>
     *   <li>缩到 0.6 倍还装不下的极端长名（≥11 个字）：只继续压行距把总高压进 39px，
     *       绝不溢出、绝不截断。</li>
     * </ol>
     *
     * <p>缩放围绕「顶部中心」做（translate 到 textTop 再 scale），水平方向仍严格居中；
     * 文字内容始终来自 {@code EPCANoteTabData.ChildTab.name}（经 EPCANoteLang 翻译），
     * 这里只负责排版。</p>
     */
    private void drawVerticalTabLabel(GuiGraphics guiGraphics, String text,
                                      int x, int y, int w, int h, int color) {
        if (text == null || text.isEmpty()) return;
        int count = text.length();
        int notch = CHILD_TAB_NOTCH_TEX * CHILD_TAB_SCALE;       // 顶部 V 形缺口，文字要避开
        int areaTop = y + notch;
        int areaH = Math.max(1, h - CHILD_TAB_TUCK - notch);     // 缺口以下、书体以上

        float spacing = lineHeight();
        float textScale = 1.0F;

        // 1) 优先压缩行距（下限 CHILD_TAB_TEXT_MIN_SPACING）
        if (count * spacing > areaH) {
            spacing = Math.max(CHILD_TAB_TEXT_MIN_SPACING, areaH / (float) count);
        }
        // 2) 行距压到下限仍放不下 -> 整段等比缩小（下限 CHILD_TAB_TEXT_MIN_SCALE）
        if (count * spacing > areaH) {
            textScale = Math.max(CHILD_TAB_TEXT_MIN_SCALE, areaH / (count * spacing));
        }
        // 3) 缩到下限还装不下（极端长名）-> 继续压行距，保证一定装进书签
        if (count * spacing * textScale > areaH) {
            spacing = areaH / (count * textScale);
        }

        int blockH = Math.max(1, Math.round(count * spacing * textScale));
        int textTop = areaTop + Math.max(0, (areaH - blockH) / 2);
        int centerX = x + w / 2;

        if (textScale < 1.0F) {
            var pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(centerX, textTop, 0.0F);
            pose.scale(textScale, textScale, 1.0F);
            for (int i = 0; i < count; i++) {
                String ch = text.substring(i, i + 1);
                int chW = font.width(ch);
                guiGraphics.drawString(font, ch,
                        Math.round(-chW / 2.0F), Math.round(i * spacing), color, true);
            }
            pose.popPose();
        } else {
            for (int i = 0; i < count; i++) {
                String ch = text.substring(i, i + 1);
                int chW = font.width(ch);
                guiGraphics.drawString(font, ch,
                        centerX - chW / 2, Math.round(textTop + i * spacing), color, true);
            }
        }
    }

    /**
     * 内容区严格限制在贴图里左右两页的纸面矩形内。
     * GuiGraphics.enableScissor 不跟随 pose 变换，所以缩放时要把裁剪框换算到缩放后的屏幕坐标。
     */
    private void enableAreaScissor(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        float s = getScaleFactor();
        if (s == 1.0f) {
            guiGraphics.enableScissor(x, y, x + w, y + h);
            return;
        }
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        int x1 = Math.round(cx + (x - cx) * s);
        int y1 = Math.round(cy + (y - cy) * s);
        int x2 = Math.round(cx + (x + w - cx) * s);
        int y2 = Math.round(cy + (y + h - cy) * s);
        guiGraphics.enableScissor(x1, y1, x2, y2);
    }

    /**
     * 画当前这一对页。
     *
     * 右页只在真有元素时才画；被「封页」（rightSealed）的右页如果没有元素就是空白页 ——
     * 空白本身也是要求的一部分（每个寄生体保底独占 2 页），所以这里不需要额外画底色。
     * 鼠标坐标传进去是为了让快捷跳转条目在悬停时变色（悬停不改变文字宽度，几何保持一致）。
     */
    private void drawPages(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (pages.isEmpty()) return;
        PageContent page = pages.get(currentPage);
        if (!page.leftElements.isEmpty()) {
            enableAreaScissor(guiGraphics, leftAreaX, leftAreaY, leftAreaW, leftAreaH);
            renderElements(guiGraphics, page.leftElements, leftAreaX, leftAreaY, leftAreaW, mouseX, mouseY);
            guiGraphics.disableScissor();
        }
        if (!page.rightElements.isEmpty()) {
            enableAreaScissor(guiGraphics, rightAreaX, rightAreaY, rightAreaW, rightAreaH);
            renderElements(guiGraphics, page.rightElements, rightAreaX, rightAreaY, rightAreaW, mouseX, mouseY);
            guiGraphics.disableScissor();
        }
    }

    private void drawFooter(GuiGraphics guiGraphics, int hoveredParent, int hoveredChild) {
        String hovered = null;
        if (hoveredChild >= 0 && hoveredChild < currentChildTabs.size()) {
            hovered = translateName(currentChildTabs.get(hoveredChild).name);
        } else if (hoveredParent >= 0 && hoveredParent < parentTabs.size()) {
            hovered = translateName(parentTabs.get(hoveredParent).name);
        }
        // 书体下方的标题：没有悬停页签时显示笔记标题，同样优先查拆分语言表
        String baseTitle = EPCANoteLang.resolveKey("epca.note.title",
                this.title == null ? "" : this.title.getString());
        String titleText = (hovered != null && !hovered.isEmpty()) ? hovered : baseTitle;

        // 标题：画在书体下方，带阴影，保证任何背景下都能看清
        String drawnTitle = fit(font, titleText, BOOK_W - 40);
        int titleX = bookX + BOOK_W / 2 - font.width(drawnTitle) / 2;
        int titleY = bookY + BOOK_H + 6;
        guiGraphics.drawString(font, drawnTitle, titleX, titleY, TITLE_TEXT, true);

        // 页码：书体底部深棕色书脊带的正中央
        if (!pages.isEmpty()) {
            String pageStr = (currentPage + 1) + "/" + pages.size();
            guiGraphics.drawCenteredString(font, pageStr, bookX + BOOK_W / 2,
                    bookY + BOOK_H - font.lineHeight - 4, TITLE_TEXT);
        }
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty()) return "";
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - 6)) + "…";
    }

    private void renderElements(GuiGraphics guiGraphics, List<RenderElement> elements,
                                int baseX, int baseY, int areaWidth, int mouseX, int mouseY) {
        int yOffset = 0;
        int lineH = lineHeight();
        for (RenderElement e : elements) {
            if (e instanceof TextLine text) {
                guiGraphics.drawString(font, text.formatted, baseX, baseY + yOffset, text.color, text.shadow);
                yOffset += lineH;
            } else if (e instanceof JumpEntryElement jump) {
                // 快捷跳转条目：已解锁 = 蓝色 + 下划线、可点击、悬停变浅蓝；
                // 未解锁 = 黑色乱码、不响应悬停（几何不变，命中判定也早就把它跳过了）
                int top = baseY + yOffset;
                boolean hovered = jump.unlocked
                        && mouseX >= baseX && mouseX < baseX + jump.width
                        && mouseY >= top && mouseY < top + lineH;
                guiGraphics.drawString(font, hovered ? jump.hoveredFormatted : jump.formatted,
                        baseX, top, PAGE_TEXT);
                yOffset += lineH;
            } else if (e instanceof CostLineElement cost) {
                // 「消耗 N 物品 解锁」：蓝色 + 下划线、可点击；悬停变浅蓝并弹 tooltip
                int top = baseY + yOffset;
                boolean hovered = mouseX >= baseX && mouseX < baseX + cost.width
                        && mouseY >= top && mouseY < top + lineH;
                guiGraphics.drawString(font, hovered ? cost.hoveredFormatted : cost.formatted,
                        baseX, top, PAGE_TEXT);
                if (hovered) {
                    pendingTextTooltip = cost.tooltip();
                }
                yOffset += lineH;
            } else if (e instanceof LootRowElement loot) {
                loot.render(guiGraphics, baseX, baseY + yOffset, areaWidth, mouseX, mouseY);
                yOffset += loot.getHeight(font);
            } else if (e instanceof ImageElement img) {
                ResourceLocation tex = img.texture;
                int drawW = Math.max(1, Math.min(img.width, areaWidth));
                int drawH = img.width <= 0 ? img.height : Math.max(1, img.height * drawW / img.width);
                boolean valid = false;
                try {
                    Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(tex);
                    if (resource.isPresent()) {
                        RenderSystem.setShaderTexture(0, tex);
                        guiGraphics.blit(tex,
                                baseX + (areaWidth - drawW) / 2, baseY + yOffset, 0, 0,
                                drawW, drawH, img.width, img.height);
                        valid = true;
                    }
                } catch (Exception ignored) {}
                if (!valid) {
                    guiGraphics.drawString(font, "Texture missing", baseX, baseY + yOffset, PAGE_TEXT_WARN);
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

        MinecraftForge.EVENT_BUS.unregister(eventListener);
        super.removed();
    }

    private interface RenderElement {
        int getHeight(Font font);
    }

    private static class TextLine implements RenderElement {
        final FormattedCharSequence formatted;
        /** 默认正文色；样式（颜色/粗体/下划线）已经写在 FormattedCharSequence 里的会覆盖它 */
        final int color;
        /** 是否绘制文字阴影；正文/详细数据块用 false（无阴影） */
        final boolean shadow;
        TextLine(FormattedCharSequence formatted) { this(formatted, PAGE_TEXT, false); }
        TextLine(FormattedCharSequence formatted, int color) { this(formatted, color, false); }
        TextLine(FormattedCharSequence formatted, int color, boolean shadow) {
            this.formatted = formatted;
            this.color = color;
            this.shadow = shadow;
        }
        @Override
        public int getHeight(Font font) { return font.lineHeight; }
    }

    /**
     * 快捷跳转页里的一行寄生体名字：已解锁 = 蓝色 + 下划线、可点击；
     * 未解锁 = §k§0 黑色乱码、不可点击。
     *
     * 几何只有一份：{@link #width} 是 {@code font.width(formatted)}（悬停版本只换颜色，
     * 字宽完全相同），{@link #layoutY} 是排版时算出来的页内纵向偏移。
     * 绘制（renderElements）、命中（hitJump）都直接用这两个值，所以点哪儿画哪儿。
     */
    private class JumpEntryElement implements RenderElement {
        /** 该名字在种阶 entries 里的下标，用来回填跳转目标页 */
        final int entryIndex;
        final FormattedCharSequence formatted;
        final FormattedCharSequence hoveredFormatted;
        final int width;
        /** 该条目是否已被玩家发现；未解锁恒为黑色乱码且没有跳转目标 */
        final boolean unlocked;
        /** 目标页码（所有段落排完后再回填），-1 表示还没有目标（未解锁永远是 -1） */
        int targetPage = -1;
        /** 页内纵向偏移，由 appendSection 在排版时写入 */
        int layoutY = 0;

        JumpEntryElement(int entryIndex, String text, int maxWidth, boolean unlocked) {
            this.entryIndex = entryIndex;
            this.unlocked = unlocked;
            String shown = text == null ? "" : text;
            // 太长的名字按页宽截断（保持可读，不越出内容区；截断后宽度仍然用 font.width 量）
            if (font.width(shown) > maxWidth) {
                shown = fit(font, shown, maxWidth);
            }
            Component normal;
            Component hovered;
            if (unlocked) {
                normal = Component.literal(shown)
                        .withStyle(ChatFormatting.BLUE)
                        .withStyle(style -> style.withUnderlined(true));
                hovered = Component.literal(shown)
                        .withStyle(ChatFormatting.AQUA)
                        .withStyle(style -> style.withUnderlined(true));
            } else {
                // 未解锁：黑色乱码（§k + §0），悬停版本与普通版本一致 —— 不给任何“可以点”的暗示
                normal = Component.literal(shown)
                        .withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.BLACK);
                hovered = normal;
            }
            this.formatted = Language.getInstance().getVisualOrder(normal);
            this.hoveredFormatted = Language.getInstance().getVisualOrder(hovered);
            this.width = font.width(this.formatted);
        }

        @Override
        public int getHeight(Font font) { return font.lineHeight; }
    }

    /**
     * 正文页里的「消耗 N 物品 解锁」那一行：蓝色下划线、可点击，悬停时弹出消耗 tooltip。
     *
     * <p>几何同样只有一份：{@link #width} 来自 {@code font.width}，{@link #layoutY} 由排版写入；
     * 绘制（renderElements）与点击命中（hitCost）共用它们。</p>
     */
    private class CostLineElement implements RenderElement {
        /** 这一行对应的实体 id（点击时原样发给服务端） */
        final ResourceLocation entityId;
        final List<EPCANoteDetailCosts.Cost> costs;
        final FormattedCharSequence formatted;
        final FormattedCharSequence hoveredFormatted;
        final int width;
        /** 页内纵向偏移，由 appendSection 在排版时写入 */
        int layoutY = 0;

        CostLineElement(ResourceLocation entityId, List<EPCANoteDetailCosts.Cost> costs) {
            this.entityId = entityId;
            this.costs = costs == null ? List.of() : List.copyOf(costs);
            String joiner = t("epca.note.detail.cost.joiner", " + ");
            List<String> parts = new ArrayList<>(this.costs.size());
            for (EPCANoteDetailCosts.Cost cost : this.costs) {
                parts.add(t("epca.note.detail.cost.part", "%s %s", cost.count(), cost.displayName()));
            }
            String shown = t("epca.note.detail.cost", "消耗 %s 解锁", String.join(joiner, parts));
            if (font.width(shown) > leftAreaW) {
                shown = fit(font, shown, leftAreaW);
            }
            Component normal = Component.literal(shown)
                    .withStyle(ChatFormatting.BLUE)
                    .withStyle(style -> style.withUnderlined(true));
            Component hovered = Component.literal(shown)
                    .withStyle(ChatFormatting.AQUA)
                    .withStyle(style -> style.withUnderlined(true));
            this.formatted = Language.getInstance().getVisualOrder(normal);
            this.hoveredFormatted = Language.getInstance().getVisualOrder(hovered);
            this.width = font.width(this.formatted);
        }

        /** tooltip：每种材料一行「消耗 N × 物品显示名」 */
        List<Component> tooltip() {
            List<Component> lines = new ArrayList<>(costs.size());
            for (EPCANoteDetailCosts.Cost cost : costs) {
                lines.add(Component.literal(t("epca.note.detail.cost.tooltip", "消耗 %s × %s",
                        cost.count(), cost.displayName())));
            }
            return lines;
        }

        @Override
        public int getHeight(Font font) { return font.lineHeight; }
    }

    /**
     * 「掉落物：」整行：标签 + 每个掉落物的**物品图标 + xN 数量**，按页宽自动换行。
     * 悬停图标时弹出物品名称 tooltip（tooltip 本身延后到 pose 弹回后再画，见 {@link #render}）。
     */
    private class LootRowElement implements RenderElement {
        private static final int ICON = 16;
        private static final int ROW_H = 18;
        private static final int COUNT_GAP = 1;
        private static final int BLOCK_GAP = 5;

        private final List<EPCANoteEntryStats.Drop> drops;
        private final String label;
        private final String emptyText;
        /** 每个图标的位置：{x, row}，下标与 drops 一一对应 */
        private final List<int[]> iconSlots = new ArrayList<>();
        private int rows = 1;

        LootRowElement(List<EPCANoteEntryStats.Drop> drops) {
            this.drops = drops == null ? List.of() : List.copyOf(drops);
            this.label = t("epca.note.detail.drops", "掉落物：");
            this.emptyText = t("epca.note.detail.drops.empty", "无");
            layoutSlots();
        }

        private void layoutSlots() {
            if (drops.isEmpty()) {
                rows = 1;
                return;
            }
            int x = font.width(label) + 2;
            int row = 0;
            for (int i = 0; i < drops.size(); i++) {
                int blockW = ICON + COUNT_GAP + font.width(countText(drops.get(i))) + BLOCK_GAP;
                if (x + blockW > leftAreaW && x > 0) {
                    row++;
                    x = 0;
                }
                iconSlots.add(new int[]{x, row});
                x += blockW;
            }
            rows = row + 1;
        }

        private String countText(EPCANoteEntryStats.Drop drop) {
            return t("epca.note.detail.count", "x%s", drop.countText);
        }

        @Override
        public int getHeight(Font font) {
            return Math.max(font.lineHeight, rows * ROW_H);
        }

        /** @param top 该元素的页内顶边（baseY + yOffset） */
        void render(GuiGraphics guiGraphics, int baseX, int top, int areaWidth,
                    int mouseX, int mouseY) {
            int textY = top + (ROW_H - font.lineHeight) / 2;
            guiGraphics.drawString(font, label, baseX, textY, PAGE_TEXT);
            if (drops.isEmpty()) {
                guiGraphics.drawString(font, emptyText, baseX + font.width(label), textY, PAGE_TEXT);
                return;
            }
            for (int i = 0; i < iconSlots.size(); i++) {
                int[] slot = iconSlots.get(i);
                EPCANoteEntryStats.Drop drop = drops.get(i);
                int iconX = baseX + slot[0];
                int iconY = top + slot[1] * ROW_H;
                guiGraphics.renderItem(drop.stack, iconX, iconY + 1);
                guiGraphics.drawString(font, countText(drop), iconX + ICON + COUNT_GAP,
                        iconY + (ROW_H - font.lineHeight) / 2, PAGE_TEXT);
                // 悬停图标 -> 记下物品 tooltip（同一帧只保留最后一个，视觉上不会有差别）
                if (mouseX >= iconX && mouseX < iconX + ICON
                        && mouseY >= iconY && mouseY < iconY + ICON) {
                    pendingItemTooltip = drop.stack;
                }
            }
        }
    }

    private static class ImageElement implements RenderElement {
        final ResourceLocation texture;
        final int width, height;
        ImageElement(ResourceLocation tex, int w, int h) { this.texture = tex; this.width = w; this.height = h; }
        @Override
        public int getHeight(Font font) { return height; }
    }

    /**
     * 一整对页（左页 + 右页）—— 笔记按“整对页”翻动，所以一页 = 一对左右页。
     *
     * {@code rightSealed} 是「强制右页留空」标记：一个段落（快捷跳转页 / 某个寄生体的正文）
     * 在本对页结束、右页还没排满时置位，表示右页只属于这个段落，后续段落不许占用。
     * 这样每个寄生体都保底独占 2 页（左 + 右），右页空着也不会被下一个寄生体填进来。
     */
    private static class PageContent {
        final List<RenderElement> leftElements;
        final List<RenderElement> rightElements;
        final boolean rightSealed;

        PageContent(List<RenderElement> left, List<RenderElement> right, boolean rightSealed) {
            this.leftElements = left;
            this.rightElements = right;
            this.rightSealed = rightSealed;
        }

        static PageContent empty() {
            return new PageContent(List.of(), List.of(), true);
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
            // 禁用态（首页的左键 / 末页的右键）画成暗色，视觉上能直接看出「这一侧不能再翻」；
            // 悬停时略提亮，强调可点击。命中判定仍由 AbstractWidget 用同一份 getX/getY/width/height 完成。
            boolean hovered = this.active
                    && mouseX >= getX() && mouseX < getX() + width
                    && mouseY >= getY() && mouseY < getY() + height;
            float tint = !this.active ? 0.35F : (hovered ? 1.15F : 1.0F);
            RenderSystem.setShaderColor(tint, tint, tint, 1.0F);
            try {
                RenderSystem.setShaderTexture(0, texture);
                guiGraphics.blit(texture, getX(), getY(), 0, 0, width, height, texWidth, texHeight);
            } finally {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}
