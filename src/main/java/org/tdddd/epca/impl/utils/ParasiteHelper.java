package org.tdddd.epca.impl.utils;

import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

public class ParasiteHelper {
    public static boolean isParasite(LivingEntity entity) {
        
        return IParasite.isParasiteByTagOrInterface(entity);
    }
}