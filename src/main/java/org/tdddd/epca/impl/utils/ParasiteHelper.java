package org.tdddd.epca.impl.utils;

import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.world.entity.LivingEntity;

public class ParasiteHelper {
    public static boolean isParasite(LivingEntity entity) {

        return ParasiteEvents.isParasiteByTagOrInterface(entity);
    }
}
