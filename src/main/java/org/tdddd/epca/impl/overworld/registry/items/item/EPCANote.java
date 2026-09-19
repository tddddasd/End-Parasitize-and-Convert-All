package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EPCANote extends Item {
    public EPCANote(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            try {
                
                Class<?> utilClass = Class.forName("org.tdddd.epca.impl.utils.ClientScreenUtil");
                java.lang.reflect.Method method = utilClass.getMethod("openNoteScreen");
                method.invoke(null);
            } catch (Exception e) {
                
                e.printStackTrace();
            }
        }
        return InteractionResult.SUCCESS;
    }
}