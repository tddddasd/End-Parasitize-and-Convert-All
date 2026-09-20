package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.client.DifficultyScreenHandler;


@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {

    @Inject(
            method = "createNewWorld(Lnet/minecraft/core/LayeredRegistryAccess;Lnet/minecraft/world/level/storage/LevelDataAndDimensions$WorldDataAndGenSettings;Ljava/util/Optional;)Z",
            at = @At("HEAD")
    )
    private void epca$setPendingDifficulty(CallbackInfoReturnable<Boolean> cir) {
        DifficultyScreenHandler.setPendingDifficulty(DifficultyScreenHandler.getSelectedDifficulty());
    }
}
