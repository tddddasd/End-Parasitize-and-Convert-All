package org.tdddd.epca.impl.events.playeranimator;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
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
    public @NotNull FirstPersonMode getFirstPersonMode(float tickDelta) {
        return mode;
    }

    @Override
    public @NotNull FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
        return configuration;
    }
}