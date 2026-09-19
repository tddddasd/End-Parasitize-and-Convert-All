package org.tdddd.epca.impl.events;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

@EventBusSubscriber(modid = epca.MODID)
public class InfestedBlockHandler {
    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity == null || !entity.isAlive()) return;

        if (IParasite.isParasiteByTagOrInterface(entity)) return;

        BlockPos pos = entity.getOnPos();
        BlockState state = entity.level().getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof InfestedBlockInterface)) return;

        if (entity.hasEffect(ModEffects.COTH)) return;
        entity.addEffect(new MobEffectInstance(ModEffects.COTH, 1200, 0));
    }
}