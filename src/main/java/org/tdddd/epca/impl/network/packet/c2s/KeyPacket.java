package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.registry.items.item.DiseasedHeart;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedFlesh;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.ParasiteViscera;

/**
 * 客户端 → 服务端：无数据的按键动作包（活体护甲箱左键交互）。
 *
 * <p><b>26.1.2 改动</b>：{@code SimpleChannel} → {@link CustomPacketPayload}。
 * 负载本身没有字段（{@code encode} 什么也不写），线上格式与 1.20.1 一致。
 * {@code ItemStack#isEdible()} 在 26.1.2 已删除，等价判断改为
 * {@code stack.get(DataComponents.FOOD) != null}（食物现在由 {@code minecraft:food} 数据组件表达）。
 */
public class KeyPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KeyPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("key_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KeyPacket> STREAM_CODEC =
            CustomPacketPayload.codec(KeyPacket::toBytes, KeyPacket::new);

    public KeyPacket() {

    }

    public KeyPacket(RegistryFriendlyByteBuf buf) {

    }

    public void toBytes(RegistryFriendlyByteBuf buf) {

    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KeyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                packet.handleKeyAction(player);
            }
        });
    }

    /** 26.1.2 的“可食用”判断：{@code minecraft:food} 组件存在即视为食物。 */
    private static boolean isEdible(ItemStack stack) {
        return stack.get(DataComponents.FOOD) != null;
    }

    private void handleKeyAction(ServerPlayer player) {

        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();

        if (!mainHandItem.isEmpty() && (isEdible(mainHandItem) || mainHandItem.getItem() instanceof InfestedFlesh || mainHandItem.getItem() instanceof DiseasedHeart
                || mainHandItem.getItem() instanceof ParasiteViscera) &&
                offHandItem.getItem() instanceof LivingArmorBox) {

            LivingArmorBox box = (LivingArmorBox) offHandItem.getItem();
            box.handleLeftClickInGUI(player, offHandItem, mainHandItem);
        }

        else if (!offHandItem.isEmpty() && (isEdible(offHandItem) || offHandItem.getItem() instanceof InfestedFlesh || offHandItem.getItem() instanceof DiseasedHeart
                || offHandItem.getItem() instanceof ParasiteViscera) &&
                mainHandItem.getItem() instanceof LivingArmorBox) {

            LivingArmorBox box = (LivingArmorBox) mainHandItem.getItem();
            box.handleLeftClickInGUI(player, mainHandItem, offHandItem);
        }
    }
}
