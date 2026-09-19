package org.tdddd.epca.impl.overworld.registry.capability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;

/**
 * 生物存活计时（到点自动 {@code discard()}）。
 *
 * <p><b>26.1.2 改动</b>
 * <ul>
 *   <li>Forge Capability（{@code Capability<ILifetimeCapability> LIFETIME} +
 *       {@code AttachCapabilitiesEvent} + {@code ICapabilitySerializable} + {@code LazyOptional}）
 *       整体删除。附件类型现在是
 *       {@link EpcaAttachments#LIFETIME}（注册在 {@code ATTACHMENT_TYPES}），
 *       读取用 {@code livingEntity.getData(EpcaAttachments.LIFETIME)}。</li>
 *   <li>持久化：{@code INBTSerializable<CompoundTag>}（已删除）→
 *       {@code ValueIOSerializable}（{@link ValueOutput}/{@link ValueInput}）。
 *       <b>存档字段名不变</b>：{@code RemainingTicks}（int）。</li>
 *   <li>{@code LivingEvent.LivingTickEvent} → {@code EntityTickEvent.Post}
 *       （与原来“每 tick 末尾执行”的时机一致；原代码只关心执行，不读事件数据）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = epca.MODID)
public class LifetimeCapability implements ILifetimeCapability, net.neoforged.neoforge.common.util.ValueIOSerializable {

    /** 向后兼容别名：1.20.1 里该字段是 {@code Capability<ILifetimeCapability>}。 */
    public static final net.neoforged.neoforge.registries.DeferredHolder<
            net.neoforged.neoforge.attachment.AttachmentType<?>,
            net.neoforged.neoforge.attachment.AttachmentType<LifetimeCapability>> LIFETIME =
            EpcaAttachments.LIFETIME;

    private int remainingTicks = -1; 
    private LivingEntity entity;     

    public LifetimeCapability(LivingEntity entity) {
        this.entity = entity;
    }

    @Override
    public void setRemainingTicks(int ticks) {
        this.remainingTicks = ticks;
    }

    @Override
    public int getRemainingTicks() {
        return remainingTicks;
    }

    @Override
    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks == 0 && entity != null && !entity.level().isClientSide()) {
                entity.discard(); 
            }
        }
    }

    // ═══════════════ ValueIOSerializable（取代 INBTSerializable<CompoundTag>） ═══════════════

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("RemainingTicks", this.remainingTicks);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.remainingTicks = input.getIntOr("RemainingTicks", -1);
    }

    
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living) {
            // 等价于 1.20.1 的 getCapability(...).ifPresent(...)：附件未被创建过就什么都不做，
            // 避免给全世界每个生物每 tick 都惰性创建一个永远不会被使用的附件。
            LifetimeCapability cap = living.getExistingDataOrNull(EpcaAttachments.LIFETIME);
            if (cap != null) {
                cap.tick();
            }
        }
    }
}
