package org.tdddd.epca.impl.overworld.registry.blocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.blocks.block.SwallowCyst;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.gui.menus.SwallowCystMenu;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SwallowCystBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            
            if (!isInventoryEmpty()) {
                emptyTicks = 0;
            }
        }
    };
    private LazyOptional<IItemHandler> handler = LazyOptional.of(() -> inventory);

    private int emptyTicks = 0;          
    private int absorbCooldown = 0;      
    private int damageCooldown = 0;       
    private int consumeCooldown = 0; 

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SwallowCystBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SWALLOW_CYST.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) level;

        boolean isLiving = state.getValue(SwallowCyst.LIVING);

        
        if (isLiving) {
            
            if (consumeCooldown <= 0) {
                consumeAndEvolve(serverLevel);
                consumeCooldown = 4; 
            } else {
                consumeCooldown--;
            }

            
            absorbItems(serverLevel, pos);

            
            damageEntitiesOnTop(serverLevel, pos);

            
            if (isInventoryEmpty()) {
                emptyTicks++;
                if (emptyTicks >= 600) {
                    becomeDead(serverLevel, pos);
                }
            } else {
                emptyTicks = 0;
            }
        }
    }

    private void consumeAndEvolve(ServerLevel level) {
        
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
                
                EvolutionManager evolutionManager = EvolutionManager.forDimension(level);
                evolutionManager.addPoints(2);
                setChanged();
                return; 
            }
        }
    }

    private void absorbItems(ServerLevel level, BlockPos pos) {
        if (absorbCooldown > 0) {
            absorbCooldown--;
            return;
        }
        absorbCooldown = 8; 

        
        AABB area = new AABB(pos.getX(), pos.getY() + 0.5, pos.getZ(),
                pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = item.getItem();
            ItemStack remaining = insertItem(stack);
            if (remaining.isEmpty()) {
                item.discard();
            } else if (remaining.getCount() != stack.getCount()) {
                item.setItem(remaining);
            }
            if (!remaining.isEmpty() && remaining.getCount() == stack.getCount()) {
                continue; 
            }
            break; 
        }
    }

    private ItemStack insertItem(ItemStack stack) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                int limit = Math.min(stack.getCount(), inventory.getSlotLimit(i));
                inventory.setStackInSlot(i, stack.split(limit));
                setChanged();
                return stack;
            } else if (ItemStack.isSameItemSameTags(slotStack, stack)) {
                int space = inventory.getSlotLimit(i) - slotStack.getCount();
                if (space > 0) {
                    int move = Math.min(stack.getCount(), space);
                    slotStack.grow(move);
                    stack.shrink(move);
                    setChanged();
                    if (stack.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    private void damageEntitiesOnTop(ServerLevel level, BlockPos pos) {
        if (damageCooldown > 0) {
            damageCooldown--;
            return;
        }
        damageCooldown = 20; 

        AABB area = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(),
                pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, area)) {
            if (entity instanceof LivingEntity living && !IParasite.isParasiteByTagOrInterface(living)) {
                if (level.random.nextFloat() < 0.05f) { 
                    living.hurt(living.damageSources().generic(), 1.0f);
                    living.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 300, 0));
                    level.playSound(null, pos, SoundEvents.PLAYER_HURT, SoundSource.HOSTILE, 0.5f, 1.0f);
                }
            }
        }
    }

    private void becomeDead(ServerLevel level, BlockPos pos) {
        BlockState newState = level.getBlockState(pos).setValue(SwallowCyst.LIVING, false);
        level.setBlock(pos, newState, 3);
        
    }

    private boolean isInventoryEmpty() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    
    public void onPlayerTake(Player player, ItemStack takenStack) {
        BlockState state = level.getBlockState(worldPosition);
        boolean isLiving = state.getValue(SwallowCyst.LIVING);
        if (!isLiving) return;
        
        
        
        if (level.random.nextFloat() < 0.1f) {
            player.hurt(player.damageSources().generic(), 1.0f);
            player.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 300, 0));
            level.playSound(null, worldPosition, SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.5f, 1.0f);
        }
    }

    
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.epca.swallow_cyst");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SwallowCystMenu(id, inv, this);
    }

    
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("EmptyTicks", emptyTicks);
        tag.putInt("AbsorbCooldown", absorbCooldown);
        tag.putInt("DamageCooldown", damageCooldown);
        tag.putInt("ConsumeCooldown", consumeCooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        emptyTicks = tag.getInt("EmptyTicks");
        absorbCooldown = tag.getInt("AbsorbCooldown");
        damageCooldown = tag.getInt("DamageCooldown");
        consumeCooldown = tag.getInt("ConsumeCooldown");
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 3, this::predicate));
    }

    private PlayState predicate(AnimationState<SwallowCystBlockEntity> state) {
        boolean living = getBlockState().getValue(SwallowCyst.LIVING);
        if (living) {
            state.setAnimation(RawAnimation.begin().thenLoop("idle"));
        } else {
            state.setAnimation(RawAnimation.begin().thenLoop("dead_idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }
}