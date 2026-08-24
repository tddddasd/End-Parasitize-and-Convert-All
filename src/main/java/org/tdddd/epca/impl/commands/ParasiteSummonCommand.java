package org.tdddd.epca.impl.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;

import java.util.Collection;

public class ParasiteSummonCommand {
    private static final String PARASITE_TAG_KEY = "Parasite";

    private static final SuggestionProvider<CommandSourceStack> ENTITY_TYPE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    ForgeRegistries.ENTITY_TYPES.getKeys().stream()
                            .map(id -> "\"" + id.toString() + "\""),
                    builder);

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("epca_parasitesummon")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("entityType", StringArgumentType.string())
                        .suggests(ENTITY_TYPE_SUGGESTIONS)
                        .executes(ctx -> summonEntity(ctx, StringArgumentType.getString(ctx, "entityType"), null, null))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(ctx -> summonEntity(ctx,
                                        StringArgumentType.getString(ctx, "entityType"),
                                        Vec3Argument.getVec3(ctx, "pos"),
                                        null))
                                .then(Commands.argument("nbt", StringArgumentType.greedyString())
                                        .executes(ctx -> summonEntity(ctx,
                                                StringArgumentType.getString(ctx, "entityType"),
                                                Vec3Argument.getVec3(ctx, "pos"),
                                                StringArgumentType.getString(ctx, "nbt"))))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> registerSetParasite() {
        return Commands.literal("epca_setparasite")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(ctx -> setParasite(ctx, EntityArgument.getEntities(ctx, "targets"))));
    }

    private static int setParasite(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> entities) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        if (entities.isEmpty()) {
            source.sendFailure(Component.literal("没有找到任何实体"));
            return 0;
        }

        int successCount = 0;
        for (Entity entity : entities) {
            entity.getPersistentData().putBoolean(PARASITE_TAG_KEY, true);
            if (entity instanceof LivingEntity living) {
                float multiplier = DifficultyEffects.getParasiteStatMultiplier(living.level());
                double newMaxHealth = living.getMaxHealth() * multiplier;
                float healthRatio = living.getHealth() / living.getMaxHealth();
                living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
                living.setHealth((float) (newMaxHealth * healthRatio));
                double newArmor = living.getArmorValue() * multiplier;
                living.getAttribute(Attributes.ARMOR).setBaseValue(newArmor);
            }
            successCount++;
        }
        int finalSuccessCount = successCount;
        source.sendSuccess(() -> Component.literal("已为 " + finalSuccessCount + " 个实体添加Parasite标签"), true);
        return successCount;
    }

    private static int summonEntity(CommandContext<CommandSourceStack> ctx, String entityTypeStr, Vec3 pos, String nbtString) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();

        ResourceLocation entityId = ResourceLocation.tryParse(entityTypeStr);
        if (entityId == null || !ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
            source.sendFailure(Component.literal("未知实体类型: " + entityTypeStr));
            return 0;
        }
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (entityType == null) {
            source.sendFailure(Component.literal("无法获取实体类型: " + entityTypeStr));
            return 0;
        }

        Vec3 spawnPos = pos == null ? source.getPosition() : pos;
        if (spawnPos.y < -64) spawnPos = new Vec3(spawnPos.x, -64, spawnPos.z);
        if (spawnPos.y > 319) spawnPos = new Vec3(spawnPos.x, 319, spawnPos.z);

        CompoundTag userNbt = new CompoundTag();
        if (nbtString != null && !nbtString.isEmpty()) {
            try {
                userNbt = TagParser.parseTag(nbtString);
            } catch (CommandSyntaxException e) {
                source.sendFailure(Component.literal("NBT解析错误: " + e.getMessage()));
                return 0;
            }
        }

        Entity entity = entityType.create(level);
        if (entity == null) {
            source.sendFailure(Component.literal("无法生成实体: " + entityTypeStr));
            return 0;
        }

        entity.load(userNbt);
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        entity.getPersistentData().putBoolean(PARASITE_TAG_KEY, true);

        level.addFreshEntity(entity);
        Vec3 finalSpawnPos = spawnPos;
        source.sendSuccess(() -> Component.literal("已生成实体: " + entityTypeStr + " 在 " + finalSpawnPos.x + " " + finalSpawnPos.y + " " + finalSpawnPos.z), true);
        return 1;
    }
}