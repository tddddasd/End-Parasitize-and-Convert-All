package org.tdddd.epca.impl.events;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.tdddd.epca.impl.overworld.registry.capability.EpcaAttachments;
import org.tdddd.epca.impl.overworld.registry.capability.ShieldCapability;


public class ShieldCapabilityHandler {

    
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ShieldCapability>> SHIELD_CAP =
            EpcaAttachments.ATTACHMENT_TYPES.register("shield",
                    () -> AttachmentType.serializable(ShieldCapability::new).build());

    /**
     * Forces this class to be initialized while the mod is still being constructed.
     *
     * <p>The static field above registers an attachment type on a {@code DeferredRegister}. NeoForge
     * rejects any registration performed after {@code RegisterEvent} has been fired, so if this class
     * were first loaded later (for example inside a runtime event handler) the game would crash with
     * {@code IllegalStateException: Cannot register new entries to DeferredRegister after RegisterEvent
     * has been fired}. Calling this no-op method from the mod constructor guarantees the registration
     * happens during construction.</p>
     */
    public static void init() {
    }

    private ShieldCapabilityHandler() {
    }
}
