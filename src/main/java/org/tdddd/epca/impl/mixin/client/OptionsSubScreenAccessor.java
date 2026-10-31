package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the {@code OptionsList} of an options sub screen. 26.1.2 declares it as a nullable
 * {@code protected} field on {@link OptionsSubScreen}, so an accessor is safer than shadowing the
 * inherited field from {@link VideoSettingsScreenMixin}.
 */
@Mixin(OptionsSubScreen.class)
public interface OptionsSubScreenAccessor {
    @Accessor("list")
    OptionsList epca$getOptionsList();
}
