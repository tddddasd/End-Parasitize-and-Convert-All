package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InfestedBlockHandler {
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || !entity.isAlive()) return;

        if (IParasite.isParasiteByTagOrInterface(entity)) return;

        BlockPos pos = entity.getOnPos();
        BlockState state = entity.level().getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof InfestedBlockInterface)) return;

        if (entity.hasEffect(ModEffects.COTH.get())) return;
        entity.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 1200, 0));
    }
}