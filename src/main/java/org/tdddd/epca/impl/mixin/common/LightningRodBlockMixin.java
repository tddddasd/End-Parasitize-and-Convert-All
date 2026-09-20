package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedRedstone;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


@Mixin(LightningRodBlock.class)
public class LightningRodBlockMixin {
    @Inject(method = "onLightningStrike", at = @At("HEAD"))
    private void onLightningStrike(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (level.isClientSide()) return;

        BlockPos below = pos.below();
        if (!(level.getBlockState(below).getBlock() instanceof InfestedBlockInterface)) return;

        int bonusLevel = 0;

        AABB box = new AABB(pos).inflate(2.5);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box,
                e -> e.getItem().getItem() instanceof InfestedRedstone);
        int total = items.stream().mapToInt(e -> e.getItem().getCount()).sum();

        if (total >= 64) {
            
            int toConsume = 64;
            for (ItemEntity itemEntity : items) {
                if (toConsume <= 0) break;
                ItemStack stack = itemEntity.getItem();
                int count = stack.getCount();
                if (count <= toConsume) {
                    toConsume -= count;
                    itemEntity.discard();
                } else {
                    stack.setCount(count - toConsume);
                    toConsume = 0;
                }
            }
            bonusLevel = 2;

            if (level instanceof ServerLevel serverLevel) {
                BlockPos finalPos = pos;
                java.util.concurrent.ScheduledExecutorService executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
                executor.schedule(() -> {
                    serverLevel.getServer().execute(() -> {
                        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                        lightning.setPos(finalPos.getX() + 0.5, finalPos.getY(), finalPos.getZ() + 0.5);
                        level.addFreshEntity(lightning);
                    });
                }, 1500, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        }

        int range = 64;
        AABB effectBox = new AABB(pos).inflate(range);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, effectBox,
                e -> e instanceof IParasite);

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int baseLevel = rand.nextInt(1, 4);
        int finalLevel = Math.min(baseLevel + bonusLevel, 3);

        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(ModEffects.RAGE, 600, finalLevel - 1, false, true));
            
            entity.addEffect(new MobEffectInstance(MobEffects.HASTE, 600, finalLevel - 1, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600, finalLevel - 1, false, true));
        }
    }
}
