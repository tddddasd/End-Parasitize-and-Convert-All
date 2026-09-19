package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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

/**
 * 26.1.2 迁移记录（对 {@code minecraft-patched-26.1.2.76} 的 {@code BeaconBlockEntity} 源码核对过）：
 * <ul>
 *   <li>{@code load(CompoundTag)} → {@code loadAdditional(ValueInput)}；
 *       {@code saveAdditional(CompoundTag)} → {@code saveAdditional(ValueOutput)}。
 *       存档字段名保持 {@code PaymentItem}（字符串 id）与 {@code ParasiteEffectTime}（long）。</li>
 *   <li>{@code getUpdateTag()} → {@code getUpdateTag(HolderLookup.Provider)}，返回值仍是 {@code CompoundTag}。</li>
 *   <li>{@code applyEffects(Level, BlockPos, int, MobEffect, MobEffect)} →
 *       {@code applyEffects(Level, BlockPos, int, Holder&lt;MobEffect&gt;, Holder&lt;MobEffect&gt;)}
 *       且方法在 26.1.2 是 {@code private static}（1.20.1 是 {@code protected static}）。</li>
 *   <li>原方法里 {@code List<LivingEntity> entities instanceof Player} 这类判断恒为 false
 *       （List 永远不可能是 Player），属于 1.20.1 就存在的死代码；这里按“只保留真正会执行的分支”
 *       重写，可观察行为与 1.20.1 一致：向范围内 {@link IParasite} 施加（巢穴领袖时）伪装/灵魂保护，
 *       向范围内非 {@link IParasite} 生物施加 COTH。</li>
 * </ul>
 */
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
        if (level != null && !level.isClientSide()) {
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
    private void onSave(ValueOutput output, CallbackInfo ci) {
        BlockPos pos = getPos();
        Item payment = PAYMENT_MAP.get(pos);
        if (payment != null) {
            Identifier key = BuiltInRegistries.ITEM.getKey(payment);
            if (key != null) {
                output.putString("PaymentItem", key.toString());
            }
        }
        output.putLong("ParasiteEffectTime", LAST_EFFECT_MAP.getOrDefault(pos, -1L));
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void onLoad(ValueInput input, CallbackInfo ci) {
        BlockPos pos = getPos();
        String paymentId = input.getStringOr("PaymentItem", "");
        if (!paymentId.isEmpty()) {
            Identifier loc = Identifier.tryParse(paymentId);
            Item item = loc == null ? null : BuiltInRegistries.ITEM.getValue(loc);
            if (item != null) {
                PAYMENT_MAP.put(pos, item);
            } else {
                PAYMENT_MAP.remove(pos);
            }
        } else {
            PAYMENT_MAP.remove(pos);
        }
        LAST_EFFECT_MAP.put(pos, input.getLongOr("ParasiteEffectTime", -1L));
    }

    @Inject(method = "getUpdateTag", at = @At("RETURN"), cancellable = true)
    private void onGetUpdateTag(net.minecraft.core.HolderLookup.Provider registries,
                                CallbackInfoReturnable<net.minecraft.nbt.CompoundTag> cir) {
        net.minecraft.nbt.CompoundTag tag = cir.getReturnValue();
        BlockPos pos = getPos();
        Item payment = PAYMENT_MAP.get(pos);
        if (payment != null) {
            Identifier key = BuiltInRegistries.ITEM.getKey(payment);
            if (key != null) {
                tag.putString("PaymentItem", key.toString());
            }
        } else {
            tag.remove("PaymentItem");
        }
        cir.setReturnValue(tag);
    }

    @Inject(
            method = "applyEffects(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)V",
            at = @At("TAIL")
    )
    private static void onApplyEffects(Level level, BlockPos pos, int levels,
                                       @Nullable Holder<MobEffect> primary, @Nullable Holder<MobEffect> secondary,
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

        long now = level.getGameTime();
        long last = mixin.getLastParasiteEffectTime();
        if (last == -1 || now - last >= 600) {
            for (LivingEntity entity : entities) {
                if (entity instanceof IParasite) {
                    if (payment instanceof InfestedEmerald) {
                        entity.addEffect(new MobEffectInstance(ModEffects.CAMOUFLAGE, 300, 0, false, true));
                    } else if (payment instanceof InfestedDiamond) {
                        entity.addEffect(new MobEffectInstance(ModEffects.SOUL_PROTECTION, 300, 2, false, true));
                    }
                }
            }
            mixin.setLastParasiteEffectTime(now);
        }

        for (LivingEntity entity : entities) {
            if (!(entity instanceof IParasite)) {
                entity.addEffect(new MobEffectInstance(ModEffects.COTH, 2400, 0, false, true));
            }
        }
    }
}
