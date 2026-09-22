package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.client.render.sky.SkyRuptureEffect;

import java.util.HashMap;
import java.util.Map;

public class ClientEvolutionData {
    private static final Map<ResourceLocation, Integer> DIMENSION_STAGES = new HashMap<>();

    /**
     * 上一次看到的客户端世界实例。换世界 / 换维度时它就是新的对象，
     * 用来判断"该重新认知阶段了"。
     */
    private static Level lastSeenLevel;

    public static void updateStage(ResourceLocation dim, int stage) {
        Minecraft mc = Minecraft.getInstance();

        // 进入新世界 / 切换维度：把认知清空。
        // 这样紧接着的那次同步会被当成"首次同步"（previous == null）而不触发结界破损，
        // 否则"从阶段 5 的世界退出、进入阶段 10 的世界"会在登录瞬间炸一次天空。
        if (mc.level != null && mc.level != lastSeenLevel) {
            lastSeenLevel = mc.level;
            DIMENSION_STAGES.clear();
            SkyRuptureEffect.stop();
        }

        Integer previous = DIMENSION_STAGES.put(dim, stage);

        // ── 世界结界破损：只在"阶段提升"且发生在玩家当前所在维度时触发 ──
        // previous == null：首次同步（进世界 / 换维度），只记录不触发。
        // 阶段下降（管理员回退）：同样不触发。
        if (previous != null && stage > previous) {
            if (mc.level != null && mc.level.dimension().location().equals(dim)) {
                SkyRuptureEffect.trigger(stage);
            }
        }
    }

    public static int getStageForDimension(Level level) {
        if (level == null) return 0;
        return DIMENSION_STAGES.getOrDefault(level.dimension().location(), 0);
    }

    
    public static void clear() {
        DIMENSION_STAGES.clear();
        lastSeenLevel = null;
        SkyRuptureEffect.stop();
    }
}
