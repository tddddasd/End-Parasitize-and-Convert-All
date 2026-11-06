package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.EpcaVideoOptions;

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
