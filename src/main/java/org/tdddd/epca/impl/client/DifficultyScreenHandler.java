package org.tdddd.epca.impl.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.ModConfig;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyLevel;
import org.tdddd.epca.impl.epca;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class DifficultyScreenHandler {

    private static final String BUTTON_KEY = "epca.difficulty.button";
    private static final String DIFFICULTY_KEY_PREFIX = "epca.difficulty.";
    private static DifficultyLevel selectedDifficulty;
    private static DifficultyLevel pendingDifficulty = null;

    public static DifficultyLevel getSelectedDifficulty() { return selectedDifficulty; }

    public static void setPendingDifficulty(DifficultyLevel difficulty) {
        pendingDifficulty = difficulty;
    }

    public static DifficultyLevel consumePendingDifficulty() {
        DifficultyLevel diff = pendingDifficulty;
        pendingDifficulty = null;
        return diff;
    }

    private static Component getDifficultyDisplayName(DifficultyLevel level) {
        return Component.translatable(DIFFICULTY_KEY_PREFIX + level.getName());
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof CreateWorldScreen) {

            selectedDifficulty = ModConfig.getDefaultExtraDifficulty();
            Button difficultyButton = Button.builder(
                            Component.translatable(BUTTON_KEY, getDifficultyDisplayName(selectedDifficulty)),
                            btn -> {
                                selectedDifficulty = selectedDifficulty.next();
                                btn.setMessage(Component.translatable(BUTTON_KEY, getDifficultyDisplayName(selectedDifficulty)));
                            })
                    .pos(10, 30)
                    .size(100, 20)
                    .build();

            event.addListener(difficultyButton);
        }
    }
}