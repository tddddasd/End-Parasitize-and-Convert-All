package org.tdddd.epca.impl.overworld.registry.capability;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.tdddd.epca.impl.epca;

/**
 * epca 的数据附件注册持有类（26.1.2 取代 Forge Capability）。
 *
 * <p>1.20.1 里 {@code LifetimeCapability} 是一个 {@code Capability<ILifetimeCapability>}，
 * 通过 {@code AttachCapabilitiesEvent} 逐实体附加。26.1.2 删除了整套 Capability 概念，
 * 改为**数据附件**：附件类型注册到 {@code NeoForgeRegistries.Keys.ATTACHMENT_TYPES}，
 * 读取用 {@code IAttachmentHolder#getData(AttachmentType)}，写入用 {@code setData}。
 * 附件类型必须在**模组总线**注册，由 {@code epca} 的构造器调用 {@link #register(IEventBus)}。
 *
 * <p>附件名沿用 1.20.1 的能力 id：{@code epca:lifetime}。
 *
 * <p><b>类型说明</b>：{@code AttachmentType.serializable(...)} 要求
 * {@code T extends ValueIOSerializable}，因此附件的类型参数用具体实现类
 * {@link LifetimeCapability}（它实现了 {@link ILifetimeCapability} 与
 * {@code ValueIOSerializable}）。原 1.20.1 的 {@code Capability<ILifetimeCapability>}
 * 接口类型因此收窄为具体类，调用方从 {@code getCapability(...)} 改为
 * {@code getData(EpcaAttachments.LIFETIME)}。
 */
public class EpcaAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, epca.MODID);

    /** 生物的存活计时（原 {@code LifetimeCapability.LIFETIME}）。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LifetimeCapability>> LIFETIME =
            ATTACHMENT_TYPES.register("lifetime",
                    () -> AttachmentType.serializable(
                            holder -> new LifetimeCapability((LivingEntity) holder)).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    private EpcaAttachments() {
    }
}
