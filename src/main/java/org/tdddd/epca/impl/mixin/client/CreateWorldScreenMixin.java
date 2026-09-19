package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.client.DifficultyScreenHandler;

/**
 * 世界创建钩子：把「新建世界」界面上选中的额外难度写进 {@link DifficultyScreenHandler} 的待处理值，
 * 供世界真正创建时读取。
 *
 * <p><b>1.20.1 原版行为。</b> 在 {@code CreateWorldScreen#createNewWorld()} 的 HEAD 处
 * {@code DifficultyScreenHandler.setPendingDifficulty(DifficultyScreenHandler.getSelectedDifficulty())}。
 * 1.20.1 的目标方法是 {@code private void createNewWorld()}，处理器收尾参数是 {@code CallbackInfo}。
 *
 * <p><b>为什么被移除。</b> 26.1.2 里 {@code createNewWorld} 不再是 void：
 * {@code private boolean createNewWorld(LayeredRegistryAccess<RegistryLayer>,
 * LevelDataAndDimensions.WorldDataAndGenSettings, Optional<GameRules>)}，
 * 继续用 {@code CallbackInfo} 会因为「回调类型与目标返回类型不匹配」而注入失败。
 * 非 void 目标必须用 {@link CallbackInfoReturnable}，泛型实参是目标返回类型的包装类
 * （{@code boolean} → {@code Boolean}）。
 *
 * <p>处理器不捕获目标方法的任何参数（{@code @Inject} 允许只收尾 CallbackInfo*），
 * 行为的 1.20.1 语义（HEAD 处写一次待处理难度）逐字保留。
 *
 * <p>目标方法与描述符已用 {@code javap -p -s} 核对（{@code minecraft-patched-26.1.2.76.jar}）：
 * <pre>
 * private boolean createNewWorld(net.minecraft.core.LayeredRegistryAccess&lt;net.minecraft.server.RegistryLayer&gt;,
 *         net.minecraft.world.level.storage.LevelDataAndDimensions$WorldDataAndGenSettings,
 *         java.util.Optional&lt;net.minecraft.world.level.gamerules.GameRules&gt;);
 *   descriptor: (Lnet/minecraft/core/LayeredRegistryAccess;Lnet/minecraft/world/level/storage/LevelDataAndDimensions$WorldDataAndGenSettings;Ljava/util/Optional;)Z
 * </pre>
 * 写完整描述符也是为了避开同名前缀的 {@code createNewWorldDirectory(...)}（不过它是另一个方法名，
 * 真正需要消歧的是描述符里的 {@code Optional} 与泛型擦除后的参数顺序；写成描述符后不会有歧义）。
 * 该类由 {@code epca.mixins.json} 的 {@code "client"} 列表限定为仅客户端应用，因此不再需要
 * {@code @OnlyIn(Dist.CLIENT)}（与同一批次的 {@code LightningBoltRendererMixin} / {@code LightningBoltRenderStateMixin} 一致）。
 */
@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {

    @Inject(
            method = "createNewWorld(Lnet/minecraft/core/LayeredRegistryAccess;Lnet/minecraft/world/level/storage/LevelDataAndDimensions$WorldDataAndGenSettings;Ljava/util/Optional;)Z",
            at = @At("HEAD")
    )
    private void epca$setPendingDifficulty(CallbackInfoReturnable<Boolean> cir) {
        DifficultyScreenHandler.setPendingDifficulty(DifficultyScreenHandler.getSelectedDifficulty());
    }
}
