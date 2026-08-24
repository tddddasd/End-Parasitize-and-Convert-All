package org.tdddd.epca.impl.events.modlue;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.events.KeyInputHandler;
import org.tdddd.epca.impl.overworld.registry.items.item.FlightModuleI;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class FlightModuleEventHandler {
    private static final float HORIZONTAL_SPEED = 0.2F;
    private static final float HOVER_HORIZONTAL_SPEED = 0.1F;
    private static final int MAX_HEIGHT_ABOVE_GROUND = 20;
    private static final int BIOMASS_COST_PER_SECOND = 1;
    
    private static final float ASCEND_ACCELERATION = 0.05F;
    private static final float MAX_ASCEND_SPEED = 0.5F;

    
    private static final Map<UUID, Integer> flightTimers = new HashMap<>();
    private static final Map<UUID, Integer> biomassConsumeTimers = new HashMap<>();
    private static final Map<UUID, Float> playerAscendSpeed = new HashMap<>();
    private static final Map<UUID, Boolean> playerIsFlying = new HashMap<>();
    private static final Map<UUID, Boolean> lastSpacePressed = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        UUID playerId = player.getUUID();

        
        if (player.level().isClientSide()) {
            return;
        }

        
        ItemStack boxStack = findLivingArmorBox(player);
        if (boxStack.isEmpty()) {
            if (playerIsFlying.getOrDefault(playerId, false)) {
                disableFlight(player);
                resetPlayerFlightState(playerId);
            }
            return;
        }

        LivingArmorBox boxItem = (LivingArmorBox) boxStack.getItem();

        
        ensureBiomassInitialized(boxItem, boxStack);

        
        boolean canFly = canFly(player, boxStack, boxItem);

        
        if (playerIsFlying.getOrDefault(playerId, false) && !canFly) {
            disableFlight(player);
            resetPlayerFlightState(playerId);
            playerIsFlying.put(playerId, false);
            return;
        }

        
        if (!canFly) {
            if (playerIsFlying.getOrDefault(playerId, false)) {
                disableFlight(player);
                playerIsFlying.put(playerId, false);
            }
            resetPlayerFlightState(playerId);
            return;
        }

        
        boolean isJumping = KeyInputHandler.isSpacePressed(playerId);
        boolean isSneaking = KeyInputHandler.isShiftPressed(playerId);
        
        boolean wasSpacePressed = lastSpacePressed.getOrDefault(playerId, false);
        boolean spaceJustPressed = isJumping && !wasSpacePressed;
        lastSpacePressed.put(playerId, isJumping);

        
        if (isJumping) {
            
            if (spaceJustPressed) {
                enableFlight(player);
                playerIsFlying.put(playerId, true);
                
                playerAscendSpeed.put(playerId, 0.0F);
            }

            
            int flightTime = flightTimers.getOrDefault(playerId, 0) + 1;
            flightTimers.put(playerId, flightTime);

            
            int biomassTimer = biomassConsumeTimers.getOrDefault(playerId, 0) + 1;
            biomassConsumeTimers.put(playerId, biomassTimer);

            
            player.fallDistance = 0.0F;

            
            int heightAboveGround = getHeightAboveGround(player);
            boolean canAscend = heightAboveGround < MAX_HEIGHT_ABOVE_GROUND;

            
            if (isSneaking) {
                
                applyHoverMovement(player);
                applyHorizontalMovement(player, HOVER_HORIZONTAL_SPEED);
                
                playerAscendSpeed.put(playerId, 0.0F);
            }
            
            else {
                
                float ascendSpeed = playerAscendSpeed.getOrDefault(playerId, 0.0F);

                
                if (canAscend) {
                    
                    ascendSpeed = Math.min(ascendSpeed + ASCEND_ACCELERATION, MAX_ASCEND_SPEED);
                    playerAscendSpeed.put(playerId, ascendSpeed);

                    
                    applyAscendMovement(player, ascendSpeed);
                } else {
                    
                    ascendSpeed = 0;
                    playerAscendSpeed.put(playerId, ascendSpeed);
                    
                    applyHoverMovement(player);
                }

                
                applyHorizontalMovement(player, HORIZONTAL_SPEED);

                
                if (player.isFallFlying()) {
                    applyElytraBoost(player);
                }
            }

            
            if (biomassTimer >= 20) { 
                biomassConsumeTimers.put(playerId, 0);

                
                int currentBiomass = boxItem.getBiomass(boxStack);

                if (currentBiomass >= BIOMASS_COST_PER_SECOND) {
                    
                    int newBiomass = currentBiomass - BIOMASS_COST_PER_SECOND;
                    boxItem.setBiomass(boxStack, newBiomass);

                    
                    if (newBiomass <= 0) {
                        disableFlight(player);
                        resetPlayerFlightState(playerId);
                        playerIsFlying.put(playerId, false);
                    }
                } else {
                    disableFlight(player);
                    resetPlayerFlightState(playerId);
                    playerIsFlying.put(playerId, false);
                }
            }

        } else {
            
            if (playerIsFlying.getOrDefault(playerId, false)) {
                disableFlight(player);
                resetPlayerFlightState(playerId);
                playerIsFlying.put(playerId, false);
            }
        }
    }

    
    private static void enableFlight(Player player) {
        player.setNoGravity(true);
        
        player.setDeltaMovement(
                player.getDeltaMovement().x(),
                0.1F,  
                player.getDeltaMovement().z()
        );
    }

    
    private static void disableFlight(Player player) {
        player.setNoGravity(false);
    }

    
    private static void applyHoverMovement(Player player) {
        
        player.setDeltaMovement(
                player.getDeltaMovement().x() * 0.5,
                0, 
                player.getDeltaMovement().z() * 0.5
        );
    }
    private static final Map<UUID, Boolean> playerWasFlying = new HashMap<>();
    
    private static final Map<UUID, Boolean> spaceKeyStates = new HashMap<>();
    private static final Map<UUID, Boolean> shiftKeyStates = new HashMap<>();

    
    private static void applyAscendMovement(Player player, float ascendSpeed) {
        
        player.setDeltaMovement(
                player.getDeltaMovement().x(),
                ascendSpeed, 
                player.getDeltaMovement().z()
        );
    }

    
    private static boolean canFly(Player player, ItemStack boxStack, LivingArmorBox boxItem) {
        
        if (!isWearingFullLivingArmor(player)) {
            return false;
        }

        
        if (!boxItem.getState(boxStack)) {
            return false;
        }

        
        if (!hasFlightModuleI(boxStack)) {
            return false;
        }

        
        int biomass = boxItem.getBiomass(boxStack);
        if (biomass <= 0) {
            return false;
        }

        return true;
    }

    
    private static void ensureBiomassInitialized(LivingArmorBox boxItem, ItemStack boxStack) {
        
        if (!boxStack.hasTag() || !boxStack.getTag().contains("Biomass")) {
            
            boxItem.setBiomass(boxStack, 0);
        }
    }

    
    private static void applyHorizontalMovement(Player player, float speed) {
        float yRotRad = player.getYRot() * ((float)Math.PI / 180F);
        float moveX = 0;
        float moveZ = 0;

        
        float xxa = player.xxa;
        float zza = player.zza;

        if (zza > 0) { 
            moveX -= Mth.sin(yRotRad) * speed;
            moveZ += Mth.cos(yRotRad) * speed;
        } else if (zza < 0) { 
            moveX += Mth.sin(yRotRad) * speed;
            moveZ -= Mth.cos(yRotRad) * speed;
        }

        if (xxa > 0) { 
            moveX += Mth.cos(yRotRad) * speed;
            moveZ += Mth.sin(yRotRad) * speed;
        } else if (xxa < 0) { 
            moveX -= Mth.cos(yRotRad) * speed;
            moveZ -= Mth.sin(yRotRad) * speed;
        }

        
        player.setDeltaMovement(
                player.getDeltaMovement().x() * 0.5 + moveX,
                player.getDeltaMovement().y(),
                player.getDeltaMovement().z() * 0.5 + moveZ
        );
    }

    
    private static void applyElytraBoost(Player player) {
        float yawRad = player.getYRot() * ((float)Math.PI / 180F);
        float pitchRad = player.getXRot() * ((float)Math.PI / 180F);

        
        float forwardX = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        float forwardZ = Mth.cos(yawRad) * Mth.cos(pitchRad);
        float forwardY = -Mth.sin(pitchRad);

        
        player.setDeltaMovement(
                player.getDeltaMovement().x() + forwardX * 0.1F,
                player.getDeltaMovement().y() + forwardY * 0.1F,
                player.getDeltaMovement().z() + forwardZ * 0.1F
        );
    }

    
    private static void resetPlayerFlightState(UUID playerId) {
        flightTimers.remove(playerId);
        biomassConsumeTimers.remove(playerId);
        playerAscendSpeed.remove(playerId);
    }

    
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            
            ItemStack boxStack = findLivingArmorBox(player);
            if (!boxStack.isEmpty()) {
                LivingArmorBox boxItem = (LivingArmorBox) boxStack.getItem();
                if (isWearingFullLivingArmor(player) &&
                        boxItem.getState(boxStack) &&
                        hasFlightModuleI(boxStack)) {
                    
                    event.setCanceled(true);
                }
            }
        }
    }

    
    private static int getHeightAboveGround(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockPos groundPos = playerPos;

        
        int minY = player.level().getMinBuildHeight();
        int searchDepth = 0;

        while (groundPos.getY() > minY && searchDepth < 100) {
            if (!player.level().isEmptyBlock(groundPos)) {
                break;
            }
            groundPos = groundPos.below();
            searchDepth++;
        }

        return Math.max(0, playerPos.getY() - groundPos.getY());
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

    
    private static boolean hasFlightModuleI(ItemStack boxStack) {
        if (!(boxStack.getItem() instanceof LivingArmorBox)) {
            return false;
        }

        LivingArmorBox box = (LivingArmorBox) boxStack.getItem();
        List<ItemStack> storedItems = box.getStoredItems(boxStack);

        for (ItemStack storedItem : storedItems) {
            if (storedItem.getItem() instanceof FlightModuleI) {
                return true;
            }
        }

        return false;
    }

    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        spaceKeyStates.remove(playerId);
        shiftKeyStates.remove(playerId);
        KeyInputHandler.removePlayer(playerId);
        resetPlayerFlightState(playerId);
        playerWasFlying.remove(playerId);
        playerIsFlying.remove(playerId);

        
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            player.setNoGravity(false);
        }
    }
}