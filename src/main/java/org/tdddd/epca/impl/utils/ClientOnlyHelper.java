package org.tdddd.epca.impl.utils;

import net.neoforged.api.distmarker.Dist;
import org.tdddd.epca.impl.client.DifficultyScreenHandler;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyLevel;

public class ClientOnlyHelper {
    public static DifficultyLevel getPendingDifficulty() {
        return DifficultyScreenHandler.consumePendingDifficulty();
    }
}