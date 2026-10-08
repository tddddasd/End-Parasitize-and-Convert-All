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

/**
 * Spawn egg that spawns a specific VARIANT of a mob whose variants are a synced ordinal on a single
 * entity type.
 *
 * <p>The spider family registers two entity types ({@code epca:infested_spider} and
 * {@code epca:walking_spider_head}) and carries the plain / bleeding / cave forms as a
 * {@code Variant} field, so a plain {@code SpawnEggItem} can only ever produce the default variant.
 * This item keeps the vanilla right-click flow but applies the variant to the entity it creates,
 * which gives one egg per variant - the four spawn-egg textures the user supplied.</p>
 *
 * <p>It deliberately follows the shape of the mod's existing custom egg
 * ({@code InfestedSlimeSpawnEgg}): override {@link #useOn}, do the work server-side only, shrink the
 * stack unless the player is in creative, and answer {@code PASS} when nothing could be placed.</p>
 *
 * <p>26.1.2 notes: {@code SpawnEggItem}'s constructor takes only {@code Properties} and the entity
 * type comes from {@code properties.spawnEgg(...)}; {@code EntityType#create} needs an
 * {@link EntitySpawnReason}; and {@code finalizeSpawn} takes four arguments (no {@code CompoundTag},
 * unlike 1.20.1).</p>
 *
 * <p><b>The entity type is held as a LAZY {@link Supplier} and is only resolved inside
 * {@link #useOn}.</b> It must never be captured as an {@code EntityType} field or resolved with
 * {@code .get()} at class-initialisation time: {@code ModItems} is populated during the ITEM
 * registration phase, while entity types are registered in a later phase. An eager
 * {@code ModEntities.X.get()} in an item initialiser throws
 * {@code Registry Object not present: epca:...} and the whole mod fails to load. Callers pass the
 * {@code DeferredHolder} itself, which is a {@link Supplier}.</p>
 */
public class VariantSpawnEgg extends SpawnEggItem {

    /** 0 = DEFAULT, 1 = BLOOD, 2 = CAVE; must match the enum order of both spider classes. */
    private final int variant;
    /**
     * Optional weights indexed by variant ordinal. null pins variant; non-null means "roll one
     * ordinal from this table" (e.g. {55, 30, 0} for the plain spider egg).
     */
    private final int[] weights;
    private final Supplier<? extends EntityType<? extends Mob>> type;

    /** Pinned variant: always spawns variant. */
    public VariantSpawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int variant, Properties properties) {
        this(type, variant, null, properties);
    }

    /** Weighted variant: rolls one ordinal from weights on the server at use time. */
    public VariantSpawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int[] weights, Properties properties) {
        this(type, 0, weights, properties);
    }

    private VariantSpawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int variant, int[] weights,
                            Properties properties) {
        // properties.spawnEgg(...) runs at item creation time, so the registry lookup stays late.
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

        // Resolved HERE, at use time - never at class-init time.
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Entity created = type.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (created == null) {
            return InteractionResult.PASS;
        }

        // Rolled HERE too: this branch only runs on the server, so the client never disagrees.
        applyVariant(created, rollVariant(serverLevel.getRandom()));
        // 26 renamed Entity#moveTo to Entity#snapTo (every call site in this tree uses snapTo).
        float yRot = context.getPlayer() == null ? 0.0F : context.getPlayer().getYRot();
        created.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yRot, 0.0F);
        if (created instanceof Mob mob) {
            // 26.1.2 finalizeSpawn: (ServerLevelAccessor, DifficultyInstance, EntitySpawnReason,
            // SpawnGroupData). The trailing argument is "no group data" here, so null.
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

    /**
     * Writes this egg's variant onto the freshly created entity. The runtime type check means a
     * mismatch degrades to leaving the entity at its default variant instead of throwing.
     */
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
