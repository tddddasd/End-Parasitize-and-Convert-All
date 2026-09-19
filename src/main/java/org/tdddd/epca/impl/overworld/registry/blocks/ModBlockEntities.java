package org.tdddd.epca.impl.overworld.registry.blocks;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.BeckonCoreBlockEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.InfestedSweetBerryBushBlockEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.SwallowCystBlockEntity;
import org.tdddd.epca.impl.epca;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, epca.MODID);

    // packed_mud_pedestal 的方块实体已随祭坛方块分离到前置模组 eej。

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SwallowCystBlockEntity>> SWALLOW_CYST =
            BLOCK_ENTITIES.register("swallow_cyst",
                    () -> new BlockEntityType<>(SwallowCystBlockEntity::new, ModBlocks.SWALLOW_CYST.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeckonCoreBlockEntity>> BECKON_CORE =
            BLOCK_ENTITIES.register("beckon_core",
                    () -> new BlockEntityType<>(BeckonCoreBlockEntity::new,
                            ModBlocks.BECKON_CORE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfestedSweetBerryBushBlockEntity>> INFESTED_SWEET_BERRY_BUSH =
            BLOCK_ENTITIES.register("infested_sweet_berry_bush",
                    () -> new BlockEntityType<>(InfestedSweetBerryBushBlockEntity::new,
                            ModBlocks.INFESTED_SWEET_BERRY_BUSH.get()));
}