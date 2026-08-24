package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.SwallowCystBlockEntity;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.gui.menus.SwallowCystMenu;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, epca.MODID);

    public static final RegistryObject<MenuType<SwallowCystMenu>> SWALLOW_CYST =
            MENUS.register("swallow_cyst",
                    () -> IForgeMenuType.create((windowId, inv, data) -> {
                        BlockPos pos = data.readBlockPos();
                        BlockEntity be = inv.player.level().getBlockEntity(pos);
                        if (be instanceof SwallowCystBlockEntity cyst) {
                            return new SwallowCystMenu(windowId, inv, cyst);
                        }
                        return null;
                    }));
}