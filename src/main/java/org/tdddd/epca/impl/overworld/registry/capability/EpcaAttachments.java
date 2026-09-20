package org.tdddd.epca.impl.overworld.registry.capability;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.tdddd.epca.impl.epca;


public class EpcaAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, epca.MODID);

    
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
