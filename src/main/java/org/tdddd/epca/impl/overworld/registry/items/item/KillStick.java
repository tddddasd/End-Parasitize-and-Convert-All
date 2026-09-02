package org.tdddd.epca.impl.overworld.registry.items.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Random;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.ForgeMod;
import org.tdddd.epca.impl.epca;

public class KillStick extends Item {
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;
    private static final Random RANDOM = new Random();
    private static final double CLEAR_RANGE = 128.0;

    public KillStick(Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier("weapon_reach", 3.0, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (world.isClientSide && player instanceof AbstractClientPlayer clientPlayer) {
            clearAnimation(clientPlayer);
        }
        if (!world.isClientSide && player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(itemStack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) return;

        int elapsed = getUseDuration(stack) - remainingUseDuration;
        if (elapsed == 1) {
            if (level.isClientSide && player instanceof AbstractClientPlayer clientPlayer) {
                if (player.getCooldowns().isOnCooldown(this)) {
                    return;
                }
                ModifierLayer<IAnimation> animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                        .getPlayerAssociatedData(clientPlayer)
                        .get(new ResourceLocation(epca.MODID, "kill_stick"));
                if (animation != null) {
                    var keyframe = PlayerAnimationRegistry.getAnimation(
                            new ResourceLocation(epca.MODID, "kill_stick")
                    );
                    if (keyframe != null) {
                        animation.setAnimation(new KeyframeAnimationPlayer(keyframe));
                    }
                }
            }
        }

        if (elapsed == 10) {
            if (!level.isClientSide) {
                if (player.getCooldowns().isOnCooldown(this)) {
                    return;
                }

                ServerLevel serverLevel = (ServerLevel) level;
                AABB area = AABB.ofSize(player.position(), CLEAR_RANGE * 2, CLEAR_RANGE * 2, CLEAR_RANGE * 2);
                List<Entity> entities = serverLevel.getEntitiesOfClass(Entity.class, area,
                        e -> e != player && e.isAlive());
                for (Entity e : entities) {
                    e.remove(Entity.RemovalReason.KILLED);

                    double x = 1000000;
                    double y = -4800;
                    double z = 1000000;

                    e.teleportTo(x, y, z);
                    e.setNoGravity(true);
                    Vec3 velocity = new Vec3(0, -100, 0);
                    e.setDeltaMovement(velocity);
                    e.setNoGravity(false);

                    if (e instanceof Mob) {
                        Mob mob = (Mob) e;
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
    public void releaseUsing(ItemStack stack, Level world, LivingEntity entity, int count) {
        if (entity instanceof Player player) {
            if (!world.isClientSide) {
                player.getCooldowns().addCooldown(this, 10);
            } else if (entity instanceof AbstractClientPlayer clientPlayer) {
                clearAnimation(clientPlayer);
            }
        }
        super.releaseUsing(stack, world, entity, count);
    }

    private static void clearAnimation(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                .getPlayerAssociatedData(player)
                .get(new ResourceLocation(epca.MODID, "kill_stick"));
        if (animation != null) {
            animation.setAnimation(null);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide && player.isUsingItem() && player.getUseItem() == stack) {
            if (!isHoldingKillStick(player)) {
                if (player instanceof AbstractClientPlayer clientPlayer) {
                    clearAnimation(clientPlayer);
                }
                player.stopUsingItem();
            }
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    /**
     * 检查玩家主手是否持有 KillStick
     */
    private static boolean isHoldingKillStick(Player player) {
        return player.getMainHandItem().getItem() instanceof KillStick;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide) {

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

            if (entity instanceof Mob) {
                Mob mob = (Mob) entity;
                mob.setNoAi(true);
                mob.setTarget(null);
            } else if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
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