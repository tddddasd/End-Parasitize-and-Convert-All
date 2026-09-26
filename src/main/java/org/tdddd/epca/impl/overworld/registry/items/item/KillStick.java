package org.tdddd.epca.impl.overworld.registry.items.item;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Random;
import net.minecraft.sounds.SoundEvents;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ArayaConstants;
import org.tdddd.epca.impl.events.ArayaSyncHandler;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

/**
 * Kill stick.
 *
 * <h2>26.1.2 item-API changes applied here</h2>
 * <ul>
 *   <li>{@code Item#getDefaultAttributeModifiers(EquipmentSlot)} is gone: item attributes are a
 *       data component now, so the reach bonus is built with {@link ItemAttributeModifiers} and
 *       installed through {@code Item.Properties#attributes(...)} in the constructor.</li>
 *   <li>{@code NeoForgeMod.ENTITY_REACH} does not exist in 26.1.2 — the reach attribute is vanilla
 *       {@code Attributes.ENTITY_INTERACTION_RANGE} (verified: {@code NeoForgeMod} only declares
 *       SWIM_SPEED / NAMETAG_DISTANCE / CREATIVE_FLIGHT).</li>
 *   <li>{@code AttributeModifier} is a record {@code (Identifier, double, Operation)}.</li>
 *   <li>{@code Item#use} returns {@link InteractionResult} ({@code InteractionResultHolder} is
 *       deleted); {@code getUseDuration} takes the stack <i>and</i> the entity;
 *       {@code releaseUsing} returns {@code boolean}; {@code inventoryTick} takes a
 *       {@link ServerLevel} and an {@link EquipmentSlot}.</li>
 * </ul>
 */
public class KillStick extends Item {
    private static final Random RANDOM = new Random();
    private static final double CLEAR_RANGE = 128.0;
    private static final int USE_DURATION = 72000;

    public KillStick(Properties properties) {
        super(properties.attributes(defaultAttributeModifiers()));
    }

    private static ItemAttributeModifiers defaultAttributeModifiers() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(Identifier.fromNamespaceAndPath(epca.MODID, "weapon_reach"),
                                3.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (world.isClientSide() && player instanceof AbstractClientPlayer clientPlayer) {
            clearAnimation(clientPlayer);
        }
        if (!world.isClientSide() && player.getCooldowns().isOnCooldown(itemStack)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) return;

        int elapsed = getUseDuration(stack, livingEntity) - remainingUseDuration;
        if (elapsed == 1) {
            if (level.isClientSide() && player instanceof AbstractClientPlayer clientPlayer) {
                if (player.getCooldowns().isOnCooldown(stack)) {
                    return;
                }
                ModifierLayer<IAnimation> animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                        .getPlayerAssociatedData(clientPlayer)
                        .get(Identifier.fromNamespaceAndPath(epca.MODID, "kill_stick"));
                if (animation != null) {
                    var keyframe = PlayerAnimationRegistry.getAnimation(
                            Identifier.fromNamespaceAndPath(epca.MODID, "kill_stick")
                    );
                    if (keyframe != null) {
                        animation.setAnimation(new KeyframeAnimationPlayer(keyframe));
                    }
                }
            }
        }

        if (elapsed == 10) {
            if (!level.isClientSide()) {
                if (player.getCooldowns().isOnCooldown(stack)) {
                    return;
                }

                ServerLevel serverLevel = (ServerLevel) level;
                AABB area = AABB.ofSize(player.position(), CLEAR_RANGE * 2, CLEAR_RANGE * 2, CLEAR_RANGE * 2);
                List<Entity> entities = serverLevel.getEntitiesOfClass(Entity.class, area,
                        e -> e != player && e.isAlive());
                for (Entity e : entities) {
                    if (e instanceof Player) {
                        return;
                    }

                    e.remove(Entity.RemovalReason.KILLED);
                    double x = 1000000;
                    double y = -4800;
                    double z = 1000000;

                    e.teleportTo(x, y, z);
                    e.setNoGravity(true);
                    Vec3 velocity = new Vec3(0, -100, 0);
                    e.setDeltaMovement(velocity);
                    e.setNoGravity(false);

                    if (e instanceof Mob mob) {
                        mob.setNoAi(true);
                        mob.setTarget(null);
                    } else if (e instanceof LivingEntity) {
                        livingEntity.setJumping(false);
                        livingEntity.setDeltaMovement(Vec3.ZERO);
                    }
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level world, LivingEntity entity, int count) {
        if (entity instanceof Player player) {
            if (!world.isClientSide()) {
                player.getCooldowns().addCooldown(stack, 10);
            } else if (entity instanceof AbstractClientPlayer clientPlayer) {
                clearAnimation(clientPlayer);
            }
        }
        return super.releaseUsing(stack, world, entity, count);
    }

    private static void clearAnimation(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                .getPlayerAssociatedData(player)
                .get(Identifier.fromNamespaceAndPath(epca.MODID, "kill_stick"));
        if (animation != null) {
            animation.setAnimation(null);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide() && player.isUsingItem() && player.getUseItem() == stack) {
            if (!isHoldingKillStick(player)) {
                if (player instanceof AbstractClientPlayer clientPlayer) {
                    clearAnimation(clientPlayer);
                }
                player.stopUsingItem();
            }
        }
        super.inventoryTick(stack, level, entity, slot);
    }

    
    private static boolean isHoldingKillStick(Player player) {
        return player.getMainHandItem().getItem() instanceof KillStick;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide()) {

            boolean isNamed = isAlayavijnana(stack);
            if (isNamed && player.level() instanceof ServerLevel serverLevel) {

                AABB box = entity.getBoundingBox();
                int count = 15 + RANDOM.nextInt(6);
                for (int i = 0; i < count; i++) {
                    double x = box.minX + RANDOM.nextDouble() * (box.maxX - box.minX);
                    double y = box.minY + RANDOM.nextDouble() * (box.maxY - box.minY);
                    double z = box.minZ + RANDOM.nextDouble() * (box.maxZ - box.minZ);

                    var particle = RANDOM.nextBoolean() ? ParticleTypes.CLOUD : ParticleTypes.END_ROD;
                    serverLevel.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0.1);
                }

                // The renamed staff's own hit: 444 points of the mod's MINIMUM damage type on the target
                // of this click, so the kill (and therefore the trident sound and the 天杀 counter in
                // ArayaSyncHandler) has one unambiguous source. The source carries the player as its
                // causing entity, which is what lets LivingDeathEvent name the killer.
                if (entity instanceof LivingEntity livingTarget) {
                    // 26.1.2: registryAccess().lookupOrThrow(...) instead of registryOrThrow(...).
                    Holder<DamageType> holder = serverLevel.registryAccess()
                            .lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(ModDamageTypes.MINIMUM);
                    livingTarget.hurt(new DamageSource(holder, player, player),
                            ArayaConstants.DAMAGE);

                    // Every left click of the renamed staff on a living entity cuts, mob or player and
                    // whatever the outcome of the hit. Sent before the victim is removed and moved to
                    // the void below, so the blade keeps the position the hit happened at.
                    ArayaSyncHandler.broadcastSlash(serverLevel, player, livingTarget);
                }
            }
            if (entity instanceof Player) {
                return false;
            }
            entity.remove(Entity.RemovalReason.KILLED);

            double x = 1000000;
            double y = -4800;
            double z = 1000000;

            entity.teleportTo(x, y, z);
            entity.setNoGravity(true);
            Vec3 velocity = new Vec3(0, -100, 0);
            entity.setDeltaMovement(velocity);
            entity.setNoGravity(false);

            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setTarget(null);
            } else if (entity instanceof LivingEntity livingEntity) {
                livingEntity.setJumping(false);
                livingEntity.setDeltaMovement(Vec3.ZERO);
            }
            if (isNamed) {

                double maxHealth = player.getMaxHealth();

                double reduction = maxHealth * 0.01;
                double newMax = maxHealth - reduction;

                if (newMax < 4.0) {
                    newMax = 4.0;
                }

                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMax);
                if (player.getHealth() > newMax) {
                    player.setHealth((float) newMax);
                }
            }

            return true;
        }

        return false;
    }

    public static boolean isAlayavijnana(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof KillStick)) {
            return false;
        }
        Component name = stack.getHoverName();
        if (name == null) return false;
        String str = name.getString();
        return "Alayavijnana".equals(str) || "阿赖耶识".equals(str);
    }

    public static boolean hasAlayavijnanaItem(Player player) {
        if (player == null) return false;
        ItemStack main = player.getMainHandItem();
        if (isAlayavijnana(main)) return true;
        ItemStack off = player.getOffhandItem();
        return isAlayavijnana(off);
    }

    public static LivingEntity findLowestHealthEntity(Player player, double range) {
        if (player == null) return null;
        Level level = player.level();
        AABB area = AABB.ofSize(player.position(), range * 2, range * 2, range * 2);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());
        if (entities.isEmpty()) return null;
        LivingEntity lowest = null;
        float minHealth = Float.MAX_VALUE;
        for (LivingEntity e : entities) {
            float h = e.getHealth();
            if (h < minHealth) {
                minHealth = h;
                lowest = e;
            }
        }
        return lowest;
    }
}
