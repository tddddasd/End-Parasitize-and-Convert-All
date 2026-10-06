package org.tdddd.epca.impl.overworld.registry.entities.entity.special;

/**
 * {@link BioTortIncarnation}
 *
 * <p> {@code ArayaConstants}
 * ""</p>
 *
 * <h2> 1</h2>
 * <ul>
 *   <li> {@link #MARK_INTERVAL_TICKS}20
 *       {@link #MARKED_SLOT_COUNT} {@link #MARKED_SLOT_MIN}..
 *       {@link #MARKED_SLOT_MAX}</li>
 *   <li> {@link #MARK_DURATION_TICKS}""
 *
 *       </li>
 *   <li>""
 *       {@link #SELECTED_SLOT_DAMAGE_INTERVAL_TICKS}2
 *       {@link #SELECTED_SLOT_DAMAGE}  YawningNekoAPI
 *       {@code yawning_neko_api:minimum}  {@link BioTortIncarnation}</li>
 *   <li>""
 *        {@link #NEEDLER_AMPLIFIER}+1 V  {@code epca:needler}
 *        {@link #NEEDLER_DURATION_TICKS}1 </li>
 * </ul>
 *
 * <h2> 2 2 + 4 </h2>
 * <ul>
 *   <li> {@link #PHASE2_ATTACK_THRESHOLD}  2
 *        + NBT</li>
 *   <li> 2  {@link #PHASE2_ATTACKS_PER_EFFECT}
 *       </li>
 *   <li>{@link #CORRUPTION_TICKS}4
 *       {@link #CORRUPTION_RED_START_TICKS}3 4
 *       {@link #CORRUPTION_DECAY_TICKS}1.5 </li>
 *   <li> {@link #CORRUPTION_TELEPORT_TICKS}3
 *       " {@link #POSITION_LOOKBACK_TICKS}5 "
 *        {@link #POSITION_HISTORY_TICKS}8 </li>
 * </ul>
 */
public final class BioTortSkillConstants {

    private BioTortSkillConstants() {
    }

    // ------------------------------------------------------------------  1

    /**  20  */
    public static final int MARK_INTERVAL_TICKS = 400;

    /** "" */
    public static final int MARK_DURATION_TICKS = MARK_INTERVAL_TICKS;

    /**  */
    public static final int MARKED_SLOT_COUNT = 3;

    /** 0..8 */
    public static final int MARKED_SLOT_MIN = 0;
    public static final int MARKED_SLOT_MAX = 8;

    /**  16 */
    public static final double VICTIM_RANGE = 64.0D;

    /**  2  */
    public static final float SELECTED_SLOT_DAMAGE = 5.0F;

    /** 2  */
    public static final int SELECTED_SLOT_DAMAGE_INTERVAL_TICKS = 40;

    /**
     * {@code epca:needler}1
     *
     * <p> {@code NeedlerEffect} " tick "
     *  20 tick ""</p>
     */
    public static final int NEEDLER_DURATION_TICKS = 20;

    /**  V = amplifier 4 = amplifier + 1 */
    public static final int NEEDLER_AMPLIFIER = 4;

    // ------------------------------------------------------------------  2

    /**  2 */
    public static final int PHASE2_ATTACK_THRESHOLD = 14;

    /**  2  */
    public static final int PHASE2_ATTACKS_PER_EFFECT = 4;

    /** ""4 0..3 3..4  */
    public static final int CORRUPTION_TICKS = 80;

    /**  tick 3  */
    public static final int CORRUPTION_RED_START_TICKS = 60;

    /** 4 1.5  */
    public static final int CORRUPTION_DECAY_TICKS = 30;

    /**  3  */
    public static final int CORRUPTION_TELEPORT_TICKS = 60;

    /** " 5 " */
    public static final int POSITION_LOOKBACK_TICKS = 100;

    /** 8 20 tick/  8 5  */
    public static final int POSITION_HISTORY_TICKS = 160;

    /** tick */
    public static final int CORRUPTION_TOTAL_TICKS = CORRUPTION_TICKS + CORRUPTION_DECAY_TICKS;
}

