package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the {@code OptionsList} of the 1.20.1 Video Settings screen (it is a private field declared on
 * the screen itself, so an accessor is used instead of a shadow).
 */
@Mixin(VideoSettingsScreen.class)
public interface VideoSettingsScreenAccessor {
    @Accessor("list")
    OptionsList epca$getOptionsList();
}
