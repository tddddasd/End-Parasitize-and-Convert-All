package org.tdddd.epca.impl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;

import java.util.Collection;

public class NegativeDamageCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epca_negativedamage")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                .executes(context -> {
                                    return damage(context.getSource(),
                                            EntityArgument.getEntities(context, "targets"),
                                            FloatArgumentType.getFloat(context, "amount"),
                                            "minecraft:generic",  
                                            null, null, null);
                                })
                                .then(Commands.argument("damageType", ResourceLocationArgument.id())
                                        .executes(context -> {
                                            return damage(context.getSource(),
                                                    EntityArgument.getEntities(context, "targets"),
                                                    FloatArgumentType.getFloat(context, "amount"),
                                                    ResourceLocationArgument.getId(context, "damageType").toString(),
                                                    null, null, null);
                                        })
                                        .then(Commands.argument("at", Vec3Argument.vec3())
                                                .executes(context -> {
                                                    return damage(context.getSource(),
                                                            EntityArgument.getEntities(context, "targets"),
                                                            FloatArgumentType.getFloat(context, "amount"),
                                                            ResourceLocationArgument.getId(context, "damageType").toString(),
                                                            Vec3Argument.getVec3(context, "at"), null, null);
                                                })
                                                .then(Commands.argument("by", EntityArgument.entity())
                                                        .executes(context -> {
                                                            return damage(context.getSource(),
                                                                    EntityArgument.getEntities(context, "targets"),
                                                                    FloatArgumentType.getFloat(context, "amount"),
                                                                    ResourceLocationArgument.getId(context, "damageType").toString(),
                                                                    Vec3Argument.getVec3(context, "at"),
                                                                    EntityArgument.getEntity(context, "by"),
                                                                    null);
                                                        })
                                                        .then(Commands.argument("from", EntityArgument.entity())
                                                                .executes(context -> {
                                                                    return damage(context.getSource(),
                                                                            EntityArgument.getEntities(context, "targets"),
                                                                            FloatArgumentType.getFloat(context, "amount"),
                                                                            ResourceLocationArgument.getId(context, "damageType").toString(),
                                                                            Vec3Argument.getVec3(context, "at"),
                                                                            EntityArgument.getEntity(context, "by"),
                                                                            EntityArgument.getEntity(context, "from"));
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        
        
    }

    private static int damage(CommandSourceStack source, Collection<? extends Entity> targets, float amount,
                              String damageType, Vec3 position, Entity directEntity, Entity causingEntity) throws CommandSyntaxException {
        int successCount = 0;

        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;

                
                DamageSource finalDamageSource = createDamageSource(source, damageType, position, directEntity, causingEntity);

                
                float oldHealth = livingEntity.getHealth();

                
                
                boolean wasHurt = livingEntity.hurt(finalDamageSource, amount);

                
                float newHealth = livingEntity.getHealth();
                float actualChange = newHealth - oldHealth;

                
                if (amount < 0) {
                    
                    if (actualChange > 0) {
                        
                        if (directEntity != null) {
                            source.sendSuccess(() -> Component.translatable("commands.negativedamage.heal.success",
                                    livingEntity.getDisplayName(),
                                    String.format("%.1f", actualChange),
                                    directEntity.getDisplayName()), true);
                        } else {
                            source.sendSuccess(() -> Component.translatable("commands.negativedamage.heal.success.unknown",
                                    livingEntity.getDisplayName(),
                                    String.format("%.1f", actualChange)), true);
                        }
                        successCount++;
                    } else {
                        
                        if (directEntity != null) {
                            source.sendSuccess(() -> Component.translatable("commands.negativedamage.heal.failed",
                                    livingEntity.getDisplayName(),
                                    directEntity.getDisplayName()), true);
                        } else {
                            source.sendSuccess(() -> Component.translatable("commands.negativedamage.heal.failed.unknown",
                                    livingEntity.getDisplayName()), true);
                        }
                    }
                } else {
                    
                    if (wasHurt) {
                        if (directEntity != null) {
                            source.sendSuccess(() -> Component.translatable("commands.negativedamage.damage.success",
                                    livingEntity.getDisplayName(),
                                    String.format("%.1f", amount),
                                    directEntity.getDisplayName()), true);
                        } else {
                            source.sendSuccess(() -> Component.translatable("commands.negativedamage.damage.success.unknown",
                                    livingEntity.getDisplayName(),
                                    String.format("%.1f", amount)), true);
                        }
                        successCount++;
                    } else {
                        
                        
                        if (DamageAdaptation.isInvulnerable(livingEntity)) {
                            source.sendSuccess(() -> Component.translatable("commands.negativedamage.damage.invulnerable",
                                    livingEntity.getDisplayName()), true);
                        } else if (directEntity != null) {
                            source.sendSuccess(() -> Component.translatable("commands.negativedamage.damage.failed",
                                    livingEntity.getDisplayName(),
                                    directEntity.getDisplayName()), true);
                        } else {
                            source.sendSuccess(() -> Component.translatable("commands.negativedamage.damage.failed.unknown",
                                    livingEntity.getDisplayName()), true);
                        }
                    }
                }
            }
        }

        if (successCount == 0 && !targets.isEmpty()) {
            source.sendFailure(Component.translatable("commands.negativedamage.no_valid_entities"));
        }

        return successCount;
    }

    private static DamageSource createDamageSource(CommandSourceStack source, String damageType,
                                                   Vec3 position, Entity directEntity, Entity causingEntity) {
        var registry = source.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        ResourceLocation damageTypeId;

        
        if (damageType.contains(":")) {
            damageTypeId = new ResourceLocation(damageType);
        } else {
            damageTypeId = new ResourceLocation("minecraft", damageType);
        }

        DamageType type = registry.get(damageTypeId);

        if (type == null) {
            
            type = registry.get(new ResourceLocation("minecraft:generic"));
        }

        
        DamageSource damageSource;
        if (directEntity != null && causingEntity != null) {
            damageSource = new DamageSource(registry.getHolderOrThrow(registry.getResourceKey(type).get()),
                    directEntity, causingEntity);
        } else if (directEntity != null) {
            damageSource = new DamageSource(registry.getHolderOrThrow(registry.getResourceKey(type).get()),
                    directEntity);
        } else if (source.getEntity() != null) {
            damageSource = new DamageSource(registry.getHolderOrThrow(registry.getResourceKey(type).get()),
                    source.getEntity());
        } else {
            damageSource = new DamageSource(registry.getHolderOrThrow(registry.getResourceKey(type).get()));
        }

        return damageSource;
    }
}