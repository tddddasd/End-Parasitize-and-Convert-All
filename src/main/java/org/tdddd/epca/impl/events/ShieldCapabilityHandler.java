package org.tdddd.epca.impl.events;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.tdddd.epca.impl.overworld.registry.capability.EpcaAttachments;
import org.tdddd.epca.impl.overworld.registry.capability.ShieldCapability;

/**
 * 灵魂护盾的数据附件。
 *
 * <p><b>26.1.2 改动</b>：1.20.1 是
 * {@code Capability<IShieldCapability> SHIELD_CAP = CapabilityManager.get(...)}
 * 且在 {@code RegisterCapabilitiesEvent} 里 {@code event.register(IShieldCapability.class)}
 * 声明能力接口。26.1.2 既没有 Forge Capability，也没有“声明能力接口”这一步
 * （{@code RegisterCapabilitiesEvent} 只用于方块/实体/物品的能力提供者），
 * 因此整个类改为数据附件的注册持有者，附件名沿用 {@code epca:shield}。
 * 附件类型由 {@code EpcaAttachments.ATTACHMENT_TYPES} 统一在模组总线注册。
 *
 * <p>读取方式：{@code livingEntity.getData(ShieldCapabilityHandler.SHIELD_CAP)}。
 * 类型参数用具体实现类 {@link ShieldCapability}（{@code AttachmentType.serializable}
 * 要求 {@code T extends ValueIOSerializable}）。
 */
public class ShieldCapabilityHandler {

    /** 灵魂护盾附件（原 {@code Capability<IShieldCapability>}）。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ShieldCapability>> SHIELD_CAP =
            EpcaAttachments.ATTACHMENT_TYPES.register("shield",
                    () -> AttachmentType.serializable(ShieldCapability::new).build());

    private ShieldCapabilityHandler() {
    }
}
