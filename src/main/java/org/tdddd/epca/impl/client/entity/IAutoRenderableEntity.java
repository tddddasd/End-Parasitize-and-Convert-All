package org.tdddd.epca.impl.client.entity;

import org.tdddd.epca.impl.api.IGeoResources;
import software.bernie.geckolib.animatable.GeoEntity;

/**
 * Marks an entity as auto-renderable via the generic EpcaGeoModel/EpcaGeoRenderer.
 * Entities implement this to opt into automatic model/texture/animation resource resolution
 * and automatic renderer registration.
 */
public interface IAutoRenderableEntity extends IGeoResources, GeoEntity {
}
