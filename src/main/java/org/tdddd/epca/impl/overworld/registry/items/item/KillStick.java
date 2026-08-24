package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class KillStick extends Item {
    private static final Random RANDOM = new Random();

    
    private static final Map<ServerLevel, List<EffectData>> EFFECTS = new HashMap<>();

    
    static {
        MinecraftForge.EVENT_BUS.addListener(KillStick::onServerTick);
    }

    public KillStick(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (world.isClientSide) {
            return InteractionResultHolder.sidedSuccess(itemStack, true);
        }

        
        HitResult hit = player.pick(5.0D, 1.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();
            if (world.getBlockState(pos).getBlock() == Blocks.CRYING_OBSIDIAN) {
                ServerLevel serverLevel = (ServerLevel) world;

                
                player.getCooldowns().addCooldown(this, 900);

                
                String cmd = "photon fx epca:epca_magic_wedge block " + pos.getX() + " " + (pos.getY() + 1) + " " + pos.getZ();
                serverLevel.getServer().getCommands().performPrefixedCommand(
                        serverLevel.getServer().createCommandSourceStack()
                                .withPosition(Vec3.atCenterOf(pos)),
                        cmd
                );

                
                EffectData data = new EffectData(pos, 600);
                EFFECTS.computeIfAbsent(serverLevel, k -> new ArrayList<>()).add(data);

                
                double range = 128.0;
                for (Player p : serverLevel.players()) {
                    if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= range * range) {
                        serverLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                }

                return InteractionResultHolder.sidedSuccess(itemStack, false);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemStack, false);
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

    

    private static class EffectData {
        final BlockPos pos;
        int remainingTicks;

        EffectData(BlockPos pos, int ticks) {
            this.pos = pos;
            this.remainingTicks = ticks;
        }
    }

    

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            List<EffectData> list = EFFECTS.get(level);
            if (list == null || list.isEmpty()) continue;
            Iterator<EffectData> it = list.iterator();
            while (it.hasNext()) {
                EffectData data = it.next();
                data.remainingTicks--;
                if (data.remainingTicks <= 0) {
                    it.remove();
                    continue;
                }
                
                if (data.remainingTicks % 5 == 0) {
                    AABB area = AABB.ofSize(Vec3.atCenterOf(data.pos), 128, 128, 128); 
                    List<Entity> entities = level.getEntitiesOfClass(Entity.class, area,
                            e -> !(e instanceof Player) && e.isAlive());
                    for (Entity e : entities) {
                        e.discard();
                    }
                }
            }
        }
    }
}