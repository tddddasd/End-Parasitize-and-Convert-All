package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedDiamond;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedEmerald;
import org.tdddd.epca.impl.utils.IBeaconMixin;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin implements IBeaconMixin {
    private static final Map<BlockPos, Item> PAYMENT_MAP = new HashMap<>();
    private static final Map<BlockPos, Long> LAST_EFFECT_MAP = new HashMap<>();

    @Unique private BlockPos getPos() {
        return ((BlockEntity)(Object)this).getBlockPos();
    }

    @Override public Item getPaymentItem() {
        return PAYMENT_MAP.get(getPos());
    }

    @Override
    public void setPaymentItem(Item item) {
        BlockPos pos = getPos();
        if (item == null) {
            PAYMENT_MAP.remove(pos);
        } else {
            PAYMENT_MAP.put(pos, item);
        }
        BlockEntity self = (BlockEntity)(Object)this;
        Level level = self.getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(pos, self.getBlockState(), self.getBlockState(), 3);
        }
    }

    @Override public long getLastParasiteEffectTime() {
        return LAST_EFFECT_MAP.getOrDefault(getPos(), -1L);
    }

    @Override public void setLastParasiteEffectTime(long time) {
        LAST_EFFECT_MAP.put(getPos(), time);
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void onSetRemoved(CallbackInfo ci) {
        BlockPos pos = getPos();
        PAYMENT_MAP.remove(pos);
        LAST_EFFECT_MAP.remove(pos);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        BlockPos pos = getPos();
        Item payment = PAYMENT_MAP.get(pos);
        if (payment != null) {
            tag.putString("PaymentItem", BuiltInRegistries.ITEM.getKey(payment).toString());
        }
        tag.putLong("ParasiteEffectTime", LAST_EFFECT_MAP.getOrDefault(pos, -1L));
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        BlockPos pos = getPos();
        if (tag.contains("PaymentItem")) {
            ResourceLocation loc = new ResourceLocation(tag.getString("PaymentItem"));
            PAYMENT_MAP.put(pos, BuiltInRegistries.ITEM.get(loc));
        } else {
            PAYMENT_MAP.remove(pos);
        }
        LAST_EFFECT_MAP.put(pos, tag.getLong("ParasiteEffectTime"));
    }

    @Inject(method = "getUpdateTag", at = @At("RETURN"), cancellable = true)
    private void onGetUpdateTag(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        BlockPos pos = getPos();
        Item payment = PAYMENT_MAP.get(pos);
        if (payment != null) {
            tag.putString("PaymentItem", BuiltInRegistries.ITEM.getKey(payment).toString());
        } else {
            tag.remove("PaymentItem");
        }
        cir.setReturnValue(tag);
    }

    @Inject(
            method = "applyEffects(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/effect/MobEffect;Lnet/minecraft/world/effect/MobEffect;)V",
            at = @At("TAIL")
    )
    private static void onApplyEffects(Level level, BlockPos pos, int levels,
                                       @Nullable MobEffect primary, @Nullable MobEffect secondary,
                                       CallbackInfo ci) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BeaconBlockEntity beacon)) return;
        IBeaconMixin mixin = (IBeaconMixin) beacon;
        Item payment = mixin.getPaymentItem();
        if (!(payment instanceof InfestedDiamond) && !(payment instanceof InfestedEmerald)) {
            return;
        }

        int range = 10 + levels * 10;
        AABB aabb = new AABB(pos).inflate(range).expandTowards(0, level.getHeight(), 0);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb);

        if (entities instanceof Player player && NestLeaderManager.isNestLeader(player.getUUID())) {
            long now = level.getGameTime();
            long last = mixin.getLastParasiteEffectTime();
            if (last == -1 || now - last >= 600) {
                for (LivingEntity entity : entities) {
                    if (entity instanceof IParasite) {
                        if (payment instanceof InfestedEmerald) {
                            entity.addEffect(new MobEffectInstance(ModEffects.CAMOUFLAGE.get(), 300, 0, false, true));
                        } else if (payment instanceof InfestedDiamond) {
                            entity.addEffect(new MobEffectInstance(ModEffects.SOUL_PROTECTION.get(), 300, 2, false, true));
                        }
                    }
                }
                mixin.setLastParasiteEffectTime(now);
            }
        }

        for (LivingEntity entity : entities) {
            if (!(entity instanceof IParasite)) {
                entity.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 2400, 0, false, true));
            }
        }


        if (!(entities instanceof Player)) {
            long now = level.getGameTime();
            long last = mixin.getLastParasiteEffectTime();
            if (last == -1 || now - last >= 600) {
                for (LivingEntity entity : entities) {
                    if (entity instanceof IParasite) {
                        if (payment instanceof InfestedEmerald) {
                            entity.addEffect(new MobEffectInstance(ModEffects.CAMOUFLAGE.get(), 300, 0, false, true));
                        } else if (payment instanceof InfestedDiamond) {
                            entity.addEffect(new MobEffectInstance(ModEffects.SOUL_PROTECTION.get(), 300, 2, false, true));
                        }
                    }
                }
                mixin.setLastParasiteEffectTime(now);
            }
        }
    }
}