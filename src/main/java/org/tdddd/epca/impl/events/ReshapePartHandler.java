package org.tdddd.epca.impl.events;

import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import org.tdddd.epca.impl.epca;

@EventBusSubscriber(modid = epca.MODID)
public class ReshapePartHandler {

    private static final String TARGET_ID = "epca:reshape_part";

    // 注意：必须写成 static，注解才会自动注册
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        // 只在游戏逻辑刻结束时执行，并过滤掉客户端（防止双端执行）
        if (event.getLevel().isClientSide()) {
            return;
        }

        Level level = event.getLevel();
        // 检测原点 (0,0,0) 附近极小范围的实体
        AABB originBox = new AABB(BlockPos.ZERO).inflate(0.001);

        level.getEntities(null, originBox).forEach(entity -> {
            // 通过注册名匹配实体类型
            Identifier rl = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (rl != null && rl.toString().equals(TARGET_ID)) {
                // 精确判断坐标是否为 (0,0,0)
                if (entity.getX() == 0.0D && entity.getY() == 0.0D && entity.getZ() == 0.0D) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                }
            }
        });
    }
}