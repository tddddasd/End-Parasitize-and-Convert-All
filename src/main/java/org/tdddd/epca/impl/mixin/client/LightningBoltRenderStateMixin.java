package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.tdddd.epca.impl.utils.IPurpleLightningBolt;


@Mixin(LightningBoltRenderState.class)
public class LightningBoltRenderStateMixin implements IPurpleLightningBolt {

    @Unique
    private boolean epca$purpleBolt;

    @Override
    public boolean epca$isPurpleBolt() {
        return this.epca$purpleBolt;
    }

    @Override
    public void epca$setPurpleBolt(boolean purple) {
        this.epca$purpleBolt = purple;
    }
}
