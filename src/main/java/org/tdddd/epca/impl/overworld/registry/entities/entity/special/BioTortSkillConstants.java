package org.tdddd.epca.impl.overworld.registry.entities.entity.special;

public final class BioTortSkillConstants {

    private BioTortSkillConstants() {
    }

    public static final int MARK_INTERVAL_TICKS = 400;

    public static final int MARK_DURATION_TICKS = MARK_INTERVAL_TICKS;

    public static final int MARKED_SLOT_COUNT = 3;

    public static final int MARKED_SLOT_MIN = 0;
    public static final int MARKED_SLOT_MAX = 8;

    public static final double VICTIM_RANGE = 64.0D;

    public static final float SELECTED_SLOT_DAMAGE = 5.0F;

    public static final int SELECTED_SLOT_DAMAGE_INTERVAL_TICKS = 40;

    public static final int SOUL_BREAKING_CURSE_DURATION_TICKS = 20;

    public static final int SOUL_BREAKING_CURSE_AMPLIFIER = 4;

    public static final int PHASE2_ATTACK_THRESHOLD = 14;

    public static final int PHASE2_ATTACKS_PER_EFFECT = 4;

    public static final int CORRUPTION_TICKS = 80;

    public static final int CORRUPTION_RED_START_TICKS = 60;

    public static final int CORRUPTION_DECAY_TICKS = 30;

    public static final int CORRUPTION_TELEPORT_TICKS = 60;

    public static final int POSITION_LOOKBACK_TICKS = 100;

    public static final int POSITION_HISTORY_TICKS = 160;

    public static final int CORRUPTION_TOTAL_TICKS = CORRUPTION_TICKS + CORRUPTION_DECAY_TICKS;
}
