package org.tdddd.epca.impl.client;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The two EPCA sliders that are appended to the vanilla Video Settings screen.
 *
 * <p>Both are plain integer sliders built on {@link OptionInstance.IntRange}, so they look and behave
 * like the vanilla ones (drag, arrow keys, narrator). Values are read from and written back to
 * {@link EpcaClientSettings}.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class EpcaVideoOptions {
    /** "Mob gib pieces": 0..64 plus a "Max" step. */
    public static final String KEY_GIB_LIMIT = "epca.option.gib_limit";
    /** Label used for the top ("no cap") step of the limit slider. */
    public static final String KEY_GIB_LIMIT_MAX = "epca.option.gib_limit.max";
    /** "Gib multiplier (debug)": 1..500. */
    public static final String KEY_GIB_MULTIPLIER = "epca.option.gib_multiplier";

    private EpcaVideoOptions() { }

    /** Slider 1: how many pieces a single death may produce (top step = unlimited). */
    public static OptionInstance<Integer> gibLimitOption() {
        return new OptionInstance<>(
                KEY_GIB_LIMIT,
                OptionInstance.noTooltip(),
                (caption, value) -> Options.genericValueLabel(caption,
                        value >= EpcaClientSettings.GIB_LIMIT_UNLIMITED
                                ? Component.translatable(KEY_GIB_LIMIT_MAX)
                                : Component.literal(Integer.toString(value))),
                new OptionInstance.IntRange(EpcaClientSettings.GIB_LIMIT_MIN, EpcaClientSettings.GIB_LIMIT_UNLIMITED),
                EpcaClientSettings.getGibLimit(),
                EpcaClientSettings::setGibLimit);
    }

    /** Slider 2: debug multiplier applied on top of the piece count. */
    public static OptionInstance<Integer> gibMultiplierOption() {
        return new OptionInstance<>(
                KEY_GIB_MULTIPLIER,
                OptionInstance.noTooltip(),
                (caption, value) -> Options.genericValueLabel(caption, Component.literal(value + "x")),
                new OptionInstance.IntRange(EpcaClientSettings.GIB_MULTIPLIER_MIN, EpcaClientSettings.GIB_MULTIPLIER_MAX),
                EpcaClientSettings.getGibMultiplier(),
                EpcaClientSettings::setGibMultiplier);
    }
}
