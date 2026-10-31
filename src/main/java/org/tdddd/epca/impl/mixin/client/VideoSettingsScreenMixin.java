package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.EpcaVideoOptions;

/**
 * Appends the EPCA gib sliders to the vanilla Video Settings list.
 *
 * <p>1.20.1 builds the whole list inside {@code VideoSettingsScreen#init()} (there is no separate
 * {@code addOptions()} hook yet), so the two rows are appended at its TAIL - after every vanilla option
 * and after the list itself has been added as a renderable widget.</p>
 */
@Mixin(VideoSettingsScreen.class)
public class VideoSettingsScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void epca$addGibOptions(CallbackInfo ci) {
        OptionsList list = ((VideoSettingsScreenAccessor) this).epca$getOptionsList();
        if (list == null) return;
        list.addBig(EpcaVideoOptions.gibLimitOption());
        list.addBig(EpcaVideoOptions.gibMultiplierOption());
    }
}
