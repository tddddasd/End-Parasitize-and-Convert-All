package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.entity.link.StageIIBeckon;

public class StageIIBeckonSpawnEgg extends ForgeSpawnEggItem {
    private static final String LAST_FAIL_TIME_KEY = "epca_beckon_last_fail_time";
    private static final String FAIL_COUNT_KEY = "epca_beckon_fail_count";
    private static final long RESET_INTERVAL_MS = 5000;

    public StageIIBeckonSpawnEgg(Properties properties) {
        super(() -> ModEntities.STAGE_I_BECKON.get(), -1, -1, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        CompoundTag persistentData = player.getPersistentData();

        int stage = EvolutionManager.getStageForDimension(serverLevel);
        if (stage < 3) {
            long now = System.currentTimeMillis();
            long lastFail = persistentData.getLong(LAST_FAIL_TIME_KEY);
            int failCount = persistentData.getInt(FAIL_COUNT_KEY);

            if (now - lastFail > RESET_INTERVAL_MS) {
                failCount = 0;
            }
            failCount++;
            persistentData.putLong(LAST_FAIL_TIME_KEY, now);
            persistentData.putInt(FAIL_COUNT_KEY, failCount);

            float factor = Math.min(failCount / 10.0f, 1.0f);
            int red = 255;
            int green = (int) (255 * (1 - factor));
            int blue = (int) (255 * (1 - factor));
            int color = (red << 16) | (green << 8) | blue;

            Component message = Component.translatable("epca.message.stage_too_low")
                    .withStyle(style -> style.withColor(color));
            player.displayClientMessage(message, true);

            return InteractionResult.FAIL;
        }

        BlockPos blockPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        ItemStack itemStack = context.getItemInHand();

        double spawnX = blockPos.getX() + 0.5 + direction.getStepX() * 0.5;
        double spawnY = blockPos.getY() + 0.5 + direction.getStepY() * 0.5;
        double spawnZ = blockPos.getZ() + 0.5 + direction.getStepZ() * 0.5;
        Vec3 targetPosition = new Vec3(spawnX, spawnY, spawnZ);

        Entity beckon = ModEntities.STAGE_II_BECKON.get().create(serverLevel);
        if (beckon != null && beckon instanceof StageIIBeckon stageIIBeckon) {
            stageIIBeckon.setRiseTarget(targetPosition);
            serverLevel.addFreshEntity(stageIIBeckon);

            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            persistentData.remove(LAST_FAIL_TIME_KEY);
            persistentData.remove(FAIL_COUNT_KEY);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }
}