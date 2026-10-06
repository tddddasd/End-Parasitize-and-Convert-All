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
import org.tdddd.epca.impl.network.packet.c2s.NestLeaderOrganActionPacket;
import org.tdddd.epca.impl.overworld.data.BiomassClientData;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganGate;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotKind;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatReadout;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatReadout.ReadoutLabels;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  GUISPEC  2  2<b>600  430 </b>
 *
 * <h2></h2>
 * <p> 600430 </p>
 * <ul>
 *   <li><b></b> {@link #DESIGN_WIDTH}  {@link #DESIGN_HEIGHT} = <b>600  430</b>
 *       39<b></b> {@code TorsoOrganMap}
 *       3   9 33
 *        3316 <b></b>
 *        2 <b> / </b>
 *   <li><b></b>{@link #MODEL_SIZE} = <b>44</b> 62
 *        {@link NestLeaderOrganModelRotation}<b></b>
 *       <b> 2 </b>{@code OrganStatReadout#damageLines}
 *        2 {@code OrganStatDefaults#format(double)}
 *        GUI
 *       {@link NestLeaderOrganActionPacket#inventorySlot(int, int)}
 *        +  + scissor <b> 600430 </b>
 * </ul>
 *
 * <h2></h2>
 * <ol>
 *   <li><b> =  16 </b>1/16
 *        {@code size}  {@code MODEL_LOCAL_ANCHOR_Y = 32 / 2 = 16}
 *        {@code 16  44 = 704}  704px
 *        y  490 490px
 *       {@code enableScissor}
 *       {@link #MODEL_ANCHOR_OFFSET_PER_SIZE} = {@code 0.9375}
 *       =   {@code size}
 *        = {@code MODEL_CENTER_Y + 0.9375  modelSize}
 *       {@link ModelBody}  {@code x 279..321}{@code y 173..255}
 *        {@link #isOverModel(double, double)} </li>
 *   <li><b></b>{@code x 514..638}  9  600  38px
 *        scissor
 *       {@code x 480..588}{@code y 216..290} {@link #INVENTORY_STRIDE} = 12
 *       </li>
 *   <li><b></b>{@link #TORSO_INNER_STRIDE} 16  12
 *        12px  4px
 *        {@code x 282..318} {@code x 246..354}
 *        x = 300 </li>
 *   <li><b> Java map </b> {@code Map#toString()}
 *       {@code  {health=2, armor=1}}
 *       {@code   +2  +1}
 *       {@code epca.organ_gui.attribute.<key>.name}  2
 *        0  {@code OrganStatReadout#statsLines}</li>
 *   <li><b></b>scissor
 *        {@code AbstractContainerScreen#renderFloatingItem} /
 *       <b></b>
 *        {@code NestLeaderOrganClientData#onSync}
 *       <b></b> tick
 *
 *        tooltip <b></b>
 *       {@link #renderCarriedStack}</li>
 * </ol>
 * <p>12px {@link #ITEM_RENDER_SCALE}
 *  {@code epca.organ_gui.*} 31
 * {@link NestLeaderOrganModelRotation} 2 </p>
 *
 * <h2></h2>
 * <p> 600  430 {@link #PANEL_WIDTH}/{@link #PANEL_HEIGHT}
 *  + 16  + 27  + 9
 * + / + <b>36 39  + 9 </b></p>
 * <p> 600  430  44 + 18px  +  16<b></b>
 * <b></b><b></b> {@link #MODEL_SIZE} = 44 </p>
 * <ul>
 *   <li>{@link #SLOT_SIZE} 18  <b>12</b>{@link #SLOT_STRIDE} 16  <b>12</b>
 *       </li>
 *   <li>{@link #TORSO_INNER_STRIDE} 18  16  <b>12</b>
 *        = </li>
 *   <li>{@link #INVENTORY_STRIDE} 16  14  <b>12</b>
 *       9  {@code 8  12 + 12 = 108}px</li>
 *   <li><b> 94</b>
 *        3  =  935 1  =  08</li>
 * </ul>
 *
 * <h2> =  = {@code (300, 214)}{@link #MODEL_SIZE} = 44</h2>
 * <p> {@code layoutScale == 1}  {@code layoutScale}
 *  (x, y) + 12  12</p>
 * <pre>
 *  X:    0   132      246   282   318      444  480                    600
 *        |--  --|  |  |  |--  9 480..588--|
 *                         x = 300
 *
 *                          x                y
 *  - 339     282 + 12               48         x=3003   36px
 *  - 3927    246 + 12               96         x=3009   108px
 *                      282 .. 318                44         4px
 *  - 2                276 / 300                 148       2
 *  - 3                132 / 132 / 132           192 / 204 / 216
 *  - 3                444 / 444 / 444           192 / 204 / 216
 *                         (300, 214) 44         x 279..321y 173..255
 *  - 2                276 / 300                 262       2
 *  - 3                132 / 132 / 132           286 / 298 / 310
 *  - 3                444 / 444 / 444           286 / 298 / 310
 *   9436           480 + 12               228 / 240 / 252 935
 *                                                      278 08 480
 *             480                       216 12px
 *         480                       268 264  4px
 *  10      6                          308  10 100px
 *   2     384                        308
 * </pre>
 *
 * <p><b></b>1600 -276..324
 * <b></b> x = 300   y
 *  4 y 48..132 y 148
 * 2y 308  x 458..600
 *  y 148  y 305  x 354  456
 * / x 478
 *  {@code x 480..588}{@code 480 + 8  12 + 12}{@code y 216..290}
 *  216..225 +  228..264 +  268..277 +  278..290</p>
 *
 * <p><b></b> {@link #INVENTORY_LEFT} = 514
 *  14 {@code 514 + 8  14 + 12 = 638}  38px
 *  {@code enableScissor} {@link #outOfPanelCount()} = 9
 *  = {@code 480 + 8  12 + 12 = 588}{@code outOfPanelCount()}  0</p>
 *
 * <h2></h2>
 * <p>52 <b></b>
 * {@link OrganSlotGroup} [0,2)  [2,4)  [4,7)  [7,10)  [10,13)
 *  [13,16)  [16,43)  [43,52)</p>
 * <p> 3 33  7
 *
 * {@code NestLeaderOrganData#applyHeadInnerDefaultsOnce()}
 * {@code NestLeaderOrganSavedData#readOrCreate}
 *  33  NBT 39 </p>
 *
 * <h2> MenuType</h2>
 * <p> {@code AbstractContainerMenu} {@code MenuType} {@code Slot}
 *  {@code NestLeaderOrganCarry}
 *  {@code Inventory#items}
 *  {@code ServerPlayer#getInventory()}  tick
 * {@code containerMenu.broadcastChanges()}_tmp_vanilla_src/normalized ServerPlayer.java  435
 * </p>
 *
 * <h2> 600  430 </h2>
 *
 * <p> {@link #DESIGN_WIDTH}  {@link #DESIGN_HEIGHT} = 600  430
 * </p>
 * <ol>
 *   <li><b></b>
 *       {@code layoutScale = clamp(min(1, (width - MARGIN * 2) / 600, (height - MARGIN * 2) / 430),
 *       LAYOUT_SCALE_MIN, 1)} {@link #computeResponsiveLayout()}</li>
 *   <li> {@code PoseStack}
 *       {@code translate(panelX, panelY) -> scale(layoutScale)}
 *       {@link #render(GuiGraphics, int, int, float)}{@code GuiGraphics#pose()}
 *       _tmp_vanilla_src/net/minecraft/client/gui/GuiGraphics.java  115
 *       </li>
 *   <li><b></b>{@code panelX/panelY}
 *       {@link GuiGraphics#enableScissor(int, int, int, int)} 157
 *       <b> GUI </b> {@code PoseStack}  165
 *       {@code applyScissor}  {@code window.getGuiScale()} </li>
 *   <li><b></b>{@link #toLayoutX(double)} / {@link #toLayoutY(double)}
 *       {@code (mouse - panelPos) / layoutScale}
 *        {@code layoutScale} </li>
 * </ol>
 *
 * <p><b> {@code width/height}  GUI </b>
 * {@code Screen.init(Minecraft, int, int)}
 * {@code Minecraft#window.getGuiScaledWidth()/getGuiScaledHeight()}
 * _tmp_vanilla_src/net/minecraft/client/Minecraft.java  1007
 *  {@code Screen#resize}  429433
 *  {@code Window#setGuiScale(double)}  {@code framebufferWidth / guiScale}
 * _tmp_vanilla_src/com/mojang/blaze3d/platform/Window.java  378384
 *  GUI 1..4 {@code guiScale}</p>
 *
 * <h2></h2>
 * <p> {@code layoutScale}  {@link #LAYOUT_SCALE_MIN}
 *  {@link #computeResponsiveLayout()}  {@code compact}
 *  /
 *  0  33
 * {@link #COMPACT_HEAD_INNER_DROP}{@code enableScissor} </p>
 */
@OnlyIn(Dist.CLIENT)
public class NestLeaderOrganScreen extends Screen {

    //   600  430
    /**  + / +  +  */
    private static final int PANEL_WIDTH = 600;
    /**  4    4  +      */
    private static final int PANEL_HEIGHT = 430;
    /**  */
    private static final int PANEL_COLOR = 0xE0100618;
    /**  */
    private static final int PANEL_BORDER_COLOR = 0xFF5A2A7A;

    //   = 600  430 +
    /**  {@link #PANEL_WIDTH} */
    private static final int DESIGN_WIDTH = PANEL_WIDTH;
    /**  {@link #PANEL_HEIGHT} */
    private static final int DESIGN_HEIGHT = PANEL_HEIGHT;
    /**  GUI  */
    private static final int VIEWPORT_MARGIN = 6;
    /**
     * {@code layoutScale}
     *
     * <p>0.40  240  172 12px  4.8px 33px
     * {@code 1.875  44  0.40}
     * {@link #computeResponsiveLayout()}  {@code enableScissor}
     * </p>
     */
    private static final float LAYOUT_SCALE_MIN = 0.40F;
    /**
     *  33
     *
     * <p>4  48..84 84
     *  4  52..8896
     * </p>
     */
    private static final int COMPACT_HEAD_INNER_DROP = 4;
    /**  */
    private static final int COMPACT_READOUT_INSET = 8;

    //  SPEC C8/C9
    /** C8  */
    public static final int COLOR_STATS = 0xFFCC66FF;
    /** C9  */
    public static final int COLOR_DAMAGE_INFO = 0xFF55FF7A;
    private static final int COLOR_TITLE = 0xFFE9C7FF;
    private static final int COLOR_HINT = 0xFF9A8AA8;
    private static final int COLOR_WARN = 0xFFFFAA55;
    private static final int COLOR_DIVIDER = 0x66FF5555;
    /**  */
    private static final int READOUT_FADE = 0xCC100618;
    /**  /  /  */
    private static final int COLOR_GROUP_LABEL = 0x66E0B0FF;

    /**  */
    private static final int SLOT_LOCKED_FILL = 0xFF1E0A2A;
    /**  */
    private static final int SLOT_LOCKED_BORDER = 0xFF4A2A5A;
    /**  */
    private static final int SLOT_PURPLE_FILL = 0xFF3E1B52;
    /**  */
    private static final int SLOT_PURPLE_BORDER = 0xFFB060E0;
    /**  */
    private static final int SLOT_RED_FILL = 0xFF4A1420;
    /**  */
    private static final int SLOT_RED_BORDER = 0xFFE04040;
    /**  */
    private static final int SLOT_GATED_OVERLAY = 0x99000000;
    /**  */
    private static final int SLOT_HOVER_OVERLAY = 0x40FFFFFF;
    /**  */
    private static final int LOCK_COLOR = 0xFFD8C0F0;
    /**  */
    private static final int LOCK_SHADOW = 0xFF7A5A90;
    /**  */
    private static final int SLOT_INVENTORY_FILL = 0xFF20182C;
    /**  */
    private static final int SLOT_INVENTORY_BORDER = 0xFF6A5A80;

    //   lang zh_cn / en_us
    /** {@code Screen#title} */
    private static final String KEY_TITLE = "epca.organ_gui.title";
    /**  1  */
    private static final String KEY_HINT_DRAG = "epca.organ_gui.hint.drag";
    /**  2 Shift  */
    private static final String KEY_HINT_QUICK_MOVE = "epca.organ_gui.hint.quick_move";
    /**  3  */
    private static final String KEY_HINT_TRANSFER = "epca.organ_gui.hint.transfer";
    /**  4 ESC  */
    private static final String KEY_HINT_CLOSE = "epca.organ_gui.hint.close";
    /**  */
    private static final String KEY_HINT_SEPARATOR = "epca.organ_gui.hint.separator";
    /**  */
    private static final String KEY_WAITING = "epca.organ_gui.waiting_data";
    /**  */
    private static final String KEY_INVENTORY = "epca.organ_gui.inventory";
    /**  */
    private static final String KEY_HOTBAR = "epca.organ_gui.hotbar";
    /**  */
    private static final String KEY_NO_MODEL = "epca.organ_gui.model.none";
    /**  */
    private static final String KEY_GROUP_HEAD = "epca.organ_gui.group.head";
    /**  */
    private static final String KEY_GROUP_TORSO = "epca.organ_gui.group.torso";
    /**  */
    private static final String KEY_GROUP_LEFT_ARM = "epca.organ_gui.group.left_arm";
    /**  */
    private static final String KEY_GROUP_RIGHT_ARM = "epca.organ_gui.group.right_arm";
    /**  */
    private static final String KEY_GROUP_LEFT_LEG = "epca.organ_gui.group.left_leg";
    /**  */
    private static final String KEY_GROUP_RIGHT_LEG = "epca.organ_gui.group.right_leg";
    /**  39 */
    private static final String KEY_GROUP_TORSO_INNER = "epca.organ_gui.group.torso_inner";
    /**  33 */
    private static final String KEY_GROUP_HEAD_INNER = "epca.organ_gui.group.head_inner";
    /**  1  */
    private static final String KEY_READOUT_ADAPT_CHANCE = "epca.organ_gui.readout.adaptation_chance";
    /**  2  */
    private static final String KEY_READOUT_ADAPT_REDUCTION = "epca.organ_gui.readout.adaptation_reduction";
    /**  {@code OrganStatDefaults}  "%"  */
    private static final String KEY_READOUT_PERCENT = "epca.organ_gui.readout.percent";
    /**  */
    private static final String KEY_SOURCE_PURPLE = "epca.organ_gui.source.purple";
    /**  */
    private static final String KEY_SOURCE_TORSO_INNER = "epca.organ_gui.source.torso_inner";
    /**  */
    private static final String KEY_SOURCE_HEAD_INNER = "epca.organ_gui.source.head_inner";
    /**  + {@code ".name"}  */
    private static final String KEY_ATTRIBUTE_PREFIX = "epca.organ_gui.attribute.";
    /**  lang {@code %s} =  */
    private static final String KEY_BIOMASS = "epca.organ_gui.biomass";
    /**
     *  -&gt;
     *
     * <p>{@link Component#translatable(String)}  7
     * {@link Component}
     * {@code getString()}</p>
     */
    private static final Map<String, Component> ATTRIBUTE_LABELS = new ConcurrentHashMap<>();

    //  600430 18/16  12/12
    /**
     * <b></b>
     *
     * <p> 1816px  + 1px  600  430
     *  430  3  + 9  + 9
     * 18  <b>12</b></p>
     *
     * <p><b></b> {@code GuiGraphics#renderItem}
     * 16  16  {@code (x + 8, y + 8)}
     * _tmp_vanilla_src/net/minecraft/client/gui/GuiGraphics.java  469477
     * {@code translate(x + 8, y + 8, ...)} + {@code scale(16, 16, 16)}
     *  {@code x + 19} 521  12px
     * <b></b> 16px  12px
     * {@link #ITEM_RENDER_SCALE}
     * {@link #renderItemInCell}tooltip
     *  12  12 </p>
     */
    private static final int SLOT_SIZE = 12;
    /**
     * {@code SLOT_SIZE / 16.0} 1 =
     *
     * <p>12px   0.75 12  12 0.75
     * {@code Font}  16px  {@code x + 19}
     * GuiGraphics.java  473 / 521  12px  3px </p>
     */
    private static final float ITEM_RENDER_SCALE = Math.min(1.0F, SLOT_SIZE / 16.0F);
    /**  / 12 =  */
    private static final int SLOT_STRIDE = 12;
    /**
     *  33 39
     *
     * <p><b>12 = {@link #SLOT_SIZE}</b>
     *  18/16  12px  6px/4px </p>
     */
    private static final int TORSO_INNER_STRIDE = 12;
    /**
     *
     *
     * <p><b>12 = {@link #SLOT_SIZE}</b> 16/14  4px/2px
     * 9  {@code 8  12 + 12 = 108} </p>
     */
    private static final int INVENTORY_STRIDE = 12;

    /**  X */
    private static final int MODEL_CENTER_X = 300;
    /**  Y */
    private static final int MODEL_CENTER_Y = 214;

    /**
     *  39
     *
     * <p>9 8  + 1 = {@code 8 * 12 + 12 = 108}
     *  {@link #MODEL_CENTER_X}    = {@code 300 - 108 / 2 = 246}</p>
     */
    private static final int TORSO_INNER_LEFT = MODEL_CENTER_X - (8 * TORSO_INNER_STRIDE + SLOT_SIZE) / 2;
    /**  39 */
    private static final int TORSO_INNER_TOP = 96;
    /**
     *  33
     *
     * <p> {@code 2 * 12 + 12 = 36}   = {@code 300 - 36 / 2 = 282}</p>
     */
    private static final int HEAD_INNER_LEFT = MODEL_CENTER_X - (2 * TORSO_INNER_STRIDE + SLOT_SIZE) / 2;
    /**  33 */
    private static final int HEAD_INNER_TOP = 48;

    //  16  2
    /**  / 3  x */
    private static final int PURPLE_ARM_LEFT_X = 132;
    /**  / 3  x */
    private static final int PURPLE_ARM_RIGHT_X = 444;
    /**  / 2  x */
    private static final int PURPLE_CENTER_LEFT_X = 276;
    /**  /  x */
    private static final int PURPLE_CENTER_RIGHT_X = 300;
    /**  y */
    private static final int PURPLE_HEAD_TOP = 148;
    /**  /  y */
    private static final int PURPLE_ARM_TOP = 192;
    /**  y */
    private static final int PURPLE_TORSO_TOP = 262;
    /**  /  y */
    private static final int PURPLE_LEG_TOP = 286;

    //  39  + 9  94
    //
    // y 308  x 6..384
    //  x 384..600 9  12  108px  74px
    //  x 458..600  y 148  y 305
    //  x 354  456 / 478
    //  480>  478 480 + 8  12 + 12 = 588
    //  12px
    /**  9 8  12px  + 1  12px    108px 588 */
    private static final int INVENTORY_LEFT = 480;
    /** 39 12px= 216 */
    private static final int INVENTORY_TOP = 228;
    /**
     * 9
     *
     * <p>= {@code 228 + 3 * 12 = 264}+ 14 14px
     *  {@code HOTBAR_TOP - 10} = 268 9px  268..276
     * </p>
     */
    private static final int HOTBAR_TOP = INVENTORY_TOP + 3 * INVENTORY_STRIDE + 14;

    //   /
    /**  */
    private static final int READOUT_TOP_OFFSET = 308;
    /**  9 10  */
    private static final int READOUT_LINE_HEIGHT = 10;
    /**
     *
     *
     * <p>STAGE A  7  9  + = 12
     * 10  12<b></b>
     * {@code (430 - 308) / 10 = 12}  {@link #readoutLineLimit()}
     * /</p>
     */
    private static final int STATS_MAX_LINES = 12;
    /**  x */
    private static final int READOUT_LEFT_INSET = 6;
    /**  x */
    private static final int READOUT_GREEN_X = 384;
    /**  y */
    private static final int READOUT_GREEN_TOP = 308;

    /** 6 */
    private static final int BIOMASS_LEFT = READOUT_LEFT_INSET;
    /**  300  */
    private static final int BIOMASS_TOP = 8;
    /**
     * =  10
     *
     * <p> {@code Font#lineHeight} = 9  10
     * </p>
     */
    private static final int BIOMASS_LINE_HEIGHT = READOUT_LINE_HEIGHT;
    /**  */
    private static final int COLOR_BIOMASS = COLOR_STATS;

    /**
     * <b></b>44 62
     *
     * <p> {@code InventoryScreen}  30  176166  GUI  62
     *  62  <b>44</b> 71%<b> 44</b></p>
     *
     * <p><b>44 </b> {@code InventoryScreen#renderBg}
     * {@code renderEntityInInventory}  {@code size} 30
     * _tmp_vanilla_src/.../InventoryScreen.java  8690
     *  1  = {@code size}  2  1
     * {@link ModelBody#MODEL_LOCAL_HEIGHT} = 32  = 2
     *  0.9375
     * <b> 42  82 </b> {@code 1.875  44 = 82.5} {@code 0.9375  44 = 41.25}
     *  {@link ModelBody#of(int, int, int)}{@code x 279..321}{@code y 173..255}
     * y 148..160y 262..274
     *  13 / 7 px </p>
     *
     * <p> 33  44 1  1
     *  {@link #MODEL_ANCHOR_OFFSET_PER_SIZE}  16 </p>
     */
    private static final int MODEL_SIZE = 44;

    /**
     *  {@code size}
     *
     * <p>{@code PlayerRenderer#scale}
     * _tmp_vanilla_src/net/minecraft/client/renderer/entity/player/PlayerRenderer.java
     *  152154 {@code float f = 0.9375F; poseStack.scale(0.9375F, 0.9375F, 0.9375F);}</p>
     */
    private static final float PLAYER_MODEL_SCALE = 0.9375F;

    /**
     * = <b> {@code modelSize}</b>
     *
     * <h2> 0.9375</h2>
     * <ol>
     *   <li> {@code y = 8 .. +24}  {@link ModelBody#MODEL_LOCAL_HEIGHT} = 32
     *       <b></b>{@code HumanoidModel#createMesh} {@code addBox(-4,-8,-4, 8,8,8)}
     *        {@code addBox(-4,0,-2, 8,12,4)} {@code PartPose.offset(1.9, 12, 0)}
     *       _tmp_vanilla_src/net/minecraft/client/model/HumanoidModel.java  6268 </li>
     *   <li> = 1/16 {@code ModelPart}  cube  16
     *       _tmp_vanilla_src/net/minecraft/client/model/geom/ModelPart.java  154 / 290292
     *        = 32 / 16 = <b>2 </b></li>
     *   <li>{@code LivingEntityRenderer#render}  {@code translate(0, 1.501, 0)}  +1.5
     *        translate  {@code scale(0.9375)}  {@code scale(-1,-1,1)}
     *        _tmp_vanilla_src/.../LivingEntityRenderer.java  99101
     *       <b>{@code renderEntityInInventory}  y</b>
     *        2 </li>
     *   <li> {@link #PLAYER_MODEL_SCALE} = 0.9375</li>
     *   <li>{@code renderEntityInInventory}  {@code mulPoseMatrix(scaling(size, size, size))}
     *       1  = {@code size}  124128
     *        {@code translate(x, y, 50)} =
     *       {@code 2  0.9375  size = 1.875  size}   =
     *       {@code 0.9375  size}</li>
     * </ol>
     * <p><b> y=  y + {@link #MODEL_ANCHOR_OFFSET_PER_SIZE}  modelSize</b>
     * <b></b></p>
     *
     * <p><b> 16 16 </b>
     * {@code ModelBody.MODEL_LOCAL_HEIGHT / 2}=
     *  {@code size} {@code 16  44 = 704}
     * <b></b> 704px{@code MODEL_CENTER_Y = 214}   y  490
     *  490px {@code panelTop}  {@code layoutScale}
     *  {@code enableScissor}   </p>
     */
    private static final double MODEL_ANCHOR_OFFSET_PER_SIZE =
            (ModelBody.MODEL_LOCAL_HALF_HEIGHT / (double) ModelBody.MODEL_LOCAL_HEIGHT)  //  /  = 0.5
                    * (ModelBody.MODEL_LOCAL_HEIGHT / (double) ModelBody.MODEL_UNITS_PER_BLOCK) //  = 2
                    * PLAYER_MODEL_SCALE;                                                   //

    /**  /  / <b></b> */
    private final NestLeaderOrganModelRotation rotation = new NestLeaderOrganModelRotation();
    private boolean draggingModel;
    private double dragAnchorX;
    private double dragAnchorY;

    /** 52  -&gt; {left, top}{@link #init()}  */
    private final int[][] slotGeometry = new int[OrganSlotGroup.totalSlots()][2];
    /** 36  -&gt; {left, top}{@link #init()}  */
    private final int[][] inventoryGeometry =
            new int[NestLeaderOrganActionPacket.INVENTORY_SLOT_COUNT][2];
    /** "" */
    @Nullable
    private LivingEntity hostEntity;

    //  {@link #init()}
    /**  -&gt;  GUI  {@link #computeResponsiveLayout()} */
    private float layoutScale = 1.0F;
    /**  x */
    private int panelLeft;
    /**  y */
    private int panelTop;
    /**  {@link #MODEL_SIZE} */
    private int modelSize = MODEL_SIZE;
    /**  +  */
    private boolean compactLayout;
    /** {@link #computeResponsiveLayout()}  */
    private Rect statsBlock = new Rect(READOUT_LEFT_INSET, READOUT_TOP_OFFSET, DESIGN_WIDTH, DESIGN_HEIGHT);
    /**  */
    private Rect damageBlock = new Rect(READOUT_GREEN_X, READOUT_GREEN_TOP, DESIGN_WIDTH, DESIGN_HEIGHT);
    /**  x{@link #render(GuiGraphics, int, int, float)}  */
    private double layoutMouseX;
    /**  y */
    private double layoutMouseY;

    public NestLeaderOrganScreen() {
        //  lang LangDataCN/EN  src/generated/.../lang/*.json key  key
        super(Component.translatable(KEY_TITLE));
    }

    /**
     *  lang {@link #KEY_HINT_SEPARATOR}
     *
     * <p>/
     * </p>
     */
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
        // /
        computeResponsiveLayout();
        computeSlotGeometry();
        computeInventoryGeometry();
    }

    /**
     *
     *
     * <p> {@link #MODEL_ANCHOR_OFFSET_PER_SIZE}
     * 1  = 1/16 {@code ModelPart}  cube  16
     * _tmp_vanilla_src/net/minecraft/client/model/geom/ModelPart.java  154 / 290292
     * 1  = {@code size}  {@code renderEntityInInventory}  {@code mulPoseMatrix(scaling(size, size, size))}
     * _tmp_vanilla_src/.../InventoryScreen.java  124128
     *  0.9375{@code PlayerRenderer#scale}  152154
     * <b> /  {@code k = size  0.9375 / 16}</b></p>
     * <pre>
     *  k16 = 0.9375  size / 2      y =  Y       y =  Y +
     *  8k = size/2                 16k = size 8 +  8  16
     *  8k  8k                      / 4k
     * </pre>
     * <p>{@code MODEL_SIZE = 44}  {@code k = 2.578} <b>42  82</b>
     * {@code MODEL_CENTER_Y = 214}{@code MODEL_CENTER_X = 300}   y=173
     *  y=255 x=279..321 x=290..310</p>
     *
     * <p><b></b> {@link #isOverModel(double, double)}
     *
     * {@code modelScreenAnchorY()}=  + {@code 0.9375  modelSize}
     *  {@code k = size / 32}
     *  {@code y 192..236} {@code y 173..255}
     *  16 </p>
     */
    protected ModelBody modelBody() {
        return ModelBody.of(MODEL_CENTER_X, MODEL_CENTER_Y, modelSize());
    }

    /**
     * =  {@code renderEntityInInventory}  {@code size}
     *
     * <p><b> {@code layoutScale}</b>
     *  pose  {@code translate(panel) + scale(layoutScale)}
     *
     *  /
     * {@code layoutScale}{@link #computeResponsiveLayout()}  4
     * {@code 44  layoutScale}
     *  {@link #isOverModel(double, double)}
     *  {@code layoutScale}  1  pose </p>
     */
    protected int modelSize() {
        return this.modelSize;
    }

    /**
     *
     *
     * <p><b></b>
     *  {@link #isOverModel(double, double)}
     * </p>
     *
     * @param head   /
     * @param torso
     * @param leftArm   x
     * @param rightArm  x
     * @param leftLeg / rightLeg
     * @param feetY   y
     * @param topY    y
     */
    protected record ModelBody(Rect head, Rect torso, Rect leftArm, Rect rightArm,
                               Rect leftLeg, Rect rightLeg, int feetY, int topY) {

        /**  = 1/16  8  +24 */
        static final int MODEL_LOCAL_HEIGHT = 32;
        /**  +24   +8 */
        static final int MODEL_LOCAL_HALF_HEIGHT = MODEL_LOCAL_HEIGHT / 2;
        /**  = 16 {@code ModelPart}  cube  16 ModelPart.java  154 / 290292  */
        static final int MODEL_UNITS_PER_BLOCK = 16;

        /**
         *
         *
         * <p> /  = {@code size  PLAYER_MODEL_SCALE / 16}
         * {@code size} = {@code renderEntityInInventory}  size1  = size
         * {@link #PLAYER_MODEL_SCALE}  0.9375
         *  y = +24 8 16
         *   16k</p>
         */
        static ModelBody of(int centerX, int centerY, int size) {
            double k = size * PLAYER_MODEL_SCALE / MODEL_UNITS_PER_BLOCK;
            int halfHeight = (int) Math.round(MODEL_LOCAL_HALF_HEIGHT * k);
            int topY = centerY - halfHeight;
            int feetY = centerY + halfHeight;

            int bodyHalf = (int) Math.round(4.0D * k);  // 8  / 2
            int shoulderHalf = (int) Math.round(8.0D * k);  //  8
            int limbHalf = (int) Math.round(2.0D * k);  // /4  / 2

            int torsoTop = topY + (int) Math.round(8.0D * k);  //  8
            int armTop = torsoTop - (int) Math.round(2.0D * k);  //  2
            int armBottom = topY + (int) Math.round(18.0D * k);  //  2..10 6..18
            int legTop = topY + (int) Math.round(20.0D * k);  //  12..24 20
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

    /** / */
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

    /**
     *  52
     *
     * <p> {@link OrganSlotGroup}</p>
     * <ul>
     *   <li><b>16 </b> 2
     *        2  2
     *        3  3
     *        {@code PURPLE_*} <b></b></li>
     *   <li><b> 39</b> =   9 +
     *        ({@link #TORSO_INNER_LEFT}, {@link #TORSO_INNER_TOP})
     *       <b></b></li>
     *   <li><b> 33</b> 33
     *       ({@link #HEAD_INNER_LEFT}, {@link #headInnerTop()})</li>
     * </ul>
     */
    private void computeSlotGeometry() {
        //   2
        placeRow(OrganSlotGroup.HEAD, PURPLE_HEAD_TOP, PURPLE_CENTER_LEFT_X, PURPLE_CENTER_RIGHT_X);

        //   2
        placeRow(OrganSlotGroup.TORSO, PURPLE_TORSO_TOP, PURPLE_CENTER_LEFT_X, PURPLE_CENTER_RIGHT_X);

        //   /  3
        placeColumn(OrganSlotGroup.LEFT_ARM, PURPLE_ARM_LEFT_X, PURPLE_ARM_TOP);
        placeColumn(OrganSlotGroup.RIGHT_ARM, PURPLE_ARM_RIGHT_X, PURPLE_ARM_TOP);

        //   /  3
        placeColumn(OrganSlotGroup.LEFT_LEG, PURPLE_ARM_LEFT_X, PURPLE_LEG_TOP);
        placeColumn(OrganSlotGroup.RIGHT_LEG, PURPLE_ARM_RIGHT_X, PURPLE_LEG_TOP);

        //   39 3   9
        for (int local = 0; local < OrganSlotGroup.TORSO_INNER.size(); local++) {
            int global = OrganSlotGroup.TORSO_INNER.globalIndex(local);
            int row = local / OrganSlotGroup.TORSO_INNER_COLUMNS;
            int column = local % OrganSlotGroup.TORSO_INNER_COLUMNS;
            slotGeometry[global][0] = TORSO_INNER_LEFT + column * TORSO_INNER_STRIDE;
            slotGeometry[global][1] = TORSO_INNER_TOP + row * TORSO_INNER_STRIDE;
        }

        //   33 33
        int headInnerTop = headInnerTop();
        for (int local = 0; local < OrganSlotGroup.HEAD_INNER.size(); local++) {
            int global = OrganSlotGroup.HEAD_INNER.globalIndex(local);
            int row = local / OrganSlotGroup.HEAD_INNER_COLUMNS;
            int column = local % OrganSlotGroup.HEAD_INNER_COLUMNS;
            slotGeometry[global][0] = HEAD_INNER_LEFT + column * TORSO_INNER_STRIDE;
            slotGeometry[global][1] = headInnerTop + row * TORSO_INNER_STRIDE;
        }
    }

    /**  33 */
    protected int headInnerTop() {
        return HEAD_INNER_TOP + (this.compactLayout ? COMPACT_HEAD_INNER_DROP : 0);
    }

    /**
     * 2  /
     *
     * <p> 0 1  2  {@code localIndex % 2}
     * </p>
     */
    private void placeRow(OrganSlotGroup group, int top, int leftX, int rightX) {
        for (int local = 0; local < group.size(); local++) {
            slotGeometry[group.globalIndex(local)][0] = (local % 2 == 0) ? leftX : rightX;
            slotGeometry[group.globalIndex(local)][1] = top;
        }
    }

    /**
     * 3  /
     *
     * <p>{@code local = 0}  {@link #SLOT_STRIDE}</p>
     */
    private void placeColumn(OrganSlotGroup group, int leftX, int top) {
        for (int local = 0; local < group.size(); local++) {
            slotGeometry[group.globalIndex(local)][0] = leftX;
            slotGeometry[group.globalIndex(local)][1] = top + local * SLOT_STRIDE;
        }
    }

    /**
     *  36
     *
     * <p> {@link Inventory#items} 08 =
     *  {@link #HOTBAR_TOP}935 = 3  {@link #INVENTORY_TOP}
     *  {@link #INVENTORY_STRIDE} {@link #INVENTORY_LEFT}
     *  94</p>
     */
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

    //   +  +

    /**
     *
     *  {@link #layoutScale} {@link #panelLeft}/{@link #panelTop}
     *  {@link #modelSize}
     *
     * <p><b> = 600  430</b></p>
     * <pre>
     * natural     = min(1, (width  - 2 * {@link #VIEWPORT_MARGIN}) / {@link #DESIGN_WIDTH},
     *                      (height - 2 * {@link #VIEWPORT_MARGIN}) / {@link #DESIGN_HEIGHT})
     * layoutScale = clamp(natural, {@link #LAYOUT_SCALE_MIN}, 1)
     * panelWidth  = round({@link #DESIGN_WIDTH}  * layoutScale)
     * panelHeight = round({@link #DESIGN_HEIGHT} * layoutScale)
     * panelLeft   = round((width  - panelWidth ) / 2)   //
     * panelTop    = round((height - panelHeight) / 2)
     * modelSize   = {@link #MODEL_SIZE}                        //  layoutScale
     * </pre>
     *
     * <p> {@code width  2 * margin}  {@code height  2 * margin}
     * {@link #usableWidth()}/{@link #usableHeight()} {@code layoutScale > 0}
     *  0 / </p>
     *
     * <p><b></b>{@code natural < LAYOUT_SCALE_MIN}
     *
     * {@link #READOUT_TOP_OFFSET}
     *  33
     * {@link #COMPACT_HEAD_INNER_DROP} 0 </p>
     */
    private void computeResponsiveLayout() {
        int availableWidth = usableWidth();
        int availableHeight = usableHeight();

        // 1.  vs 1.0
        double naturalScale = Math.min(1.0D, Math.min(
                availableWidth / (double) DESIGN_WIDTH,
                availableHeight / (double) DESIGN_HEIGHT));
        this.layoutScale = (float) Math.max(LAYOUT_SCALE_MIN, naturalScale);

        // 2.
        //    layoutScale == 1  panelLeft/Top  (width - PANEL_WIDTH) / 2
        int panelWidth = Math.round(DESIGN_WIDTH * this.layoutScale);
        int panelHeight = Math.round(DESIGN_HEIGHT * this.layoutScale);
        this.panelLeft = Math.round((this.width - panelWidth) / 2.0F);
        this.panelTop = Math.round((this.height - panelHeight) / 2.0F);

        // 3.
        boolean floored = naturalScale < LAYOUT_SCALE_MIN;

        // 4.  layoutScale
        //     pose  scale(layoutScale)
        //     44  44  layoutScale
        //     isOverModel  layoutScale  layoutScale  1
        this.modelSize = MODEL_SIZE;

        // 5.  /
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
            // x y
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

    /**
     *
     */
    private Rect readoutDamageBlock(int designRightLimit, int panelViewportBottom) {
        if (!this.compactLayout) {
            return new Rect(READOUT_GREEN_X, READOUT_GREEN_TOP,
                    designRightLimit, panelViewportBottom - this.panelTop);
        }
        int right = Math.max(this.statsBlock.left() + 40, designRightLimit);
        return new Rect(this.statsBlock.left(), this.statsBlock.bottom() + 8, right,
                panelViewportBottom - this.panelTop);
    }

    /**  {@code Screen#width}  */
    protected int usableWidth() {
        return Math.max(1, this.width - VIEWPORT_MARGIN * 2);
    }

    /**  */
    protected int usableHeight() {
        return Math.max(1, this.height - VIEWPORT_MARGIN * 2);
    }

    /**  ->  x +  */
    protected double designToScreenX(double designX) {
        return this.panelLeft + designX * this.layoutScale;
    }

    /**  ->  y +  */
    protected double designToScreenY(double designY) {
        return this.panelTop + designY * this.layoutScale;
    }

    /**  x ->  {@code MouseHandler}  */
    protected double toLayoutX(double screenX) {
        return (screenX - this.panelLeft) / this.layoutScale;
    }

    /**  y ->  */
    protected double toLayoutY(double screenY) {
        return (screenY - this.panelTop) / this.layoutScale;
    }

    /**  x{@link #render(GuiGraphics, int, int, float)}  */
    protected double layoutMouseX() {
        return this.layoutMouseX;
    }

    /**  y */
    protected double layoutMouseY() {
        return this.layoutMouseY;
    }

    //   translate + scale

    /**  GUI  */
    protected int panelLeft() {
        return this.panelLeft;
    }

    /**  GUI  */
    protected int panelTop() {
        return this.panelTop;
    }

    /**  GUI  */
    protected int panelScreenWidth() {
        return Math.round(DESIGN_WIDTH * this.layoutScale);
    }

    /**  GUI  */
    protected int panelScreenHeight() {
        return Math.round(DESIGN_HEIGHT * this.layoutScale);
    }

    /**  */
    protected float layoutScale() {
        return this.layoutScale;
    }

    /**  +  */
    protected boolean compactLayout() {
        return this.compactLayout;
    }

    /**  Y  */
    protected int modelCenterY() {
        return MODEL_CENTER_Y;
    }

    /**
     *  y {@code renderEntityInInventory}  y  = <b></b>
     *
     * <p><b></b>
     * {@link #renderRotatableModel} pose
     * {@code translate(panelLeft, panelTop) + scale(layoutScale)}
     *  {@link #render(GuiGraphics, int, int, float)}
     *  {@code translate(x, y, 50)}
     * _tmp_vanilla_src/.../InventoryScreen.java  124128
     *  x / y    {@code panelTop}
     *  {@code layoutScale}
     *  16  490px  scissor </p>
     *
     * <p> =  {@link #MODEL_CENTER_Y} + {@link #MODEL_ANCHOR_OFFSET_PER_SIZE}
     *  {@link #modelSize()}<b></b></p>
     */
    protected int modelScreenAnchorY() {
        return MODEL_CENTER_Y + (int) Math.round(MODEL_ANCHOR_OFFSET_PER_SIZE * this.modelSize);
    }

    /**  */
    protected Rect statsBlock() {
        return this.statsBlock;
    }

    /**  */
    protected Rect damageBlock() {
        return this.damageBlock;
    }

    /**  Y */
    protected int readoutTop() {
        return this.statsBlock.top();
    }

    /**  Y */
    protected int readoutDamageTop() {
        return this.damageBlock.top();
    }

    /**
     *  {@link #STATS_MAX_LINES}
     *
     * <p>
     * {@link #computeResponsiveLayout()}  {@code min(panelBottom, windowHeight)}
     *  {@link #STATS_MAX_LINES}
     *  0</p>
     */
    protected int readoutLineLimit() {
        if (statsBlock.right() <= statsBlock.left()) return 0;
        int bottom = this.statsBlock.bottom() - this.statsBlock.top();
        if (bottom <= 0) return 0;
        return Math.max(0, Math.min(STATS_MAX_LINES, bottom / READOUT_LINE_HEIGHT));
    }

    /**  = 2 */
    protected int damageLineLimit() {
        if (damageBlock.right() <= damageBlock.left()) return 0;
        int available = damageBlock.bottom() - damageBlock.top();
        if (available <= 0) return 0;
        return Math.max(0, Math.min(OrganStatReadout.DAMAGE_LINE_COUNT,
                available / READOUT_LINE_HEIGHT));
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //  tick
        this.rotation.update(deltaTick());

        //  ->  GUI
        // MouseHandler  xpos * guiScaledWidth / screenWidth _tmp_vanilla_src/...
        // MouseHandler.java  8485 / 218219
        this.layoutMouseX = toLayoutX(mouseX);
        this.layoutMouseY = toLayoutY(mouseY);

        // 1.20.1  renderBackground(GuiGraphics) super.render
        this.renderBackground(graphics);

        //  +
        int panelX = this.panelLeft;
        int panelY = this.panelTop;
        int panelW = panelScreenWidth();
        int panelH = panelScreenHeight();

        //  1
        //  tooltip
        // enableScissor  GUI  PoseStack
        // GuiGraphics#applyScissor  window.getGuiScale()
        //  _tmp_vanilla_src/net/minecraft/client/gui/GuiGraphics.java  157 / 165
        //  pushPose/scale
        int clipLeft = Math.max(0, panelX);
        int clipTop = Math.max(0, panelY);
        int clipRight = Math.min(this.width - 1, panelX + panelW);
        int clipBottom = Math.min(this.height - 1, panelY + panelH);
        boolean scissor = clipRight > clipLeft && clipBottom > clipTop;
        if (scissor) {
            graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        }

        // pushPose -> translate -> scale
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
        if (!NestLeaderOrganClientData.hasData()) {
            graphics.drawCenteredString(this.font, Component.translatable(KEY_WAITING),
                    DESIGN_WIDTH / 2, 32, COLOR_WARN);
        }

        renderPlayerModel(graphics);
        renderSlots(graphics, (int) Math.floor(this.layoutMouseX), (int) Math.floor(this.layoutMouseY));
        renderInventory(graphics, (int) Math.floor(this.layoutMouseX), (int) Math.floor(this.layoutMouseY));
        renderReadouts(graphics);

        //  super.render  super.renderBackground 1.20.1  Screen
        //  addRenderableWidget
        //   layoutMouseX/Y
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.pose().popPose();
        if (scissor) {
            graphics.disableScissor();
        }

        //  tooltip
        //  tooltip /  /
        //  AbstractContainerScreen#render  floating item
        // _tmp_vanilla_src/.../AbstractContainerScreen.java  118133
        //  172179  renderFloatingItem
        renderSlotTooltip(graphics, mouseX, mouseY);
        renderCarriedStack(graphics, mouseX, mouseY);
    }

    /**  {@code Minecraft#getDeltaFrameTime()} tick  0..1 tick  */
    private float deltaTick() {
        Minecraft minecraft = this.minecraft;
        return minecraft == null ? 1.0F : minecraft.getDeltaFrameTime();
    }

    /**  52  {@link OrganSlotGroup} */
    protected void renderSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        NestLeaderOrganData data = NestLeaderOrganClientData.data();
        NestLeaderOrganGate.Gates gates = NestLeaderOrganClientData.gates();

        for (OrganSlotGroup group : OrganSlotGroup.ALL) {
            for (int local = 0; local < group.size(); local++) {
                int index = group.globalIndex(local);
                //  translate + scalemouseX/mouseY
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

        //  33  39  6
        int dividerY = headInnerTop() - 4;
        graphics.fill(HEAD_INNER_LEFT, dividerY,
                HEAD_INNER_LEFT + OrganSlotGroup.HEAD_INNER.columns() * TORSO_INNER_STRIDE,
                dividerY + 1, COLOR_DIVIDER);

        // """"
        // /
        renderGroupLabel(graphics, OrganSlotGroup.HEAD, KEY_GROUP_HEAD);
        renderGroupLabel(graphics, OrganSlotGroup.TORSO, KEY_GROUP_TORSO);
        renderGroupLabel(graphics, OrganSlotGroup.LEFT_ARM, KEY_GROUP_LEFT_ARM);
        renderGroupLabel(graphics, OrganSlotGroup.RIGHT_ARM, KEY_GROUP_RIGHT_ARM);
        renderGroupLabel(graphics, OrganSlotGroup.LEFT_LEG, KEY_GROUP_LEFT_LEG);
        renderGroupLabel(graphics, OrganSlotGroup.RIGHT_LEG, KEY_GROUP_RIGHT_LEG);

        //  lang
        // /
        // - 156 - 444
        //  x 6..218
        //  230
        renderRegionLabelLeft(graphics, slotGroupBounds(OrganSlotGroup.HEAD_INNER),
                KEY_GROUP_HEAD_INNER);
        renderRegionLabelLeft(graphics, slotGroupBounds(OrganSlotGroup.TORSO_INNER),
                KEY_GROUP_TORSO_INNER);
    }

    /**  null */
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

    /** / lang key  key  */
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

    /**
     * <b></b>
     *
     * <p>{@code x = 6}
     *  6..218
     * 4   36px 55px</p>
     */
    private void renderRegionLabelLeft(GuiGraphics graphics, @Nullable Rect bounds, String langKey) {
        if (bounds == null) return;
        Component label = Component.translatable(langKey);
        drawLabel(graphics, label, 6 + this.font.width(label) / 2, bounds.centerY() - 4);
    }

    /**
     *
     *
     * <p><b></b>
     * {@link #compactLayout} </p>
     */
    private void drawLabel(GuiGraphics graphics, Component label, int centeredX, int y) {
        int width = this.font.width(label);
        if (rectsOverlapPx(centeredX - width / 2, y, width, this.font.lineHeight, this.statsBlock)
                || rectsOverlapPx(centeredX - width / 2, y, width, this.font.lineHeight,
                        this.damageBlock)) {
            return;
        }
        graphics.drawCenteredString(this.font, label, centeredX, y, COLOR_GROUP_LABEL);
    }

    /**  +  +  */
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

    /**
     *  {@code SLOT_SIZE  SLOT_SIZE} <b> (x, y)</b>
     *
     * <h2> {@code renderItem(stack, x, y)}</h2>
     * <p> {@code GuiGraphics#renderItem}  16  16
     * </p>
     * <ul>
     *   <li> {@code (x + 8, y + 8)}
     *       {@code scale(16, 16, 16)}_tmp_vanilla_src/net/minecraft/client/gui/GuiGraphics.java
     *        469477  {@code x .. x + 16}</li>
     *   <li> {@code x + 19 - 2 - font.width(count)} 521
     *        {@code x + 2 .. x + 15} {@code x .. x + 16} 524539 </li>
     * </ul>
     * <p> {@link #SLOT_SIZE} = 12
     * {@code translate(x, y)} {@code scale(ITEM_RENDER_SCALE)}
     * 16  16  12  12
     * {@code 12 - 3  0.75 = 9.75}
     * {@link #renderSlotBackground}tooltip
     * {@link #renderSlotTooltip}{@link #slotAt} / {@link #inventorySlotAt}
     * </p>
     *
     * <p>  16  {@link #ITEM_RENDER_SCALE}  1
     * </p>
     */
    protected void renderItemInCell(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(ITEM_RENDER_SCALE, ITEM_RENDER_SCALE, 1.0F);
        //  + (0, 0) 0..16 19
        graphics.renderItem(stack, 0, 0);
        graphics.renderItemDecorations(this.font, stack, 0, 0);
        graphics.pose().popPose();
    }

    /**
     * 39  + 9  035 = {@link Inventory#items}  36
     *
     * <p><b></b> 94 3 935
     *  {@link #INVENTORY_TOP}  1 08{@link #HOTBAR_TOP}
     * {@code INVENTORY_TOP - 12} / {@code HOTBAR_TOP - 10}
     *  {@link #INVENTORY_LEFT}  {@code x 480..588}{@code y 216..290}
     *  52 </p>
     *
     * <p>
     * container </p>
     */
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

    /**  null */
    @Nullable
    protected Inventory clientInventory() {
        AbstractClientPlayer player = localPlayer();
        return player == null ? null : player.getInventory();
    }

    /**  */
    private static ItemStack inventoryItem(Inventory inventory, int index) {
        if (index < 0 || index >= inventory.items.size()) return ItemStack.EMPTY;
        return inventory.items.get(index);
    }

    /**
     * 89
     *
     * <p> GUI {@code widget/lock} 1.20.1
     *  {@link GuiGraphics#fill}
     * </p>
     */
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

    /**
     *  tooltip
     *
     * <p><b></b>
     * {@code AbstractContainerScreen#renderTooltip}  {@code menu.getCarried().isEmpty()}
     * _tmp_vanilla_src/.../AbstractContainerScreen.java  160166
     * </p>
     *
     * <p><b></b><b></b>52 + 36
     *  tooltip <b></b></p>
     */
    private void renderSlotTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!NestLeaderOrganClientData.carried().isEmpty()) return;
        int layoutX = (int) Math.floor(this.layoutMouseX);
        int layoutY = (int) Math.floor(this.layoutMouseY);
        int hovered = slotAt(layoutX, layoutY);
        if (hovered >= 0) {
            ItemStack stack = NestLeaderOrganClientData.data().getItem(hovered);
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

    /**
     *
     *
     * <p><b></b> {@code popPose()}  {@code disableScissor()}
     *  {@link #render(GuiGraphics, int, int, float)}
     * {@code mouseX}/{@code mouseY}
     * scissor    {@code renderSlotTooltip} </p>
     *
     * <p><b></b> {@code AbstractContainerScreen#renderFloatingItem}
     * _tmp_vanilla_src/.../AbstractContainerScreen.java  172179
     * {@code pushPose -> translate(0, 0, 232) -> renderItem(x, y) ->
     * renderItemDecorations(font, x, y) -> popPose}
     * {@code x, y = mouseX - 8, mouseY - 8} 120132
     * {@code renderItemDecorations}  64  "64"</p>
     *
     * <p><b></b>{@link NestLeaderOrganClientData#carried()}
     *  {@code NestLeaderOrganCarry}
     * </p>
     */
    protected void renderCarriedStack(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack carried = NestLeaderOrganClientData.carried();
        if (carried.isEmpty()) return;
        int x = mouseX - 8;
        int y = mouseY - 8;
        graphics.pose().pushPose();
        //  floating item  z = 232GuiGraphics#renderItem  z  150
        //  GUI
        graphics.pose().translate(0.0F, 0.0F, 232.0F);
        graphics.renderItem(carried, x, y);
        graphics.renderItemDecorations(this.font, carried, x, y);
        graphics.pose().popPose();
    }


    /**
     * {@link #KEY_BIOMASS}
     *
     * <p>
     * {@code OrganSlotUnlock}
     *  {@code DESIGN_WIDTH / 2} </p>
     *
     * <p><b></b>{@code BiomassClientData#getPoints()}   HUD
     * {@code BiomassHUD} +
     *  {@code BiomassSyncPacket}  /
     * </p>
     *
     * <p><b></b> {@code (BIOMASS_LEFT, BIOMASS_TOP) = (6, 8)}
     *  {@code (6, 8) - (6 + font.width(label), 18)}
     *  =  = {@link #BIOMASS_LINE_HEIGHT} = 10 9
     *  y = 20  2px
     *  300  104px 248
     *  64 y = 48 y = 96
     *  y &gt;= 148 y &gt;= 173 y &gt;= 216 y &gt;= 308
     *  {@code scale(layoutScale)}
     * </p>
     *
     * <p> {@code drawLabel}
     *
     * </p>
     */
    protected void renderBiomassReadout(GuiGraphics graphics) {
        Component label = Component.translatable(KEY_BIOMASS, BiomassClientData.getPoints());
        int width = this.font.width(label);
        if (rectsOverlapPx(BIOMASS_LEFT, BIOMASS_TOP, width, BIOMASS_LINE_HEIGHT, this.statsBlock)
                || rectsOverlapPx(BIOMASS_LEFT, BIOMASS_TOP, width, BIOMASS_LINE_HEIGHT,
                        this.damageBlock)) {
            return;
        }
        //  = 300 - /2
        int limit = Math.min(DESIGN_WIDTH - READOUT_LEFT_INSET,
                DESIGN_WIDTH / 2 - this.font.width(this.title) / 2);
        if (BIOMASS_LEFT + width > limit) {
            return;
        }
        graphics.drawString(this.font, label.getString(), BIOMASS_LEFT, BIOMASS_TOP, COLOR_BIOMASS, true);
    }

    /**
     * SPEC C8SPEC C9 2
     *
     * <p>
     * {@link GuiGraphics#drawString(net.minecraft.client.gui.Font, String, int, int, int, boolean)}
     *  {@code shadow = true}  {@link OrganStatSummary}
     * </p>
     */
    protected void renderReadouts(GuiGraphics graphics) {
        OrganStatSummary summary = OrganStatSummary.compute(NestLeaderOrganClientData.data());
        //  /  /  /  /
        //  lang  OrganStatReadout / OrganStatDefaults
        //  2  2
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

        //  /
        int damageX = this.damageBlock.left();
        int damageY = readoutDamageTop();
        for (int i = 0; i < damageLines.size(); i++) {
            graphics.drawString(this.font, damageLines.get(i), damageX,
                    damageY + i * READOUT_LINE_HEIGHT, COLOR_DAMAGE_INFO, true);
        }
    }

    /**
     *  lang
     *
     * <p>
     * {@link ReadoutLabels#attributeName(String)}  9
     *  {@link Component}  {@link #ATTRIBUTE_LABELS} </p>
     */
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

    /**  null */
    @Nullable
    protected Registry<DamageType> damageTypeRegistry() {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null || minecraft.level == null) return null;
        return minecraft.level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
    }


    protected void renderPlayerModel(GuiGraphics graphics) {
        AbstractClientPlayer player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null) {
            // ""
            graphics.drawCenteredString(this.font, Component.translatable(KEY_NO_MODEL),
                    MODEL_CENTER_X, modelCenterY() - 4, COLOR_HINT);
            return;
        }
        this.hostEntity = player;
        // x / y  renderEntityInInventory
        // PoseStack  translate(x, y, 50)  scale(size)InventoryScreen.java  124128
        //  translate  scale(layoutScale)
        // =
        //
        // translate  z = 50  scale  scale(s, s, 1)
        // z  z = 0 z  28..72
        //  GuiGraphics#flush_tmp_vanilla_src/.../GuiGraphics.java  123127
        //  disableDepthTest  endBatch  =
        //  ->  flush-> //
        renderRotatableModel(graphics, MODEL_CENTER_X, modelScreenAnchorY(),
                this.modelSize, this.rotation.yaw(), this.rotation.pitch(), player);
    }

    /**
     *  +
     *
     * <p>{@code anchorX} / {@code anchorY}
     * {@code InventoryScreen#renderEntityInInventoryFollowsAngle}
     * <b> PoseStack </b> =
     * <b></b> {@code 1.875  size}
     * 2   0.9375  size {@link #MODEL_ANCHOR_OFFSET_PER_SIZE}</p>
     *
     * @param anchorX       x
     * @param anchorY       y
     * @param yawDegrees   180 =
     * @param pitchDegrees  =
     */
    public static void renderRotatableModel(GuiGraphics graphics, int anchorX, int anchorY,
                                            int size,
                                            float yawDegrees, float pitchDegrees, LivingEntity entity) {
        // entity.setYRot(180 + f * 40)""InventoryScreen.java  112
        float yawComponent = (yawDegrees - 180.0F) / 40.0F;
        // entity.setXRot(-f1 * 20) 113
        float pitchComponent = -pitchDegrees / 20.0F;
        InventoryScreen.renderEntityInInventoryFollowsAngle(
                graphics, anchorX, anchorY, size, yawComponent, pitchComponent, entity);
    }

    //
    //  mouseClicked/mouseDragged/mouseReleased  GUI
    // MouseHandler.java  8485 / 140141 / 218219
    //  toLayoutX/toLayoutY =  layoutScale

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double layoutX = toLayoutX(mouseX);
        double layoutY = toLayoutY(mouseY);
        if (button == NestLeaderOrganActionPacket.BUTTON_LEFT
                || button == NestLeaderOrganActionPacket.BUTTON_RIGHT
                || button == NestLeaderOrganActionPacket.BUTTON_MIDDLE) {
            int slot = slotAt(layoutX, layoutY);
            if (slot >= 0 && !this.draggingModel) {
                NestLeaderOrganActionPacket.Action action;
                if (button == NestLeaderOrganActionPacket.BUTTON_MIDDLE) {
                    //  = SPEC ""
                    action = NestLeaderOrganActionPacket.Action.CLEAR;
                } else if (hasShiftDown()) {
                    // Shift  =
                    action = NestLeaderOrganActionPacket.Action.QUICK_MOVE;
                } else {
                    action = NestLeaderOrganActionPacket.Action.CLICK;
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

        if (button == NestLeaderOrganActionPacket.BUTTON_LEFT && !isInsidePanel(layoutX, layoutY)) {
            if (NestLeaderOrganClientData.hasPendingLocalCarried()) {
                //  = ""
                sendReturnCarried();
                return true;
            }
        }

        if (button == NestLeaderOrganActionPacket.BUTTON_LEFT && isOverModel(layoutX, layoutY)) {
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
        if (this.draggingModel && button == NestLeaderOrganActionPacket.BUTTON_LEFT) {
            //  dragX/dragY
            float deltaX = (float) (mouseX - this.dragAnchorX);
            float deltaY = (float) (mouseY - this.dragAnchorY);
            this.dragAnchorX = mouseX;
            this.dragAnchorY = mouseY;

            //  /  /  rotation
            //  1  = 1 / layoutScale
            //  layoutScale
            // /GUI layoutScale == 1
            float sensitivity = this.layoutScale;
            this.rotation.dragged(deltaX * sensitivity, deltaY * sensitivity);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingModel && button == NestLeaderOrganActionPacket.BUTTON_LEFT) {
            this.draggingModel = false;
            this.rotation.endDrag();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     *
     *
     * <p>{@link #modelBody()}{@link ModelBody}
     * {@link #modelScreenAnchorY()}
     * <b></b>
     *  6px </p>
     *
     * <p><b></b> {@link #toLayoutX(double)}
     *  {@link #modelBody()}    pose
     * {@code layoutScale} </p>
     */
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

    /**  */
    protected boolean isInsidePanel(double mouseX, double mouseY) {
        return mouseX >= 0 && mouseX <= DESIGN_WIDTH && mouseY >= 0 && mouseY <= DESIGN_HEIGHT;
    }

    /**  -1 */
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

    /**  -1 */
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

    /**  +  */
    private static boolean rectsOverlapPx(int x, int y, int width, int height, Rect other) {
        return x < other.right() && other.left() < x + width
                && y < other.bottom() && other.top() < y + height;
    }


    /**
     *
     *  {@link NestLeaderOrganClientData}
     *
     * <p><b></b>{@code NestLeaderOrganActionHandler#click} /
     * {@code #quickMove} / {@code #clear}
     * {@code expectedSlot}
     * {@code carriedAfter}
     * {@link NestLeaderOrganClientData#predictOrganSlotAction} </p>
     */
    protected void sendSlotAction(NestLeaderOrganActionPacket.Action action, int slot, int button) {
        switch (action) {
            case CLICK -> predictOrganSlotClick(slot, button);
            case QUICK_MOVE -> predictOrganSlotQuickMove(slot);
            case CLEAR -> predictOrganSlotClear(slot);
            default -> {
            }
        }

        ModNetwork.sendToServer(new NestLeaderOrganActionPacket(action, slot, button));
    }

    /**
     *  {@code NestLeaderOrganActionHandler#click}
     *
     * <p>   /  {@code max(1, count/2)}
     *    /  1 </p>
     *
     * <p><b> +  + </b>
     * SPEC D2 {@code NestLeaderOrganActionHandler#click}
     * {@code tryUnlockSlot}
     * <b></b>{@code expectedSlot = inSlot = }
     * {@code carriedAfter = carried = }
     *
     *
     * <b></b>
     *
     * {@link #sendSlotAction} </p>
     */
    private void predictOrganSlotClick(int slot, int button) {
        ItemStack carried = NestLeaderOrganClientData.carried();
        ItemStack inSlot = organSlotStack(slot);
        boolean right = button == NestLeaderOrganActionPacket.BUTTON_RIGHT;

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
        } else if (!NestLeaderOrganData.acceptsItem(carried)) {
            //  epca:organ_part
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
        } else if (NestLeaderOrganData.acceptsItem(inSlot)) {
            expectedSlot = carried.copy();
            carriedAfter = inSlot.copy();
        }
        NestLeaderOrganClientData.predictOrganSlotAction(slot, expectedSlot, carried, carriedAfter);
    }

    /**
     * Shift  {@code NestLeaderOrganActionHandler#quickMove}
     *
     * <p>
     *    /  / </p>
     */
    private void predictOrganSlotQuickMove(int slot) {
        ItemStack carried = NestLeaderOrganClientData.carried();
        ItemStack inSlot = organSlotStack(slot);

        ItemStack carriedAfter = carried;
        ItemStack expectedSlot = inSlot;

        if (carried.isEmpty()) {
            if (!inSlot.isEmpty()) expectedSlot = ItemStack.EMPTY;
        } else if (!NestLeaderOrganData.acceptsItem(carried)) {
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
        NestLeaderOrganClientData.predictOrganSlotAction(slot, expectedSlot, carried, carriedAfter);
    }

    /**  {@code NestLeaderOrganActionHandler#clear} */
    private void predictOrganSlotClear(int slot) {
        ItemStack carried = NestLeaderOrganClientData.carried();
        ItemStack inSlot = organSlotStack(slot);
        ItemStack expectedSlot = inSlot.isEmpty() ? inSlot : ItemStack.EMPTY;
        NestLeaderOrganClientData.predictOrganSlotAction(slot, expectedSlot, carried, carried);
    }

    /**  /  */
    private static ItemStack organSlotStack(int slot) {
        NestLeaderOrganData data = NestLeaderOrganClientData.data();
        if (slot < 0 || slot >= data.size()) return ItemStack.EMPTY;
        return data.getItem(slot);
    }

    /**  {@code amount} {@code shrinkCarried}  */
    private static ItemStack takeFrom(ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return stack;
        int left = stack.getCount() - amount;
        return left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left);
    }

    /**
     *
     *
     * <p><b></b>
     *  tick  container
     *  {@code NestLeaderOrganActionHandler#clickInventory} /
     * {@code #quickMoveFromInventory}<b></b>
     * {@code (count + 1) / 2} {@code max(1, count / 2)}
     *  {@code BUTTON_MIDDLE}  shift
     * </p>
     */
    protected void sendInventoryAction(int inventorySlot, int button) {
        if (button == NestLeaderOrganActionPacket.BUTTON_MIDDLE
                || (hasShiftDown() && button == NestLeaderOrganActionPacket.BUTTON_LEFT)) {
            //  tag /  /
            //
            //  INVENTORY_SLOT + BUTTON_MIDDLE
            // NestLeaderOrganActionHandler  78
            // inventoryQuickMove()  BUTTON_INVENTORY_QUICK_MOVE = BUTTON_MIDDLE
            // quickMoveFromInventory
            NestLeaderOrganClientData.predictCarried(ItemStack.EMPTY);
            ModNetwork.sendToServer(NestLeaderOrganActionPacket.inventoryQuickMove(inventorySlot));
            return;
        }

        ItemStack carried = NestLeaderOrganClientData.carried();
        Inventory inventory = clientInventory();
        ItemStack inSlot = inventory == null ? ItemStack.EMPTY : inventoryItem(inventory, inventorySlot);
        boolean right = button == NestLeaderOrganActionPacket.BUTTON_RIGHT;

        ItemStack carriedAfter = carried;
        if (carried.isEmpty()) {
            if (!inSlot.isEmpty()) {
                //  doClick
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

        NestLeaderOrganClientData.predictCarried(carriedAfter);
        ModNetwork.sendToServer(NestLeaderOrganActionPacket.inventorySlot(inventorySlot, button));
    }

    /**  */
    protected void sendReturnCarried() {
        NestLeaderOrganClientData.clearCarried();
        ModNetwork.sendToServer(NestLeaderOrganActionPacket.returnCarried());
    }


    /**  GUI  {@code EPCANoteScreen}  */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     *
     *
     * <p> -&gt; ESC
     * {@code NestLeaderOrganEvents}</p>
     *
     * <p><b></b> {@code hasPendingLocalCarried()}
     *
     * </p>
     */
    @Override
    public void removed() {
        this.draggingModel = false;
        sendReturnCarried();
        super.removed();
    }

    /**  STAGE 3  */
    protected AbstractClientPlayer localPlayer() {
        Minecraft minecraft = this.minecraft;
        return minecraft == null ? null : minecraft.player;
    }


    /** {@code getter}  */
    protected int[] layoutSnapshot() {
        return new int[]{
                MODEL_SIZE, PANEL_WIDTH, PANEL_HEIGHT, MODEL_CENTER_X, MODEL_CENTER_Y,
                SLOT_SIZE, SLOT_STRIDE, TORSO_INNER_STRIDE, INVENTORY_STRIDE,
                TORSO_INNER_LEFT, TORSO_INNER_TOP, HEAD_INNER_LEFT, HEAD_INNER_TOP,
                INVENTORY_LEFT, INVENTORY_TOP, HOTBAR_TOP,
                // 1000
                this.width, this.height,
                Math.round(this.layoutScale * 1000.0F),
                panelScreenWidth(), panelScreenHeight(),
                this.modelSize,
                this.compactLayout ? 1 : 0,
                MODEL_SIZE - this.modelSize};
    }

    /**
     *
     *
     * <p>{@code scale= =,  = = =/
     * =/ = =}
     * {@code Screen#resize}  {@code repositionElements -> rebuildWidgets}
     * {@link #init()}_tmp_vanilla_src/.../Screen.java  425433
     * 19201080  GUI  1/2/3/4 </p>
     */
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

    /**
     * 0 =
     *
     * <p>52  36
     *  <b>0</b></p>
     */
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

    /**
     * /0 =
     *
     * <p> 0
     *  0</p>
     */
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

    /**
     * 0 =  600  430
     *
     * <p> 12px
     * {@link #modelBody()}</p>
     */
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

