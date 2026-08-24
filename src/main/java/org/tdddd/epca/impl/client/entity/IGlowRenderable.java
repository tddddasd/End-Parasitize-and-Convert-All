package org.tdddd.epca.impl.client.entity;

import net.minecraft.resources.ResourceLocation;

/**
 * 实现此接口的实体将拥有独立的发光叠加层纹理
 */
public interface IGlowRenderable {
    /**
     * 发光纹理（建议使用透明背景，仅绘制发光部分）
     */
    ResourceLocation getGlowTexture();

    /**
     * 发光颜色和强度（RGB 分量，通常 0~1）
     */
    default float[] getGlowColor() {
        return new float[]{1.0F, 1.0F, 1.0F};
    }

    /**
     * 是否启用发光（可用于动态开关）
     */
    default boolean isGlowEnabled() {
        return true;
    }
}