package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.epca.impl.overworld.registry.blocks.block.PackedMudPedestal;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.PackedMudPedestalBlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SmallItemFrame extends Item {
    private static final int MAX_IDS = 9;

    public SmallItemFrame(Properties properties) {
        super(properties);
    }

    // ========== NBT 工具方法 ==========
    public static List<String> getItemIds(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains("item_ids")) {
            return new ArrayList<>();
        }
        ListTag listTag = stack.getTag().getList("item_ids", Tag.TAG_STRING);
        List<String> ids = new ArrayList<>();
        for (Tag tag : listTag) {
            ids.add(tag.getAsString());
        }
        return ids;
    }

    public static void setItemIds(ItemStack stack, List<String> ids) {
        ListTag listTag = new ListTag();
        for (String id : ids) {
            listTag.add(StringTag.valueOf(id));
        }
        stack.getOrCreateTag().put("item_ids", listTag);
    }

    public static boolean addItemId(ItemStack stack, String id) {
        List<String> ids = getItemIds(stack);
        if (ids.contains(id)) return false;
        if (ids.size() >= MAX_IDS) return false;
        ids.add(id);
        setItemIds(stack, ids);
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PackedMudPedestal) {
            if (!level.isClientSide && player instanceof ServerPlayer) {
                ItemStack stack = context.getItemInHand();
                List<String> ids = getItemIds(stack);
                if (level.getBlockEntity(pos) instanceof PackedMudPedestalBlockEntity pedestal) {
                    pedestal.setFilterData(ids);
                    level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(offhand.getItem());
            if (id != null) {
                String idStr = id.toString();
                if (addItemId(stack, idStr)) {
                    player.displayClientMessage(
                            Component.translatable("item.small_item_frame.added", idStr), true);
                    return InteractionResult.SUCCESS;
                } else {
                    player.displayClientMessage(
                            Component.translatable("item.small_item_frame.max_reached"), true);
                    return InteractionResult.FAIL;
                }
            }
        } else {
            player.displayClientMessage(
                    Component.translatable("item.small_item_frame.no_offhand"), true);
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        List<String> ids = getItemIds(stack);
        if (!ids.isEmpty()) {
            for (String id : ids) {
                tooltip.add(Component.literal(" - " + id)
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            }
            tooltip.add(Component.translatable("item.small_item_frame.count", ids.size(), MAX_IDS)
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}