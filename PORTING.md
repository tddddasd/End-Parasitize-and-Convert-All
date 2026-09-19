# 26.1.2 NeoForge 移植说明

本目录是 **End-Parasitize and Convert All**（mod id: `epca`）的 Minecraft **26.1.2 / NeoForge 26.1.2.76** 移植工程。

## 当前状态

- [x] 工程骨架：ModDevGradle 2.0.140 + Java 25 工具链 + 版本目录 + `neoforge.mods.toml` 模板
- [x] **构建链路已验证打通**（用最小占位类实测）：`createMinecraftArtifacts` → `compileJava` → `processResources` → `jar` 全部成功，产出 `build/libs/yawning_neko_api-1.0.5.jar`
- [x] 1.20.1 的源码/资源已复制进来作为移植起点
- [ ] 源码适配 NeoForge 26.1.2 API（当前旧源码编译不过，属预期）
- [ ] 逐模组编译验证

## 构建

```bash
./gradlew build
```

### 已就位的前置条件

- **JDK 25**：已由 Gradle 自动下载到 `~/.jdks/openjdk-25.0.2`（26.1.2 要求 Java 25）。
- **NeoForge 构件**：已缓存到本机 Gradle 缓存
  （`~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.76/` 含 `userdev` 与 `sources`；
  `~/.gradle/caches/neoformruntime/` 含 `minecraft_26.1.2_client/server` 与补丁结果）。
  **构件已缓存，日常构建不再需要代理。**
- `gradle.properties` 里目前保留了下载用的本地代理（`systemProp.*.proxyHost=127.0.0.1:51081`）。
  代理关闭后如果构建报网络错误，删掉这 5 行即可（或当作离线构建的开关）。
- 参考用产物：`build/moddev/artifacts/minecraft-patched-26.1.2.76.jar` 与 `-sources.jar`（可查 vanilla API）、
  `~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.76/*-sources.jar`（NeoForge API）。

## 已实测确认的 26.1.2 API 变化

| 1.20.1（Forge） | 26.1.2（NeoForge） |
|---|---|
| `net.minecraft.resources.ResourceLocation` | **`net.minecraft.resources.Identifier`**（`Identifier.fromNamespaceAndPath(ns, path)`） |
| `@Mod` 无参构造 + `FMLJavaModLoadingContext` | **构造注入 `(IEventBus modEventBus, ModContainer modContainer)`** |
| `MinecraftForge.EVENT_BUS` | **`NeoForge.EVENT_BUS`**（`modEventBus` 仍用于注册表/加载期事件） |
| `@Mod.EventBusSubscriber` | `@EventBusSubscriber` |
| `LivingHurtEvent` | **`LivingIncomingDamageEvent`**，伤害改为 **`DamageContainer`** 管线（`getContainer()` / `addReductionModifier()` / `setAmount()` / `setInvulnerabilityTicks()`） |
| `LivingDamageEvent` | `LivingDamageEvent.Pre` / `LivingDamageEvent.Post` |
| `LivingEvent.LivingTickEvent` | **`EntityTickEvent.Pre` / `EntityTickEvent.Post`** |
| Forge Capability | **`net.neoforged.neoforge.attachment.AttachmentType` / `IAttachmentHolder`**（或 `RegisterCapabilitiesEvent`） |
| `ForgeRegistries` | `BuiltInRegistries`；`DeferredRegister` 仍在（新增 `DeferredRegister.Blocks` / `.DataComponents` 等入口） |
| `SimpleChannel` / `NetworkRegistry` | `RegisterPayloadHandlersEvent` + `CustomPayload` + `IPayloadContext`（详见 `neoforge-*-sources.jar`） |
| `ForgeConfigSpec` | `ModConfigSpec` |
| `META-INF/mods.toml` | `src/main/templates/META-INF/neoforge.mods.toml`（由 `generateModMetadata` 展开） |
| GeckoLib 4.x | **GeckoLib 5.5.2**（大版本 API 变更） |

> 移植时可直接用 `gradlew compileJava` 的报错逐条驱动（编译器已能给出 26.1.2 的真实符号错误）。
