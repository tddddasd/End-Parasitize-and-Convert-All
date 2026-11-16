package org.tdddd.epca.impl.client.entity;

public final class EpcaAnimations {

    public static final int GEO_TRANSITION_TICKS = 2;

    /**
     * 攀爬相关动画的交叉淡入淡出时长（tick）。
     * <p>
     * 攀爬与走路/待机之间的姿态差异较大，用 6 tick（约 0.3s）做平滑过渡，避免硬切；
     * 该值落在 5~8 tick 的合理区间内。
     */
    public static final int CLIMB_TRANSITION_TICKS = 6;

    private EpcaAnimations() {
    }
}
