package org.tdddd.epca.impl.utils;

import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.events.ParasiteEvents;

public class ParasiteHelper {
    public static boolean isParasite(LivingEntity entity) {

        return ParasiteEvents.isParasiteByTagOrInterface(entity);
    }
}
