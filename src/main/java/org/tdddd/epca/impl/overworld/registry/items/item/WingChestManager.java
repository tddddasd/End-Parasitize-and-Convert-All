package org.tdddd.epca.impl.overworld.registry.items.item;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class WingChestManager {
    public static final Map<UUID, WingType> activeWingTypes = new HashMap<>();
    private static final Map<UUID, Boolean> wasFlightEnabledByOtherMod = new HashMap<>();

    public static void init() {
        // 26.1.2: 该类自带 @EventBusSubscriber，FML 已自动注册；重复注册会让监听器执行两次，故移除。
    }

    public static void registerWingPlayer(Player player, WingType type) {
        
        if (!activeWingTypes.containsKey(player.getUUID())) {
            wasFlightEnabledByOtherMod.put(player.getUUID(), player.getAbilities().mayfly);
        }
        activeWingTypes.put(player.getUUID(), type);
        enableFlight(player, type.getFlightSpeed());
    }

    public static void unregisterWingPlayer(Player player) {
        UUID playerId = player.getUUID();
        WingType removedType = activeWingTypes.remove(playerId);
        if (removedType != null) {
            
            Boolean wasEnabledByOther = wasFlightEnabledByOtherMod.remove(playerId);
            if (wasEnabledByOther != null && !wasEnabledByOther) {
                disableFlight(player);
            } else {
                
                player.getAbilities().setFlyingSpeed(0.05f); 
                player.onUpdateAbilities();
            }
        }
    }

    private static void checkAndRegisterWingPlayer(Player player) {
        ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);

        if (chestItem.getItem() instanceof WingChestItem wingChest) {
            registerWingPlayer(player, wingChest.wingType);
        } else {
            unregisterWingPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        checkAndRegisterWingPlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        checkAndRegisterWingPlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        checkAndRegisterWingPlayer(player);
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player && event.getSlot() == EquipmentSlot.CHEST) {
            checkAndRegisterWingPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {

        Player player = event.getEntity();
        UUID playerId = player.getUUID();

        
        WingType wingType = activeWingTypes.get(playerId);
        if (wingType == null) {
            return; 
        }

        
        if (!player.isCreative() && !player.isSpectator()) {
            
            enableFlight(player, wingType.getFlightSpeed());

            
            if (player.getAbilities().flying && player.onGround()) {
                player.getAbilities().flying = false;
                if (!player.level().isClientSide()) {
                    player.onUpdateAbilities();
                }
            }
        } else {
            
            disableCustomFlight(player);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            UUID playerId = player.getUUID();
            if (activeWingTypes.containsKey(playerId) && !player.isCreative() && !player.isSpectator()) {
                event.setCanceled(true);
            }
        }
    }

    private static void enableFlight(Player player, float speed) {
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
        }
        player.getAbilities().setFlyingSpeed(speed);
        player.onUpdateAbilities();
    }

    private static void disableFlight(Player player) {
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private static void disableCustomFlight(Player player) {
        
    }

    public static void syncFlightState(Player player, boolean flying) {
        
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = flying;
            player.onUpdateAbilities();
        }
    }

    public enum WingType {
        SENTIENT(0.05f); 

        private final float flightSpeed;

        WingType(float flightSpeed) {
            this.flightSpeed = flightSpeed;
        }

        public float getFlightSpeed() {
            return flightSpeed;
        }
    }
}
