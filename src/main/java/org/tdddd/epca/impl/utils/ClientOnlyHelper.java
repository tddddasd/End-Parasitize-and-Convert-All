package org.tdddd.epca.impl.utils;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.client.DifficultyScreenHandler;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyLevel;

@OnlyIn(Dist.CLIENT)
public class ClientOnlyHelper {
    public static DifficultyLevel getPendingDifficulty() {
        return DifficultyScreenHandler.consumePendingDifficulty();
    }
}