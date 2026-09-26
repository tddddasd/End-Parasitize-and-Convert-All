package org.tdddd.epca.impl.events.playeranimator;

import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import org.jetbrains.annotations.NotNull;

public class FirstPersonModifier extends AbstractModifier {
    private final FirstPersonMode mode;
    private final FirstPersonConfiguration configuration;

    public FirstPersonModifier(FirstPersonMode mode) {
        this(mode, new FirstPersonConfiguration());
    }

    public FirstPersonModifier(FirstPersonMode mode, FirstPersonConfiguration configuration) {
        this.mode = mode;
        this.configuration = configuration;
    }

    @Override
    public @NotNull FirstPersonMode getFirstPersonMode() {
        return mode;
    }

    @Override
    public @NotNull FirstPersonConfiguration getFirstPersonConfiguration() {
        return configuration;
    }
}