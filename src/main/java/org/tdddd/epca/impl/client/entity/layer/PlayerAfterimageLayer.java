package org.tdddd.epca.impl.client.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.client.entity.ModelPartSnapshot;
import org.tdddd.epca.impl.client.entity.PlayerAfterimageData;
import org.tdddd.epca.impl.overworld.registry.items.item.AfterimageModule;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerAfterimageLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final Map<UUID, List<PlayerAfterimageData>> AFTERIMAGES = new ConcurrentHashMap<>();
    private static final int MAX_AFTERIMAGES = 12;
    private static final float SPAWN_CHANCE = 0.30f;
    private static final float SPEED_THRESHOLD_SQ = 0.02f;

    public PlayerAfterimageLayer(PlayerRenderer renderer) {
        super(renderer);
    }

    private static boolean isWearingFullLivingArmor(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        return isLivingArmor(helmet) &&
                isLivingArmor(chestplate) &&
                isLivingArmor(leggings) &&
                isLivingArmor(boots);
    }

    private static boolean isLivingArmor(ItemStack stack) {
        return stack.getItem() instanceof LivingArmorItem;
    }

    private static ItemStack findLivingArmorBox(Player player) {

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (mainHand.getItem() instanceof LivingArmorBox) {
            return mainHand;
        }
        if (offHand.getItem() instanceof LivingArmorBox) {
            return offHand;
        }


        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof LivingArmorBox) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static boolean hasInstinctModuleI(ItemStack boxStack) {
        if (!(boxStack.getItem() instanceof LivingArmorBox)) {
            return false;
        }

        LivingArmorBox box = (LivingArmorBox) boxStack.getItem();
        List<ItemStack> storedItems = box.getStoredItems(boxStack);

        for (ItemStack storedItem : storedItems) {
            if (storedItem.getItem() instanceof AfterimageModule) {
                return true;
            }
        }

        return false;
    }

    private void trySpawn(Player player, int currentTick) {
        UUID id = player.getUUID();
        List<PlayerAfterimageData> list = AFTERIMAGES.computeIfAbsent(id, k -> new ArrayList<>());
        list.removeIf(data -> !data.isAlive(currentTick));

        if (list.size() >= MAX_AFTERIMAGES) return;

        double dx = player.getX() - player.xo;
        double dy = player.getY() - player.yo;
        double dz = player.getZ() - player.zo;
        double distSq = dx*dx + dy*dy + dz*dz;
        if (distSq < SPEED_THRESHOLD_SQ) return;

        if (player.getRandom().nextFloat() < SPAWN_CHANCE) {
            Map<String, ModelPartSnapshot> snapshots = captureCurrentPose(getParentModel());
            list.add(new PlayerAfterimageData(player, currentTick, snapshots));
        }
    }

    private Map<String, ModelPartSnapshot> captureCurrentPose(PlayerModel<AbstractClientPlayer> model) {
        Map<String, ModelPartSnapshot> snapshots = new HashMap<>();
        // 根据部件名称存储
        snapshots.put("head", new ModelPartSnapshot(model.head));
        snapshots.put("body", new ModelPartSnapshot(model.body));
        snapshots.put("leftArm", new ModelPartSnapshot(model.leftArm));
        snapshots.put("rightArm", new ModelPartSnapshot(model.rightArm));
        snapshots.put("leftLeg", new ModelPartSnapshot(model.leftLeg));
        snapshots.put("rightLeg", new ModelPartSnapshot(model.rightLeg));
        snapshots.put("hat", new ModelPartSnapshot(model.hat));
        // 如果有 cloak 等也可以加，但 PlayerModel 没有 cloak，cloak 是独立渲染的
        return snapshots;
    }

    private void applySnapshots(Map<String, ModelPartSnapshot> snapshots, PlayerModel<AbstractClientPlayer> model) {
        applyPart(model.head, snapshots.get("head"));
        applyPart(model.body, snapshots.get("body"));
        applyPart(model.leftArm, snapshots.get("leftArm"));
        applyPart(model.rightArm, snapshots.get("rightArm"));
        applyPart(model.leftLeg, snapshots.get("leftLeg"));
        applyPart(model.rightLeg, snapshots.get("rightLeg"));
        applyPart(model.hat, snapshots.get("hat"));
    }

    private void applyPart(ModelPart part, ModelPartSnapshot snapshot) {
        if (snapshot != null) snapshot.applyTo(part);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, AbstractClientPlayer abstractClientPlayer, float v, float v1, float v2, float v3, float v4, float v5) {
        int currentTick = abstractClientPlayer.tickCount;
        UUID id = abstractClientPlayer.getUUID();

        if (abstractClientPlayer.level().isClientSide()) {
            return;
        }

        if (!isWearingFullLivingArmor(abstractClientPlayer)) {
            return;
        }

        ItemStack boxStack = findLivingArmorBox(abstractClientPlayer);
        if (boxStack.isEmpty()) {
            return;
        }

        LivingArmorBox boxItem = (LivingArmorBox) boxStack.getItem();

        if (!boxItem.getState(boxStack)) {
            return;
        }

        if (!hasInstinctModuleI(boxStack)) {
            return;
        }
        trySpawn(abstractClientPlayer, currentTick);

        List<PlayerAfterimageData> list = AFTERIMAGES.get(id);
        if (list == null || list.isEmpty()) return;

        list.removeIf(data -> !data.isAlive(currentTick));

        PlayerModel<AbstractClientPlayer> model = getParentModel();
        Map<String, ModelPartSnapshot> originalState = captureCurrentPose(model);

        for (PlayerAfterimageData data : list) {
            float alpha = data.getAlpha(currentTick);
            if (alpha <= 0) continue;

            poseStack.pushPose();
            var pos = data.position;
            poseStack.translate(pos.x - abstractClientPlayer.getX(), pos.y - abstractClientPlayer.getY() + 0.02, pos.z - abstractClientPlayer.getZ());
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - data.yRot));

            applySnapshots(data.partSnapshots, model);

            RenderType renderType = RenderType.entityTranslucent(abstractClientPlayer.getSkinTextureLocation());
            VertexConsumer consumer = multiBufferSource.getBuffer(renderType);
            model.renderToBuffer(poseStack, consumer, i, OverlayTexture.NO_OVERLAY,
                    1.0f, 1.0f, 1.0f, alpha * 0.55f);

            poseStack.popPose();
        }

        applySnapshots(originalState, model);
    }
}