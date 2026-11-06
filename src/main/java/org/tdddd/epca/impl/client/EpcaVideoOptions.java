package org.tdddd.epca.impl.client;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class EpcaVideoOptions {

    public static final String KEY_GIB_LIMIT = "epca.option.gib_limit";

    public static final String KEY_GIB_LIMIT_MAX = "epca.option.gib_limit.max";

    public static final String KEY_GIB_MULTIPLIER = "epca.option.gib_multiplier";

    private EpcaVideoOptions() { }

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
