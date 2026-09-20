package org.tdddd.epca.impl.events;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.tdddd.epca.impl.overworld.registry.capability.EpcaAttachments;
import org.tdddd.epca.impl.overworld.registry.capability.ShieldCapability;


public class ShieldCapabilityHandler {

    
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ShieldCapability>> SHIELD_CAP =
            EpcaAttachments.ATTACHMENT_TYPES.register("shield",
                    () -> AttachmentType.serializable(ShieldCapability::new).build());

    private ShieldCapabilityHandler() {
    }
}
