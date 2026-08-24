package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.DifficultyScreenHandler;

@OnlyIn(Dist.CLIENT)
@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {
    @Inject(method = "createNewWorld", at = @At("HEAD"))
    private void onCreateWorld(CallbackInfo ci) {
        DifficultyScreenHandler.setPendingDifficulty(DifficultyScreenHandler.getSelectedDifficulty());
    }
}