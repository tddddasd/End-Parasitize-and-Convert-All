package org.tdddd.epca.impl.client.entity;

import net.minecraft.client.model.geom.ModelPart;

public class ModelPartSnapshot {
    public final float x, y, z;
    public final float xRot, yRot, zRot;
    public final float xScale, yScale, zScale;

    public ModelPartSnapshot(ModelPart part) {
        this.x = part.x;
        this.y = part.y;
        this.z = part.z;
        this.xRot = part.xRot;
        this.yRot = part.yRot;
        this.zRot = part.zRot;
        this.xScale = part.xScale;
        this.yScale = part.yScale;
        this.zScale = part.zScale;
    }

    public void applyTo(ModelPart part) {
        part.x = x;
        part.y = y;
        part.z = z;
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
        part.xScale = xScale;
        part.yScale = yScale;
        part.zScale = zScale;
    }
}