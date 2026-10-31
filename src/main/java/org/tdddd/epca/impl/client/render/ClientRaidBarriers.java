package org.tdddd.epca.impl.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.overworld.raid.InfestedRaidBarrierData;

import java.util.List;

/** Client-side mirror of the raid barriers the server has told us about. */
@OnlyIn(Dist.CLIENT)
public final class ClientRaidBarriers {
    private static volatile List<InfestedRaidBarrierData.Barrier> barriers = List.of();

    private ClientRaidBarriers() { }

    public static void replace(List<InfestedRaidBarrierData.Barrier> next) {
        barriers = List.copyOf(next);
    }

    public static List<InfestedRaidBarrierData.Barrier> all() {
        return barriers;
    }

    /** Barriers that belong to the given dimension and have not expired yet. */
    public static List<InfestedRaidBarrierData.Barrier> activeIn(ResourceLocation dimension) {
        return barriers.stream()
                .filter(b -> b.dimension().equals(dimension) && b.active())
                .toList();
    }
}
