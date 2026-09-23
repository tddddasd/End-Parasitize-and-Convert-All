package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.render.ItemCorruptionRenderer;

/**
 * Recovers the {@link ItemStack} for an {@link ItemStackRenderState} while both are still available.
 *
 * <h2>Why this hook</h2>
 * 26.1.2's extract/submit split means the stack is known only while the model is resolved, and
 * {@code ItemStackRenderState} does not carry it (verified with {@code javap -p}: the fields are
 * {@code displayContext}, {@code activeLayerCount}, {@code animated}, {@code oversizedInGui},
 * {@code cachedModelBoundingBox}, {@code layers}). The 1.20.1 hook, {@code ItemRenderer#render}, had
 * the stack as a parameter and no longer exists.
 *
 * <p>{@code ItemModelResolver#appendItemLayers(ItemStackRenderState, ItemStack, ItemDisplayContext,
 * Level, ItemOwner, int)} is the single funnel that all three of {@code updateForLiving},
 * {@code updateForNonLiving} and {@code updateForTopItem} call, so <b>one</b> injection covers every
 * item rendering path: dropped items, item frames, the first-person hand, the GUI and the inventory.
 * Descriptor verified with {@code javap -s} on the 26.1.2 patched jar:</p>
 * <pre>
 *   (Lnet/minecraft/client/renderer/item/ItemStackRenderState;
 *    Lnet/minecraft/world/item/ItemStack;
 *    Lnet/minecraft/world/item/ItemDisplayContext;
 *    Lnet/minecraft/world/level/Level;
 *    Lnet/minecraft/world/entity/ItemOwner;I)V
 * </pre>
 *
 * <p>The TAIL is used because the stack has to be associated only once the model has been resolved;
 * anything the resolver does with the stack is irrelevant here.</p>
 */
@Mixin(ItemModelResolver.class)
public abstract class ItemLayerCaptureMixin {

    @Inject(method = "appendItemLayers(Lnet/minecraft/client/renderer/item/ItemStackRenderState;"
            + "Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/item/ItemDisplayContext;"
            + "Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/entity/ItemOwner;I)V",
            at = @At("TAIL"))
    private void epca$captureItemLayerStack(ItemStackRenderState renderState, ItemStack stack,
                                            ItemDisplayContext context, Level level,
                                            ItemOwner owner, int seed, CallbackInfo ci) {
        ItemCorruptionRenderer.capture(renderState, stack, context);
    }
}
