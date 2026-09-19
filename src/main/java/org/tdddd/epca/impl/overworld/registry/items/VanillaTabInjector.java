package org.tdddd.epca.impl.overworld.registry.items;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class VanillaTabInjector {

    private VanillaTabInjector() {}

    @SubscribeEvent
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) return;

        ItemStack copper = new ItemStack(ModItems.COPPER_NUGGET.get());
        var entries = event.getEntries();

        if (entries.contains(copper)) return;

        List<Map.Entry<ItemStack, CreativeModeTab.TabVisibility>> snapshot = new ArrayList<>();
        for (var e : entries) {
            snapshot.add(Map.entry(e.getKey(), e.getValue()));
        }

        for (var e : snapshot) {
            entries.remove(e.getKey());
        }

        boolean inserted = false;
        for (var e : snapshot) {
            entries.put(e.getKey(), e.getValue());
            if (!inserted && e.getKey().is(Items.IRON_NUGGET)) {
                entries.put(copper, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                inserted = true;
            }
        }

        if (!inserted) {
            entries.put(copper, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}