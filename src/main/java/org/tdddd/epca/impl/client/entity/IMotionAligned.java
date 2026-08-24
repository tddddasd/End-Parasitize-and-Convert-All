package org.tdddd.epca.impl.client.entity;

/**
 * Marker interface for projectile-like entities whose rendering should align
 * with their velocity vector (motion direction) instead of the entity's yaw/pitch.
 * Handled automatically by {@link EpcaGeoRenderer#applyRotations}.
 */
public interface IMotionAligned {
}
