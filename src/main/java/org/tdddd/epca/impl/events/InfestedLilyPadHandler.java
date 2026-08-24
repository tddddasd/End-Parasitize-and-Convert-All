package org.tdddd.epca.impl.events;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.InfestedSourcePacket;
import org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedLilyPad;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InfestedLilyPadHandler {
    // 记录每个玩家最后所在的虫染睡莲位置
    private static final Map<UUID, BlockPos> PLAYER_LAST_LILY = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            Level level = player.level();
            if (level.isClientSide) continue;

            UUID uuid = player.getUUID();
            BlockPos lastPos = PLAYER_LAST_LILY.get(uuid);

            // 获取玩家边界框并扩大 0.05 容差
            AABB boundingBox = player.getBoundingBox().inflate(0.05);
            BlockPos foundLilyPos = null;

            // 遍历扩大后的边界框覆盖的所有方块
            int minX = (int) Math.floor(boundingBox.minX);
            int minY = (int) Math.floor(boundingBox.minY);
            int minZ = (int) Math.floor(boundingBox.minZ);
            int maxX = (int) Math.floor(boundingBox.maxX - 1e-6);
            int maxY = (int) Math.floor(boundingBox.maxY - 1e-6);
            int maxZ = (int) Math.floor(boundingBox.maxZ - 1e-6);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.getBlock() instanceof InfestedLilyPad) {
                            foundLilyPos = pos;
                            break;
                        }
                    }
                    if (foundLilyPos != null) break;
                }
                if (foundLilyPos != null) break;
            }

            if (foundLilyPos != null) {
                if (lastPos == null || !lastPos.equals(foundLilyPos)) {
                    // 首次进入
                    if (level.random.nextFloat() < 0.5f) {
                        level.destroyBlock(foundLilyPos, false);
                    }
                    PLAYER_LAST_LILY.put(uuid, foundLilyPos);
                }
            } else {
                // 不在任何睡莲上
                if (lastPos != null) {
                    PLAYER_LAST_LILY.remove(uuid);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        // 仅在服务端处理
        if (event.getEntity() == null || event.getEntity().level().isClientSide) return;
        Level level = event.getEntity().level();
        if (!(level instanceof ServerLevel)) return;

        BlockState placedState = event.getPlacedBlock();
        // 只处理原版睡莲
        if (!placedState.is(Blocks.LILY_PAD)) return;

        BlockPos pos = event.getPos();
        BlockConversionManager manager = BlockConversionManager.getInstance();

        // 查找下方七格内是否有虫染方块
        BlockState infestedBelow = manager.findInfestedBlockBelow(level, pos, 7);
        if (infestedBelow == null) return;

        // 获取目标虫染睡莲方块（优先配置，否则默认）
        Block targetBlock = manager.getTargetLilyPadBlock();
        if (targetBlock == null || targetBlock == Blocks.AIR) return;

        // 构建新状态
        BlockState newState = targetBlock.defaultBlockState();

        if (newState.hasProperty(InfestedLilyPad.NATURAL_SPAWN)) {
            newState = newState.setValue(InfestedLilyPad.NATURAL_SPAWN, true);
        }

        event.setCanceled(true); // 取消原放置
        level.setBlock(pos, newState, 3); // 手动设置
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 仅在服务端执行
        if (event.getSide().isClient()) return;

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof InfestedLilyPad)) return;

        Level level = event.getLevel();
        BlockPos targetPos = event.getPos();
        Direction face = event.getFace();
        if (face == null) return;

        // 1. 判断目标方块是否为静态水源
        BlockState targetState = level.getBlockState(targetPos);
        FluidState fluid = targetState.getFluidState();
        boolean isWaterSource = fluid.getType() == Fluids.WATER && fluid.isSource();

        BlockPos placePos;
        if (isWaterSource) {
            // 点击水方块 → 只能点击顶面，放置在水面上方一格
            if (face == Direction.UP) {
                placePos = targetPos.above();
            } else {
                event.setCanceled(true);
                return;
            }
        } else {
            // 非水方块 → 根据点击面计算位置
            if (face == Direction.UP) {
                // 顶面 → 上方第二格
                placePos = targetPos.above(2);
            } else if (face.getAxis().isHorizontal()) {
                // 侧面 → 侧面上方一格
                placePos = targetPos.relative(face).above();
            } else {
                event.setCanceled(true);
                return;
            }
        }

        // 2. 验证放置位置是否合法（下方静态水源、自身为空气）
        if (!isValidPlacement(level, placePos)) {
            event.setCanceled(true);
            return;
        }

        // 3. 执行放置
        BlockState newState = ((InfestedLilyPad) blockItem.getBlock()).defaultBlockState()
                .setValue(InfestedLilyPad.NATURAL_SPAWN, false);
        level.setBlock(placePos, newState, 3);
        sendInfestedPacketToClients((ServerLevel) level, targetPos, true);

        // 4. 播放放置音效
        SoundType soundType = newState.getSoundType(level, placePos, player);
        level.playSound(null, placePos, soundType.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

        // 5. 消耗物品（创造模式不消耗）
        if (!player.isCreative()) {
            stack.shrink(1);
        }

        // 6. 取消原事件，阻止原版放置逻辑
        event.setCanceled(true);
    }

    private static boolean isValidPlacement(Level level, BlockPos pos) {
        // 下方必须为静态水源
        BlockState below = level.getBlockState(pos.below());
        FluidState fluid = below.getFluidState();
        if (fluid.getType() != Fluids.WATER || !fluid.isSource()) return false;
        // 当前位置必须为空气
        return level.getBlockState(pos).isAir();
    }

    private static void sendInfestedPacketToClients(ServerLevel level, BlockPos pos, boolean add) {
        if (!level.isClientSide()) {
            if (add) {
                ModNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new InfestedSourcePacket.AddInfestedSourcePacket(pos));
            } else {
                ModNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new InfestedSourcePacket.RemoveInfestedSourcePacket(pos));
            }
        }
    }
}