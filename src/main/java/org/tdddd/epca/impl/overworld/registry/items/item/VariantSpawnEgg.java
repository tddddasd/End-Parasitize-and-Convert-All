package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSpider;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingSpiderHead;

import java.util.function.Supplier;

public class VariantSpawnEgg extends SpawnEggItem {

    private final int variant;

    private final int[] weights;
    private final Supplier<? extends EntityType<? extends Mob>> type;

    public VariantSpawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int variant, Properties properties) {
        this(type, variant, null, properties);
    }

    public VariantSpawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int[] weights, Properties properties) {
        this(type, 0, weights, properties);
    }

    private VariantSpawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int variant, int[] weights,
                            Properties properties) {

        super(properties.spawnEgg(type.get()));
        this.type = type;
        this.variant = variant;
        this.weights = weights;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Entity created = type.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (created == null) {
            return InteractionResult.PASS;
        }

        applyVariant(created, rollVariant(serverLevel.getRandom()));

        float yRot = context.getPlayer() == null ? 0.0F : context.getPlayer().getYRot();
        created.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yRot, 0.0F);
        if (created instanceof Mob mob) {

            mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos),
                    EntitySpawnReason.SPAWN_ITEM_USE, null);
        }
        serverLevel.addFreshEntity(created);

        Player player = context.getPlayer();
        if (player == null || !player.isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private int rollVariant(net.minecraft.util.RandomSource random) {
        if (weights == null || weights.length == 0) {
            return variant;
        }
        int total = 0;
        for (int weight : weights) {
            if (weight > 0) {
                total += weight;
            }
        }
        if (total <= 0) {
            return 0;
        }
        int roll = random.nextInt(total);
        for (int ordinal = 0; ordinal < weights.length; ordinal++) {
            int weight = weights[ordinal];
            if (weight <= 0) {
                continue;
            }
            if (roll < weight) {
                return ordinal;
            }
            roll -= weight;
        }
        return 0;
    }

    private void applyVariant(Entity entity, int ordinal) {
        if (entity instanceof InfestedSpider spider) {
            spider.setVariant(InfestedSpider.Variant.values()[clampVariant(ordinal, InfestedSpider.Variant.values().length)]);
        } else if (entity instanceof WalkingSpiderHead head) {
            head.setVariant(WalkingSpiderHead.Variant.values()[clampVariant(ordinal, WalkingSpiderHead.Variant.values().length)]);
        }
    }

    private int clampVariant(int ordinal, int length) {
        if (ordinal < 0 || ordinal >= length) {
            return 0;
        }
        return ordinal;
    }
}
