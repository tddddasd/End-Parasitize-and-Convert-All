package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.items.ILivingArmorBoxStorable;
import org.tdddd.epca.impl.overworld.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LivingArmorBox extends Item {
    private static final String TAG_STATE = "state";
    private static final String TAG_ADAPTATION_COUNT = "AdaptationCount";
    private static final String TAG_STORED_ITEMS = "StoredItems";
    private static final String TAG_BIOMASS = "Biomass";

    public static final int MAX_ADAPTATIONS_PER_PIECE = 9;
    public static final float ADAPTATION_REDUCTION_PER_STACK = 0.05f;
    private static final float MAX_REDUCTION = 0.45f;
    public static final int MAX_STORED_ITEMS = 8;
    public static final int MAX_BIOMASS = 50000;

    
    private static final String TOOLTIP_STORAGE_INFO = "item.epca.living_armor_box.tooltip.storage_info";
    private static final String TOOLTIP_STORAGE_SPACE = "item.epca.living_armor_box.tooltip.storage_space";
    private static final String TOOLTIP_STORED_ITEMS = "item.epca.living_armor_box.tooltip.stored_items";
    private static final String TOOLTIP_EMPTY = "item.epca.living_armor_box.tooltip.empty";
    private static final String TOOLTIP_MORE_ITEMS = "item.epca.living_armor_box.tooltip.more_items";
    private static final String TOOLTIP_USAGE = "item.epca.living_armor_box.tooltip.usage";
    private static final String TOOLTIP_USE_RIGHT_CLICK = "item.epca.living_armor_box.tooltip.use_right_click";
    private static final String TOOLTIP_USE_SNEAK_RIGHT_CLICK_WITH_ITEM = "item.epca.living_armor_box.tooltip.use_sneak_right_click_with_item";
    private static final String TOOLTIP_USE_SNEAK_RIGHT_CLICK_EMPTY = "item.epca.living_armor_box.tooltip.use_sneak_right_click_empty";
    private static final String TOOLTIP_ADAPTATION_LEVEL = "item.epca.living_armor_box.tooltip.adaptation_level";
    private static final String TOOLTIP_DAMAGE_REDUCTION = "item.epca.living_armor_box.tooltip.damage_reduction";
    private static final String TOOLTIP_CURRENT_STATE = "item.epca.living_armor_box.tooltip.current_state";
    private static final String TOOLTIP_STATE_EQUIPPED = "item.epca.living_armor_box.tooltip.state_equipped";
    private static final String TOOLTIP_STATE_UNEQUIPPED = "item.epca.living_armor_box.tooltip.state_unequipped";
    private static final String TOOLTIP_BIOMASS = "item.epca.living_armor_box.tooltip.biomass";
    private static final String TOOLTIP_BIOMASS_USAGE = "item.epca.living_armor_box.tooltip.biomass_usage";

    
    private static final int BIOMASS_THRESHOLD_HASTE = 10000;
    private static final int BIOMASS_THRESHOLD_SPEED = 20000;
    private static final int BIOMASS_THRESHOLD_STRENGTH_I = 30000;
    private static final int BIOMASS_THRESHOLD_DAMAGE_RESISTANCE = 40000;
    private static final int BIOMASS_THRESHOLD_STRENGTH_II = 47500;
    private static final int BIOMASS_THRESHOLD_SOUL_I = 47500;
    private static final int BIOMASS_THRESHOLD_SOUL_II = 49000;

    public LivingArmorBox(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 16;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        EnchantmentCategory category = enchantment.category;
        // 允许所有盔甲相关类别（含通用、头、胸、腿、足）以及可穿戴诅咒类
        return category == EnchantmentCategory.ARMOR ||
                category == EnchantmentCategory.ARMOR_HEAD ||
                category == EnchantmentCategory.ARMOR_CHEST ||
                category == EnchantmentCategory.ARMOR_LEGS ||
                category == EnchantmentCategory.ARMOR_FEET ||
                category == EnchantmentCategory.WEARABLE;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable(TOOLTIP_STORAGE_INFO).withStyle(ChatFormatting.DARK_GREEN));

        List<ItemStack> storedItems = getStoredItems(stack);
        int storedCount = storedItems.size();

        
        tooltip.add(Component.translatable(TOOLTIP_STORAGE_SPACE, storedCount, MAX_STORED_ITEMS)
                .withStyle(storedCount >= MAX_STORED_ITEMS ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GREEN));

        
        if (storedCount > 0) {
            tooltip.add(Component.translatable(TOOLTIP_STORED_ITEMS).withStyle(ChatFormatting.DARK_GREEN));
            for (int i = 0; i < Math.min(storedCount, 8); i++) {
                ItemStack storedItem = storedItems.get(i);
                tooltip.add(Component.literal("  " + (i + 1) + ". " + storedItem.getDisplayName().getString())
                        .withStyle(ChatFormatting.DARK_GREEN));
            }

            
            if (storedCount > 8) {
                tooltip.add(Component.translatable(TOOLTIP_MORE_ITEMS, storedCount - 8)
                        .withStyle(ChatFormatting.DARK_GREEN));
            }
        } else {
            tooltip.add(Component.translatable(TOOLTIP_EMPTY).withStyle(ChatFormatting.DARK_GREEN));
        }

        
        int biomass = getBiomass(stack);
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable(TOOLTIP_BIOMASS, biomass, MAX_BIOMASS)
                .withStyle(biomass >= MAX_BIOMASS ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GREEN));

        
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable(TOOLTIP_USAGE).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable(TOOLTIP_USE_RIGHT_CLICK).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable(TOOLTIP_USE_SNEAK_RIGHT_CLICK_WITH_ITEM).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable(TOOLTIP_USE_SNEAK_RIGHT_CLICK_EMPTY).withStyle(ChatFormatting.GREEN));
        
        tooltip.add(Component.translatable(TOOLTIP_BIOMASS_USAGE).withStyle(ChatFormatting.GREEN));

        
        int adaptationCount = getBoxAdaptationCount(stack);
        if (adaptationCount > 0) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable(TOOLTIP_ADAPTATION_LEVEL, adaptationCount).withStyle(ChatFormatting.DARK_GREEN));
            float damageReduction = getBoxDamageReduction(stack);
            tooltip.add(Component.translatable(TOOLTIP_DAMAGE_REDUCTION, String.format("%.1f", damageReduction * 100))
                    .withStyle(ChatFormatting.DARK_GREEN));
        }

        
        boolean isOpen = getState(stack);
        tooltip.add(Component.literal(""));
        String stateText = isOpen ? TOOLTIP_STATE_EQUIPPED : TOOLTIP_STATE_UNEQUIPPED;
        tooltip.add(Component.translatable(TOOLTIP_CURRENT_STATE, Component.translatable(stateText))
                .withStyle(isOpen ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack boxStack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            
            if (player.isShiftKeyDown()) {
                return InteractionResultHolder.success(boxStack);
            }

            boolean isOpen = getState(boxStack);

            if (!isOpen && isArmorEmpty(player)) {
                
                equipLivingArmorSet(player, boxStack);
                setState(boxStack, true);
                return InteractionResultHolder.success(boxStack);

            } else if (isOpen && hasLivingArmor(player)) {
                
                removeLivingArmor(player);
                setState(boxStack, false);
                return InteractionResultHolder.success(boxStack);
            } else {
                
                if (isOpen && !hasLivingArmor(player)) {
                    setState(boxStack, false);
                }
            }
        }

        return InteractionResultHolder.pass(boxStack);
    }

    
    public void handleLeftClickInGUI(Player player, ItemStack boxStack, ItemStack heldItem) {
        if (player.level().isClientSide()) {
            return;
        }

        if (heldItem.isEmpty()) {
            return;
        }

        int currentBiomass = getBiomass(boxStack);
        if (currentBiomass >= MAX_BIOMASS) {
            return; 
        }

        int biomassToAdd = 0;
        int itemsToConsume = 0;

        
        if (heldItem.getItem() instanceof InfestedFlesh) {
            
            biomassToAdd = 40;
            itemsToConsume = calculateConsumableItems(currentBiomass, biomassToAdd, heldItem.getCount());
        } else if (heldItem.getItem() instanceof DiseasedHeart) {
            
            biomassToAdd = 60;
            itemsToConsume = calculateConsumableItems(currentBiomass, biomassToAdd, heldItem.getCount());
        } else if (heldItem.getItem() instanceof ParasiteViscera) {
            
            biomassToAdd = 20;
            itemsToConsume = calculateConsumableItems(currentBiomass, biomassToAdd, heldItem.getCount());
        } else if (heldItem.isEdible()) {
            
            FoodProperties foodProperties = heldItem.getFoodProperties(player);
            if (foodProperties != null) {
                int nutrition = foodProperties.getNutrition();
                float saturation = foodProperties.getSaturationModifier();
                biomassToAdd = (int) Math.ceil((nutrition + saturation) * 2);
                itemsToConsume = calculateConsumableItems(currentBiomass, biomassToAdd, heldItem.getCount());
            }
        }

        if (itemsToConsume > 0) {
            
            heldItem.shrink(itemsToConsume);
            int newBiomass = Math.min(currentBiomass + (biomassToAdd * itemsToConsume), MAX_BIOMASS);
            setBiomass(boxStack, newBiomass);
        }
    }

    
    private int calculateConsumableItems(int currentBiomass, int biomassPerItem, int availableItems) {
        if (biomassPerItem <= 0) {
            return 0;
        }

        int remainingCapacity = MAX_BIOMASS - currentBiomass;
        int maxItemsByCapacity = (int) Math.ceil((double) remainingCapacity / biomassPerItem);

        return Math.min(availableItems, maxItemsByCapacity);
    }

    
    public void addBiomass(ItemStack boxStack, int amount) {
        int currentBiomass = getBiomass(boxStack);
        setBiomass(boxStack, currentBiomass + amount);
    }

    
    public boolean storeItem(ItemStack boxStack, ItemStack itemToStore) {
        
        if (!(itemToStore.getItem() instanceof ILivingArmorBoxStorable)) {
            return false;
        }

        List<ItemStack> storedItems = getStoredItems(boxStack);

        
        if (storedItems.size() >= MAX_STORED_ITEMS) {
            return false;
        }

        
        for (ItemStack storedItem : storedItems) {
            if (ItemStack.isSameItemSameTags(storedItem, itemToStore)) {
                return false;
            }
        }

        
        ItemStack storedCopy = itemToStore.copy();
        storedCopy.setCount(1);

        storedItems.add(storedCopy);
        setStoredItems(boxStack, storedItems);

        return true;
    }

    
    public ItemStack retrieveItem(ItemStack boxStack, int index) {
        List<ItemStack> storedItems = getStoredItems(boxStack);

        if (index < 0 || index >= storedItems.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack retrievedItem = storedItems.remove(index);
        setStoredItems(boxStack, storedItems);

        return retrievedItem;
    }

    
    public List<ItemStack> getStoredItems(ItemStack boxStack) {
        List<ItemStack> items = new ArrayList<>();

        if (!boxStack.hasTag()) {
            return items;
        }

        CompoundTag tag = boxStack.getTag();
        if (!tag.contains(TAG_STORED_ITEMS)) {
            return items;
        }

        ListTag itemsList = tag.getList(TAG_STORED_ITEMS, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompound(i);
            ItemStack itemStack = ItemStack.of(itemTag);
            if (!itemStack.isEmpty()) {
                items.add(itemStack);
            }
        }

        return items;
    }

    
    private void setStoredItems(ItemStack boxStack, List<ItemStack> items) {
        CompoundTag tag = boxStack.getOrCreateTag();
        ListTag itemsList = new ListTag();

        for (ItemStack item : items) {
            CompoundTag itemTag = new CompoundTag();
            item.save(itemTag);
            itemsList.add(itemTag);
        }

        tag.put(TAG_STORED_ITEMS, itemsList);
    }

    
    public int getStoredItemCount(ItemStack boxStack) {
        return getStoredItems(boxStack).size();
    }

    
    private boolean isArmorEmpty(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!player.getItemBySlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    
    private boolean hasLivingArmor(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armorStack = player.getItemBySlot(slot);
            if (isLivingArmor(armorStack)) {
                return true;
            }
        }
        return false;
    }

    
    private boolean isLivingArmor(ItemStack stack) {
        return stack.getItem() instanceof LivingArmorItem;
    }

    private void equipLivingArmorSet(Player player, ItemStack boxStack) {
        int boxAdaptationCount = getBoxAdaptationCount(boxStack);
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(boxStack);

        ItemStack helmet = new ItemStack(ModItems.LIVING_HELMET.get());
        syncAdaptationFromBox(helmet, boxAdaptationCount);
        EnchantmentHelper.setEnchantments(enchantments, helmet);
        helmet.enchant(Enchantments.BINDING_CURSE, 1);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);

        ItemStack chestplate = new ItemStack(ModItems.LIVING_CHESTPLATE.get());
        syncAdaptationFromBox(chestplate, boxAdaptationCount);
        EnchantmentHelper.setEnchantments(enchantments, chestplate);
        chestplate.enchant(Enchantments.BINDING_CURSE, 1);
        player.setItemSlot(EquipmentSlot.CHEST, chestplate);

        ItemStack leggings = new ItemStack(ModItems.LIVING_LEGGINGS.get());
        syncAdaptationFromBox(leggings, boxAdaptationCount);
        EnchantmentHelper.setEnchantments(enchantments, leggings);
        leggings.enchant(Enchantments.BINDING_CURSE, 1);
        player.setItemSlot(EquipmentSlot.LEGS, leggings);

        ItemStack boots = new ItemStack(ModItems.LIVING_BOOTS.get());
        syncAdaptationFromBox(boots, boxAdaptationCount);
        EnchantmentHelper.setEnchantments(enchantments, boots);
        boots.enchant(Enchantments.BINDING_CURSE, 1);
        player.setItemSlot(EquipmentSlot.FEET, boots);
    }

    
    private void removeLivingArmor(Player player) {
        int totalAdaptation = 0;
        int livingArmorCount = 0;

        
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armorStack = player.getItemBySlot(slot);
            if (isLivingArmor(armorStack)) {
                totalAdaptation += getArmorAdaptationCount(armorStack);
                livingArmorCount++;
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
        }

        
        if (livingArmorCount > 0) {
            int averageAdaptation = totalAdaptation / livingArmorCount;
            
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof LivingArmorBox) {
                    setBoxAdaptationCount(stack, averageAdaptation);
                    break;
                }
            }
        }
    }

    
    private void syncAdaptationFromBox(ItemStack armorStack, int adaptationCount) {
        armorStack.getOrCreateTag().putInt(TAG_ADAPTATION_COUNT, Math.min(adaptationCount, MAX_ADAPTATIONS_PER_PIECE));
    }

    
    private int getArmorAdaptationCount(ItemStack armorStack) {
        if (!armorStack.hasTag()) {
            return 0;
        }
        return armorStack.getTag().getInt(TAG_ADAPTATION_COUNT);
    }

    
    public int getBoxAdaptationCount(ItemStack boxStack) {
        if (!boxStack.hasTag()) {
            return 0;
        }
        return boxStack.getTag().getInt(TAG_ADAPTATION_COUNT);
    }

    
    public void setBoxAdaptationCount(ItemStack boxStack, int count) {
        boxStack.getOrCreateTag().putInt(TAG_ADAPTATION_COUNT, Math.min(count, MAX_ADAPTATIONS_PER_PIECE * 4));
    }

    
    public void addAdaptationToBox(ItemStack boxStack) {
        int currentCount = getBoxAdaptationCount(boxStack);
        if (currentCount < MAX_ADAPTATIONS_PER_PIECE * 4) {
            setBoxAdaptationCount(boxStack, currentCount + 1);
        }
    }

    
    public float getBoxDamageReduction(ItemStack boxStack) {
        int adaptationCount = getBoxAdaptationCount(boxStack);
        return Math.min(adaptationCount * ADAPTATION_REDUCTION_PER_STACK, MAX_REDUCTION);
    }

    
    public boolean getState(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(TAG_STATE);
    }

    
    public void setState(ItemStack stack, boolean state) {
        stack.getOrCreateTag().putBoolean(TAG_STATE, state);
    }

    
    public int getBiomass(ItemStack boxStack) {
        if (boxStack == null || boxStack.isEmpty()) {
            return 0;
        }
        CompoundTag tag = boxStack.getTag();
        if (tag == null || !tag.contains(TAG_BIOMASS)) {
            return 0; 
        }
        return tag.getInt(TAG_BIOMASS);
    }

    
    public void setBiomass(ItemStack boxStack, int biomass) {
        if (boxStack == null || boxStack.isEmpty()) {
            return;
        }
        CompoundTag tag = boxStack.getOrCreateTag();
        tag.putInt(TAG_BIOMASS, Math.min(Math.max(biomass, 0), MAX_BIOMASS));
    }

    
    public int consumeBiomass(ItemStack boxStack, int amount) {
        if (boxStack == null || boxStack.isEmpty() || amount <= 0) {
            return 0;
        }

        int currentBiomass = getBiomass(boxStack);
        if (currentBiomass < amount) {
            return 0; 
        }

        int newBiomass = currentBiomass - amount;
        setBiomass(boxStack, newBiomass);
        return amount;
    }

    @Override
    public boolean isFireResistant() {
        
        return hasNetheriteModuleI(this.getDefaultInstance());
    }

    @Override
    public boolean canBeHurtBy(DamageSource damageSource) {
        
        if (hasNetheriteModuleI(this.getDefaultInstance())) {
            if (damageSource.is(DamageTypeTags.IS_FIRE)) {
                return false;
            }
        }
        return super.canBeHurtBy(damageSource);
    }

    
    private boolean hasNetheriteModuleI(ItemStack boxStack) {
        List<ItemStack> storedItems = getStoredItems(boxStack);

        for (ItemStack storedItem : storedItems) {
            if (storedItem.getItem() instanceof NetheriteModuleI) {
                return true;
            }
        }

        return false;
    }

    

    
    private static boolean isWearingFullLivingArmor(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof LivingArmorItem)) {
                return false;
            }
        }
        return true;
    }

    
    private static ItemStack findLivingArmorBox(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof LivingArmorBox) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    
    public static void applyBiomassEffects(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        
        if (!isWearingFullLivingArmor(player)) {
            return;
        }

        
        ItemStack boxStack = findLivingArmorBox(player);
        if (boxStack.isEmpty()) {
            return;
        }

        
        LivingArmorBox boxItem = (LivingArmorBox) boxStack.getItem();
        if (!boxItem.getState(boxStack)) {
            return;
        }

        int biomass = boxItem.getBiomass(boxStack);
        int duration = 100; 

        
        if (biomass >= BIOMASS_THRESHOLD_HASTE) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 0, false, false, true));
        }
        
        if (biomass >= BIOMASS_THRESHOLD_SPEED) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, false, true));
        }
        
        if (biomass >= BIOMASS_THRESHOLD_DAMAGE_RESISTANCE) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false, true));
        }
        
        if (biomass >= BIOMASS_THRESHOLD_STRENGTH_II) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, false, true));
        } else if (biomass >= BIOMASS_THRESHOLD_STRENGTH_I) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, false, true));
        }

        if (biomass >= BIOMASS_THRESHOLD_SOUL_II) {
            player.addEffect(new MobEffectInstance(ModEffects.SOUL_PROTECTION.get(), duration, 1, false, false, true));
        } else if (biomass >= BIOMASS_THRESHOLD_SOUL_I) {
            player.addEffect(new MobEffectInstance(ModEffects.SOUL_PROTECTION.get(), duration, 0, false, false, true));
        }
    }
}