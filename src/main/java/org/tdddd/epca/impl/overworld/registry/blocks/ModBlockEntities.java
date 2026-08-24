package org.tdddd.epca.impl.overworld.registry.blocks;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.BeckonCoreBlockEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.InfestedSweetBerryBushBlockEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.SwallowCystBlockEntity;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.PackedMudPedestalBlockEntity;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, epca.MODID);

    public static final RegistryObject<BlockEntityType<PackedMudPedestalBlockEntity>> PACKED_MUD_PEDESTAL =
            BLOCK_ENTITIES.register("packed_mud_pedestal", () ->
                        BlockEntityType.Builder.of(PackedMudPedestalBlockEntity::new, ModBlocks.PACKED_MUD_PEDESTAL.get())
                                .build(null));

    public static final RegistryObject<BlockEntityType<SwallowCystBlockEntity>> SWALLOW_CYST =
            BLOCK_ENTITIES.register("swallow_cyst",
                    () -> BlockEntityType.Builder.of(SwallowCystBlockEntity::new, ModBlocks.SWALLOW_CYST.get()).build(null));

    public static final RegistryObject<BlockEntityType<BeckonCoreBlockEntity>> BECKON_CORE =
            BLOCK_ENTITIES.register("beckon_core",
                    () -> BlockEntityType.Builder.of(BeckonCoreBlockEntity::new,
                            ModBlocks.BECKON_CORE.get()).build(null));

    public static final RegistryObject<BlockEntityType<InfestedSweetBerryBushBlockEntity>> INFESTED_SWEET_BERRY_BUSH =
            BLOCK_ENTITIES.register("infested_sweet_berry_bush",
                    () -> BlockEntityType.Builder.of(InfestedSweetBerryBushBlockEntity::new,
                            ModBlocks.INFESTED_SWEET_BERRY_BUSH.get()).build(null));
}