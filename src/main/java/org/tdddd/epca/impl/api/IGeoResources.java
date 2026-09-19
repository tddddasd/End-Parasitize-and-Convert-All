package org.tdddd.epca.impl.api;

import net.minecraft.resources.Identifier;

/**
 * Contract for entities that provide their own geo resources (model, texture, animation).
 * Entities implementing this interface can use the auto-registration system.
 */
public interface IGeoResources {
    Identifier model();
    Identifier texture();
    Identifier animation();
}
