package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.OrganModelDisplay;

/**
 * 让村民害怕寄生体 / 领巢者。
 *
 * <p><b>村民被领巢者吓跑的链路</b>：原版村民没有「躲玩家」的 AI，这条恐惧完全来自本类给每只村民
 * 添加的 {@link AvoidEntityGoal}，它的目标选择谓词用的是 {@link ParasiteEvents#isParasiteByTagOrInterface}
 * （领巢者玩家被视作寄生体）。因此这里只需要给谓词加一个开关判断即可，不需要动原版 AI。</p>
 *
 * <p>开关规则（2026-11 追加需求）：领巢者玩家只有 <b>开启器官模型显示</b>
 * （{@code EpcaOrganModelVisible = true}）时才会被村民当成威胁；默认关闭时村民完全无视他。
 * 谓词是每 tick 由 AI 重新求值的，所以玩家中途切换开关会立刻生效。其它寄生体不受影响。</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID)
public class VillagerSpawnEventHandler {

    @SubscribeEvent
    public static void onVillagerSpawn(EntityJoinLevelEvent event) {

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Villager villager) {

            villager.goalSelector.addGoal(1, new AvoidEntityGoal<>(
                    villager,
                    LivingEntity.class,
                    entity -> isScary(entity),
                    8.0F,
                    0.9F,
                    1.0F,
                    entity -> isScary(entity)
                            && villager.distanceToSqr(entity) < 64.0D
                            && villager.hasLineOfSight(entity)
            ));
        }
    }

    /**
     * 该实体是否应该让村民逃跑。
     *
     * @return {@code true} 才会进入「远离该实体」的目标选择
     */
    private static boolean isScary(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;
        if (!ParasiteEvents.isParasiteByTagOrInterface(entity)) return false;

        if (entity instanceof Player player && HiveLeaderManager.isHiveLeader(player.getUUID())) {
            // 未开启器官模型显示的领巢者不会吓跑村民（默认关闭）。
            return OrganModelDisplay.isVisible(player);
        }
        return true;
    }
}
