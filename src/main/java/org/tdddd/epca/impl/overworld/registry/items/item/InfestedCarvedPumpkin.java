package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedPumpkinHead;

public class InfestedCarvedPumpkin extends BlockItem {

    public InfestedCarvedPumpkin(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && !player.isShiftKeyDown()) {
            Level level = context.getLevel();
            BlockPos clickedPos = context.getClickedPos();
            Direction face = context.getClickedFace();
            if (!level.isClientSide) {
                InfestedPumpkinHead entity = ModEntities.INFESTED_PUMPKIN_HEAD.get().create(level);
                if (entity != null) {
                    double x = clickedPos.getX() + 0.5 + face.getStepX() * 0.5;
                    double y = clickedPos.getY() + 0.5 + face.getStepY() * 0.5;
                    double z = clickedPos.getZ() + 0.5 + face.getStepZ() * 0.5;

                    entity.setPos(x, y, z);
                    Direction facing = context.getHorizontalDirection().getOpposite();
                    entity.setYRot(facing.toYRot());
                    level.addFreshEntity(entity);
                }
            }

            if (!player.getAbilities().instabuild) {
                ItemStack stack = context.getItemInHand();
                stack.shrink(1);
            }

            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}