package org.tdddd.epca.impl.datagen;

import com.google.gson.*;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEntities;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.data.DataProvider.saveStable;

/**
 * 统一管理所有自定义数据类型的 DataProvider。
 * 每种数据类型对应一个内部 Provider 类，实现 DataProvider 接口直接写入 JSON。
 *
 * 参考 InfectionCoreFramework 的 EvolutionDataProvider / BlockSpreadDataProvider 模式。
 */
public class CustomDataProviders {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // ═══════════════════ 工具方法 ═══════════════════
    private static Path dataPath(PackOutput out, String subfolder, String file) {
        return out.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(epca.MODID + "/" + subfolder + "/" + file + ".json");
    }

    /** 获取实体 → "modid:name" 字符串 */
    private static String regName(EntityType<?> type) {
        return Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(type)).toString();
    }

    // ═══════════════════ 1. Entity Conversions ═══════════════════

    public static class EntityConversionRule {
        public String from, to, fins_to, mozzie_to;
        public boolean small_entity_priority = true;
        public int priority = 0;
        public Map<String, Object> nbt_conditions;
    }

    public static class EntityConversionDataProvider implements DataProvider {
        private final PackOutput out;
        public EntityConversionDataProvider(PackOutput out) { this.out = out; }

        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            List<CompletableFuture<?>> tasks = new ArrayList<>();

            // 原版实体 → 受染实体
            conv(cache, tasks, "cow",      EntityType.COW,      ModEntities.INFESTED_COW,            true,  1);
            conv(cache, tasks, "chicken",  EntityType.CHICKEN,  ModEntities.INFESTED_CHICKEN,         false, 1);
            conv(cache, tasks, "pig",      EntityType.PIG,      ModEntities.INFESTED_PIG,             true,  1);
            conv(cache, tasks, "sheep",    EntityType.SHEEP,    ModEntities.INFESTED_SHEEP,           true,  1);
            conv(cache, tasks, "villager", EntityType.VILLAGER, ModEntities.INFESTED_VILLAGER,        true,  1);
            conv(cache, tasks, "zombie",   EntityType.ZOMBIE,   ModEntities.INFESTED_ZOMBIE,          true,  1);
            conv(cache, tasks, "husk",     EntityType.HUSK,     ModEntities.INFESTED_HUSK,            true,  1);
            conv(cache, tasks, "drowned",  EntityType.DROWNED,  ModEntities.INFESTED_DROWNED,         true,  1);
            conv(cache, tasks, "skeleton", EntityType.SKELETON, ModEntities.INFESTED_SKELETON,        true,  1);
            conv(cache, tasks, "fox",      EntityType.FOX,      ModEntities.INFESTED_FOX,             true,  1);
            conv(cache, tasks, "wolf",     EntityType.WOLF,     ModEntities.INFESTED_WOLF,            true,  1);
            conv(cache, tasks, "enderman", EntityType.ENDERMAN, ModEntities.INFESTED_ENDERMAN,        true,  1);
            conv(cache, tasks, "zombie_villager", EntityType.ZOMBIE_VILLAGER, ModEntities.INFESTED_ZOMBIE_VILLAGER, true, 1);
            conv(cache, tasks, "pillager", EntityType.PILLAGER, ModEntities.INFESTED_PILLAGER,        true,  1);
            conv(cache, tasks, "vindcator",EntityType.VINDICATOR,ModEntities.INFESTED_VINDICATOR,     true,  1);
            conv(cache, tasks, "endermite",EntityType.ENDERMITE, ModEntities.INFESTED_ENDERMITE,      true,  1);
            conv(cache, tasks, "silverfish",EntityType.SILVERFISH,ModEntities.INFESTED_SILVERFISH,    true,  1);
            conv(cache, tasks, "bat",EntityType.BAT ,ModEntities.INFESTED_BAT,    true,  1);

            // slime — nbt_conditions 区分 Size
            convNbt(cache, tasks, "slime_size0", EntityType.SLIME, ModEntities.INFESTED_SLIME_SIZE0, false, 1, Map.of("Size", 0));
            convNbt(cache, tasks, "slime_size1", EntityType.SLIME, ModEntities.INFESTED_SLIME_SIZE1, true,  1, Map.of("Size", 1));
            convNbt(cache, tasks, "slime_size3", EntityType.SLIME, ModEntities.INFESTED_SLIME_SIZE3, true,  1, Map.of("Size", 3));

            // baby 变体（from 相同，priority=0 供 NBT 条件匹配用）
            conv(cache, tasks, "cow_baby",      EntityType.COW,      ModEntities.INFESTED_COW,      true,  0);
            conv(cache, tasks, "chicken_baby",  EntityType.CHICKEN,  ModEntities.INFESTED_CHICKEN,  false, 0);
            conv(cache, tasks, "pig_baby",      EntityType.PIG,      ModEntities.INFESTED_PIG,      true,  0);
            conv(cache, tasks, "sheep_baby",    EntityType.SHEEP,    ModEntities.INFESTED_SHEEP,    true,  0);
            conv(cache, tasks, "fox_baby",      EntityType.FOX,      ModEntities.INFESTED_FOX,      true,  0);
            conv(cache, tasks, "wolf_baby",     EntityType.WOLF,     ModEntities.INFESTED_WOLF,     true,  0);
            conv(cache, tasks, "zombie_baby",   EntityType.ZOMBIE,   ModEntities.INFESTED_ZOMBIE,   true,  0);
            conv(cache, tasks, "husk_baby",     EntityType.HUSK,     ModEntities.INFESTED_HUSK,     true,  0);
            conv(cache, tasks, "drowned_baby",  EntityType.DROWNED,  ModEntities.INFESTED_DROWNED,  true,  0);

            // 特殊/boss 原版实体 (to=null, 免疫转换, 仅预留)
            convBoss(cache, tasks, "ender_dragon",      EntityType.ENDER_DRAGON,    true, 1);

            // mozzie 转化（to=null, mozzie_to=small_incomplete_form）
            convMozzie(cache, tasks, "creeper", EntityType.CREEPER);
            convMozzie(cache, tasks, "iron_golem", EntityType.IRON_GOLEM);
            convMozzie(cache, tasks, "guardian", EntityType.GUARDIAN);
            convMozzie(cache, tasks, "elder_guardian", EntityType.ELDER_GUARDIAN);
            convMozzie(cache, tasks, "warden", EntityType.WARDEN);
            convMozzie(cache, tasks, "magma_cube", EntityType.MAGMA_CUBE);
            convMozzie(cache, tasks, "vex", EntityType.VEX);
            convMozzie(cache, tasks, "allay", EntityType.ALLAY);
            convMozzie(cache, tasks, "blaze", EntityType.BLAZE);

            return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
        }

        // ——— 简写方法 ———

        /** from=EntityType, to=RegistryObject */
        private void conv(CachedOutput c, List<CompletableFuture<?>> tasks,
                          String file, EntityType<?> from, RegistryObject<? extends EntityType<?>> to,
                          boolean smallPrio, int prio) {
            tasks.add(write(c, file, regName(from), regName(to.get()), smallPrio, prio, null));
        }
        /** from=RegistryObject, to=RegistryObject (受染→walking_head) */
        private void conv(CachedOutput c, List<CompletableFuture<?>> tasks,
                          String file, RegistryObject<? extends EntityType<?>> from,
                          RegistryObject<? extends EntityType<?>> to,
                          boolean smallPrio, int prio) {
            tasks.add(write(c, file, regName(from.get()), regName(to.get()), smallPrio, prio, null));
        }
        /** 原版→受染但受染实体未注册 (boss 预留) */
        private void convBoss(CachedOutput c, List<CompletableFuture<?>> tasks,
                              String file, EntityType<?> from, boolean smallPrio, int prio) {
            tasks.add(write(c, file, regName(from), null, smallPrio, prio, null));
        }
        /** 带 nbt_conditions */
        private void convNbt(CachedOutput c, List<CompletableFuture<?>> tasks,
                             String file, EntityType<?> from, RegistryObject<? extends EntityType<?>> to,
                             boolean smallPrio, int prio, Map<String, Object> nbt) {
            tasks.add(write(c, file, regName(from), regName(to.get()), smallPrio, prio, nbt));
        }
        /** mozzie 专用: to=null, fins_to=null, mozzie_to=small_incomplete_form */
        private void convMozzie(CachedOutput c, List<CompletableFuture<?>> tasks,
                                String file, EntityType<?> from) {
            EntityConversionRule rule = new EntityConversionRule();
            rule.from = regName(from);
            rule.to = null;
            rule.fins_to = null;
            rule.mozzie_to = regName(ModEntities.SMALL_INCOMPLETE_FORM.get());
            rule.small_entity_priority = true;
            rule.priority = 0;
            tasks.add(saveStable(c, JsonParser.parseString(GSON.toJson(rule)),
                    dataPath(out, "entity_conversions", file)));
        }

        /** 核心写出方法 (to == fins_to == mozzie_to) */
        private CompletableFuture<?> write(CachedOutput c, String file,
                                            String from, String to, boolean smallPrio, int prio,
                                            Map<String, Object> nbt) {
            EntityConversionRule rule = new EntityConversionRule();
            rule.from = from;
            rule.to = to;
            rule.fins_to = to;
            rule.mozzie_to = to;
            rule.small_entity_priority = smallPrio;
            rule.priority = prio;
            if (nbt != null) rule.nbt_conditions = nbt;
            return saveStable(c, JsonParser.parseString(GSON.toJson(rule)),
                    dataPath(out, "entity_conversions", file));
        }

        @Override public String getName() { return "EPCA Entity Conversions"; }
    }

    // ═══════════════════ 3. Entity Carry ═══════════════════

    public static class EntityCarryData {
        public List<String> carryable;
        EntityCarryData(List<String> c) { this.carryable = c; }
    }

    public static class EntityCarryDataProvider implements DataProvider {
        private final PackOutput out;
        public EntityCarryDataProvider(PackOutput out) { this.out = out; }

        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            List<String> endermanCarry = names(ModEntities.RIPPER,
                    ModEntities.SMALL_INCOMPLETE_FORM, ModEntities.MEDIUM_INCOMPLETE_FORM,
                    ModEntities.INFESTED_ZOMBIE, ModEntities.WALKING_ZOMBIE_HEAD,
                    ModEntities.INFESTED_HUSK, ModEntities.WALKING_HUSK_HEAD,
                    ModEntities.INFESTED_DROWNED, ModEntities.WALKING_DROWNED_HEAD,
                    ModEntities.INFESTED_PILLAGER, ModEntities.WALKING_PILLAGER_HEAD,
                    ModEntities.INFESTED_VINDICATOR, ModEntities.WALKING_VINDICATOR_HEAD,
                    ModEntities.INFESTED_VILLAGER, ModEntities.WALKING_VILLAGER_HEAD,
                    ModEntities.INFESTED_ZOMBIE_VILLAGER, ModEntities.WALKING_ZOMBIE_VILLAGER_HEAD,
                    ModEntities.INFESTED_PIG, ModEntities.WALKING_PIG_HEAD,
                    ModEntities.INFESTED_SHEEP, ModEntities.WALKING_SHEEP_HEAD,
                    ModEntities.INFESTED_COW, ModEntities.WALKING_COW_HEAD,
                    ModEntities.INFESTED_WOLF, ModEntities.WALKING_WOLF_HEAD,
                    ModEntities.INFESTED_CHICKEN, ModEntities.WALKING_CHICKEN_HEAD,
                    ModEntities.INFESTED_SKELETON, ModEntities.WALKING_SKELETON_HEAD,
                    ModEntities.INFESTED_FOX, ModEntities.WALKING_FOX_HEAD,
                    ModEntities.LIGHT_CARRIER);

            List<String> endermanHeadCarry = names(ModEntities.RIPPER,
                    ModEntities.SMALL_INCOMPLETE_FORM, ModEntities.WALKING_ZOMBIE_HEAD,
                    ModEntities.WALKING_HUSK_HEAD, ModEntities.WALKING_DROWNED_HEAD,
                    ModEntities.WALKING_PILLAGER_HEAD, ModEntities.WALKING_VINDICATOR_HEAD,
                    ModEntities.WALKING_VILLAGER_HEAD, ModEntities.WALKING_ZOMBIE_VILLAGER_HEAD,
                    ModEntities.WALKING_PIG_HEAD, ModEntities.WALKING_SHEEP_HEAD,
                    ModEntities.WALKING_COW_HEAD, ModEntities.WALKING_WOLF_HEAD,
                    ModEntities.WALKING_CHICKEN_HEAD, ModEntities.WALKING_SKELETON_HEAD,
                    ModEntities.WALKING_FOX_HEAD);

            return CompletableFuture.allOf(
                    save(cache, "infested_enderman", endermanCarry),
                    save(cache, "walking_enderman_head", endermanHeadCarry)
            );
        }
        @SafeVarargs
        private List<String> names(RegistryObject<? extends EntityType<?>>... entities) {
            List<String> list = new ArrayList<>();
            for (var e : entities) list.add(regName(e.get()));
            return list;
        }
        private CompletableFuture<?> save(CachedOutput c, String file, List<String> list) {
            return saveStable(c, JsonParser.parseString(GSON.toJson(new EntityCarryData(list))),
                    dataPath(out, "entity_carry", file));
        }
        @Override public String getName() { return "EPCA Entity Carry"; }
    }

    // ═══════════════════ 4. Block Conversions ═══════════════════

    public static class BlockConversionsData {
        public Map<String, String> conversions;
        BlockConversionsData(Map<String, String> c) { this.conversions = c; }
    }

    public static class StageConfigData {
        public Map<String, String> conversions;
        public int plant_radius;
        public int leaves_radius;
        public int leaves_interval;

        public StageConfigData(Map<String, String> conversions, int plantRadius, int leavesRadius, int leavesInterval) {
            this.conversions = conversions;
            this.plant_radius = plantRadius;
            this.leaves_radius = leavesRadius;
            this.leaves_interval = leavesInterval;
        }
    }

    public static class BlockConversionDataProvider implements DataProvider {
        private final PackOutput out;
        public BlockConversionDataProvider(PackOutput out) { this.out = out; }

        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            // general
            Map<String, String> general = new LinkedHashMap<>();
            put(general, "minecraft:dirt", "epca:infested_dirt");
            put(general, "minecraft:grass_block", "epca:infested_dirt");
            put(general, "minecraft:podzol", "epca:infested_dirt");
            put(general, "minecraft:coarse_dirt", "epca:infested_dirt");
            put(general, "minecraft:rooted_dirt", "epca:infested_dirt");
            put(general, "minecraft:mud", "epca:infested_dirt");
            put(general, "minecraft:muddy_mangrove_roots", "epca:infested_dirt");
            put(general, "minecraft:gravel", "epca:infested_dirt");
            put(general, "minecraft:suspicious_gravel", "epca:infested_dirt");
            put(general, "minecraft:clay", "epca:infested_dirt");
            put(general, "minecraft:mycelium", "epca:infested_dirt");
            put(general, "minecraft:dirt_path", "epca:infested_dirt");
            put(general, "minecraft:farmland", "epca:infested_dirt");

            // 木板、台阶、楼梯、栅栏
            for (String wood : Arrays.asList("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry")) {
                put(general, "minecraft:" + wood + "_planks", "epca:infested_planks");
                put(general, "minecraft:" + wood + "_slab", "epca:infested_planks_slab");
                put(general, "minecraft:" + wood + "_stairs", "epca:infested_planks_stairs");
                put(general, "minecraft:" + wood + "_fence", "epca:infested_planks_fence");
            }

            // 原木、木头、去皮原木、去皮木头
            for (String wood : Arrays.asList("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry")) {
                put(general, "minecraft:" + wood + "_log", "epca:infested_log");
                put(general, "minecraft:" + wood + "_wood", "epca:infested_wood");
                put(general, "minecraft:stripped_" + wood + "_log", "epca:infested_stripped_log");
                put(general, "minecraft:stripped_" + wood + "_wood", "epca:infested_stripped_wood");
            }

            // 沙子
            put(general, "minecraft:sand", "epca:infested_sand");
            put(general, "minecraft:suspicious_sand", "epca:infested_sand");
            put(general, "minecraft:red_sand", "epca:infested_sand");

            // 普通石头及其变种
            put(general, "minecraft:stone", "epca:infested_stone");
            put(general, "minecraft:diorite", "epca:infested_stone");
            put(general, "minecraft:andesite", "epca:infested_stone");
            put(general, "minecraft:granite", "epca:infested_stone");
            put(general, "minecraft:calcite", "epca:infested_stone");
            put(general, "minecraft:dripstone_block", "epca:infested_stone");

            put(general, "minecraft:smooth_stone", "epca:infested_polished_stone");
            put(general, "minecraft:polished_diorite", "epca:infested_polished_stone");
            put(general, "minecraft:polished_andesite", "epca:infested_polished_stone");
            put(general, "minecraft:polished_granite", "epca:infested_polished_stone");

            // 石台阶
            put(general, "minecraft:stone_slab", "epca:infested_stone_slab");
            put(general, "minecraft:diorite_slab", "epca:infested_stone_slab");
            put(general, "minecraft:andesite_slab", "epca:infested_stone_slab");
            put(general, "minecraft:granite_slab", "epca:infested_stone_slab");
            put(general, "minecraft:smooth_stone_slab", "epca:infested_polished_stone_slab");
            put(general, "minecraft:polished_diorite_slab", "epca:infested_polished_stone_slab");
            put(general, "minecraft:polished_andesite_slab", "epca:infested_polished_stone_slab");
            put(general, "minecraft:polished_granite_slab", "epca:infested_polished_stone_slab");

            // 石楼梯
            put(general, "minecraft:stone_stairs", "epca:infested_stone_stairs");
            put(general, "minecraft:diorite_stairs", "epca:infested_stone_stairs");
            put(general, "minecraft:andesite_stairs", "epca:infested_stone_stairs");
            put(general, "minecraft:granite_stairs", "epca:infested_stone_stairs");
            put(general, "minecraft:polished_diorite_stairs", "epca:infested_polished_stone_stairs");
            put(general, "minecraft:polished_andesite_stairs", "epca:infested_polished_stone_stairs");
            put(general, "minecraft:polished_granite_stairs", "epca:infested_polished_stone_stairs");

            // 石墙（包含 diorite, andesite, granite）
            put(general, "minecraft:diorite_wall", "epca:infested_stone_wall");
            put(general, "minecraft:andesite_wall", "epca:infested_stone_wall");
            put(general, "minecraft:granite_wall", "epca:infested_stone_wall");

            // 圆石
            put(general, "minecraft:cobblestone", "epca:infested_cobblestone");
            put(general, "minecraft:mossy_cobblestone", "epca:infested_cobblestone");
            put(general, "minecraft:cobblestone_slab", "epca:infested_cobblestone_slab");
            put(general, "minecraft:mossy_cobblestone_slab", "epca:infested_cobblestone_slab");
            put(general, "minecraft:cobblestone_stairs", "epca:infested_cobblestone_stairs");
            put(general, "minecraft:mossy_cobblestone_stairs", "epca:infested_cobblestone_stairs");
            put(general, "minecraft:cobblestone_wall", "epca:infested_cobblestone_wall");
            put(general, "minecraft:mossy_cobblestone_wall", "epca:infested_cobblestone_wall");

            // 石砖
            put(general, "minecraft:stone_bricks", "epca:infested_stone_bricks");
            put(general, "minecraft:mossy_stone_bricks", "epca:infested_stone_bricks");
            put(general, "minecraft:stone_bricks_slab", "epca:infested_stone_bricks_slab");
            put(general, "minecraft:mossy_stone_bricks_slab", "epca:infested_stone_bricks_slab");
            put(general, "minecraft:stone_bricks_stairs", "epca:infested_stone_bricks_stairs");
            put(general, "minecraft:mossy_stone_bricks_stairs", "epca:infested_stone_bricks_stairs");
            put(general, "minecraft:stone_bricks_wall", "epca:infested_stone_bricks_wall");
            put(general, "minecraft:mossy_stone_bricks_wall", "epca:infested_stone_bricks_wall");
            put(general, "minecraft:cracked_stone_bricks", "epca:infested_cracked_stone_bricks");
            put(general, "minecraft:chiseled_stone_bricks", "epca:infested_chiseled_stone_bricks");

            // 砂岩
            put(general, "minecraft:sandstone", "epca:infested_sandstone");
            put(general, "minecraft:red_sandstone", "epca:infested_sandstone");
            put(general, "minecraft:sandstone_slab", "epca:infested_sandstone_slab");
            put(general, "minecraft:red_sandstone_slab", "epca:infested_sandstone_slab");
            put(general, "minecraft:sandstone_stairs", "epca:infested_sandstone_stairs");
            put(general, "minecraft:red_sandstone_stairs", "epca:infested_sandstone_stairs");
            put(general, "minecraft:sandstone_wall", "epca:infested_sandstone_wall");
            put(general, "minecraft:red_sandstone_wall", "epca:infested_sandstone_wall");
            put(general, "minecraft:chiseled_red_sandstone", "epca:infested_chiseled_red_sandstone");
            put(general, "minecraft:chiseled_sandstone", "epca:infested_chiseled_sandstone");
            put(general, "minecraft:cut_sandstone", "epca:infested_cut_sandstone");
            put(general, "minecraft:cut_red_sandstone", "epca:infested_cut_sandstone");
            put(general, "minecraft:cut_sandstone_slab", "epca:infested_cut_sandstone_slab");
            put(general, "minecraft:cut_red_sandstone_slab", "epca:infested_cut_sandstone_slab");
            put(general, "minecraft:smooth_sandstone", "epca:infested_smooth_sandstone");
            put(general, "minecraft:smooth_red_sandstone", "epca:infested_smooth_sandstone");
            put(general, "minecraft:smooth_sandstone_slab", "epca:infested_smooth_sandstone_slab");
            put(general, "minecraft:smooth_red_sandstone_slab", "epca:infested_smooth_sandstone_slab");
            put(general, "minecraft:smooth_sandstone_stairs", "epca:infested_smooth_sandstone_stairs");
            put(general, "minecraft:smooth_red_sandstone_stairs", "epca:infested_smooth_sandstone_stairs");

            // 矿石（普通）
            put(general, "minecraft:coal_ore", "epca:infested_coal_ore");
            put(general, "minecraft:copper_ore", "epca:infested_copper_ore");
            put(general, "minecraft:iron_ore", "epca:infested_iron_ore");
            put(general, "minecraft:gold_ore", "epca:infested_gold_ore");
            put(general, "minecraft:lapis_ore", "epca:infested_lapis_ore");
            put(general, "minecraft:redstone_ore", "epca:infested_redstone_ore");
            put(general, "minecraft:emerald_ore", "epca:infested_emerald_ore");
            put(general, "minecraft:diamond_ore", "epca:infested_diamond_ore");

            // 雪
            put(general, "minecraft:snow", "epca:infested_snow");
            put(general, "minecraft:snow_block", "epca:infested_snow_block");

            // 被感染的方块（原版）→ 自己的感染变种
            put(general, "minecraft:infested_cobblestone", "epca:infested_infested_cobblestone");
            put(general, "minecraft:infested_stone", "epca:infested_infested_stone");
            put(general, "minecraft:infested_stone_bricks", "epca:infested_infested_stone_bricks");
            put(general, "minecraft:infested_mossy_stone_bricks", "epca:infested_infested_stone_bricks");
            put(general, "minecraft:infested_cracked_stone_bricks", "epca:infested_infested_cracked_stone_bricks");
            put(general, "minecraft:infested_chiseled_stone_bricks", "epca:infested_infested_chiseled_stone_bricks");

            // 来自 caerula_arbor 的方块
            put(general, "caerula_arbor:sea_trail_grown", "epca:infested_nethersea_brand_grown");
            put(general, "caerula_arbor:sea_trail_solid", "epca:infested_nethersea_brand_solid");

            // 钟乳石
            put(general, "minecraft:pointed_dripstone", "epca:infested_pointed_dripstone");

            // 深板岩及厚重石头系列
            put(general, "minecraft:deepslate", "epca:infested_heavy_stone");
            put(general, "minecraft:tuff", "epca:infested_heavy_stone");
            put(general, "minecraft:infested_deepslate", "epca:infested_infested_heavy_stone");
            put(general, "minecraft:deepslate_coal_ore", "epca:infested_heavy_coal_ore");
            put(general, "minecraft:deepslate_copper_ore", "epca:infested_heavy_copper_ore");
            put(general, "minecraft:deepslate_iron_ore", "epca:infested_heavy_iron_ore");
            put(general, "minecraft:deepslate_gold_ore", "epca:infested_heavy_gold_ore");
            put(general, "minecraft:deepslate_lapis_ore", "epca:infested_heavy_lapis_ore");
            put(general, "minecraft:deepslate_redstone_ore", "epca:infested_heavy_redstone_ore");
            put(general, "minecraft:deepslate_emerald_ore", "epca:infested_heavy_emerald_ore");
            put(general, "minecraft:deepslate_diamond_ore", "epca:infested_heavy_diamond_ore");
            put(general, "minecraft:cobbled_deepslate", "epca:infested_heavy_cobblestone");
            put(general, "minecraft:vine", "epca:infested_vine");
            put(general, "minecraft:cobbled_deepslate_stairs", "epca:infested_heavy_cobblestone_stairs");
            put(general, "minecraft:cobbled_deepslate_slab", "epca:infested_heavy_cobblestone_slab");
            put(general, "minecraft:polished_deepslate", "epca:infested_polished_heavy_stone");
            put(general, "minecraft:polished_deepslate_stairs", "epca:infested_polished_heavy_stone_stairs");
            put(general, "minecraft:polished_deepslate_slab", "epca:infested_polished_heavy_stone_slab");
            put(general, "minecraft:polished_deepslate_wall", "epca:infested_polished_heavy_stone_wall");
            put(general, "minecraft:sweet_berry_bush", "epca:infested_sweet_berry_bush");
            put(general, "minecraft:lily_pad", "epca:infested_lily_pad");
            put(general, "minecraft:deepslate_bricks", "epca:infested_heavy_bricks");
            put(general, "minecraft:cracked_deepslate_bricks", "epca:infested_cracked_heavy_bricks");
            put(general, "minecraft:deepslate_bricks_slab", "epca:infested_heavy_bricks_slab");
            put(general, "minecraft:deepslate_bricks_stairs", "epca:infested_heavy_bricks_stairs");
            put(general, "minecraft:deepslate_bricks_wall", "epca:infested_heavy_bricks_wall");
            put(general, "minecraft:carved_pumpkin", "epca:infested_carved_pumpkin");
            put(general, "minecraft:jack_o_lantern", "epca:infested_carved_pumpkin");
            put(general, "minecraft:pumpkin", "epca:infested_pumpkin");
            put(general, "minecraft:cactus", "epca:infested_cactus");
            put(general, "minecraft:sugar_cane", "epca:infested_sugar_cane");
            put(general, "minecraft:web", "epca:infested_spider_web");
            put(general, "minecraft:deepslate_tiles", "epca:infested_heavy_tiles");
            put(general, "minecraft:cracked_deepslate_tiles", "epca:infested_cracked_heavy_tiles");
            put(general, "minecraft:deepslate_tiles_slab", "epca:infested_heavy_tiles_slab");
            put(general, "minecraft:deepslate_tiles_stairs", "epca:infested_heavy_tiles_stairs");
            put(general, "minecraft:deepslate_tiles_wall", "epca:infested_heavy_tiles_wall");

            // beckon — 同 general
            Map<String, String> beckon = new LinkedHashMap<>(general);

            return CompletableFuture.allOf(
                    saveConv(cache, "general_block_conversions", general, 1, 4, 2),
                    saveConv(cache, "stage_i_block_conversions", beckon, 1, 10, 2),
                    saveConv(cache, "stage_ii_block_conversions", beckon, 1, 20, 2)
            );
        }
        private void put(Map<String, String> m, String k, String v) { m.put(k, v); }
        private CompletableFuture<?> saveConv(CachedOutput c, String file, Map<String, String> map,
                                              int plantRadius, int leavesRadius, int leavesInterval) {
            StageConfigData data = new StageConfigData(map, plantRadius, leavesRadius, leavesInterval);
            return saveStable(c, JsonParser.parseString(GSON.toJson(data)),
                    dataPath(out, "block_conversions", file));
        }
        @Override public String getName() { return "EPCA Block Conversions"; }
    }

    // ═══════════════════ 5. Biomass Spawns ═══════════════════

    public static class SpawnEffect {
        public String effect; public int duration; public int amplifier;
        public boolean ambient, visible, icon;
        SpawnEffect(String e, int d, int a) {
            this.effect = e; this.duration = d; this.amplifier = a;
            this.ambient = false; this.visible = true; this.icon = true;
        }
    }
    public static class SpawnEntry {
        public String entity; public int weight;
        public int min_count, max_count, life_time;
        public List<SpawnEffect> effects;
    }
    public static class BiomassSpawnsData {
        public List<SpawnEntry> water_spawns, land_spawns;
    }

    public static class BiomassSpawnDataProvider implements DataProvider {
        private final PackOutput out;
        public BiomassSpawnDataProvider(PackOutput out) { this.out = out; }
        private static final List<SpawnEffect> RAGE = List.of(new SpawnEffect("epca:rage", 1200, 1));

        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            var small = new BiomassSpawnsData();
            small.water_spawns = List.of(e(ModEntities.FINS, 100, 1, 1));
            small.land_spawns  = List.of(e(ModEntities.RIPPER, 100, 1, 1));

            var medium = new BiomassSpawnsData();
            medium.water_spawns = List.of(
                    e(ModEntities.INFESTED_DROWNED, 80, 1, 1), e(ModEntities.FINS, 20, 1, 2));
            medium.land_spawns = List.of(
                    e(ModEntities.INFESTED_ZOMBIE, 5,1,1), e(ModEntities.INFESTED_HUSK, 5,1,1),
                    e(ModEntities.INFESTED_VILLAGER, 5,1,1), e(ModEntities.INFESTED_ZOMBIE_VILLAGER, 5,1,1),
                    e(ModEntities.INFESTED_PILLAGER, 5,1,1), e(ModEntities.INFESTED_VINDICATOR, 5,1,1),
                    e(ModEntities.INFESTED_PIG, 5,1,1), e(ModEntities.INFESTED_COW, 6,1,1),
                    e(ModEntities.INFESTED_SHEEP, 6,1,1), e(ModEntities.INFESTED_WOLF, 6,1,2),
                    e(ModEntities.INFESTED_SILVERFISH, 5,3,4),
                    e(ModEntities.INFESTED_SLIME_SIZE1, 4,1,1), e(ModEntities.INFESTED_SLIME_SIZE3, 4,1,1),
                    e(ModEntities.INFESTED_CHICKEN, 5,2,2), e(ModEntities.INFESTED_BAT, 5,4,5),
                    e(ModEntities.RIPPER, 5,2,3), e(ModEntities.INFESTED_SKELETON, 5,1,1),
                    e(ModEntities.FLYING_CARRIER, 5,1,1), e(ModEntities.LIGHT_CARRIER, 2,1,1),
                    e(ModEntities.INFESTED_ENDERMAN, 2,1,1), e(ModEntities.INFESTED_FOX, 5,1,2));

            return CompletableFuture.allOf(
                    saveSpawns(cache, "biomass_small", small),
                    saveSpawns(cache, "biomass_medium", medium)
            );
        }
        private CompletableFuture<?> saveSpawns(CachedOutput c, String file, BiomassSpawnsData data) {
            return saveStable(c, JsonParser.parseString(GSON.toJson(data)),
                    dataPath(out, "biomass_spawns", file));
        }
        private SpawnEntry e(RegistryObject<? extends EntityType<?>> entity, int weight, int min, int max) {
            SpawnEntry entry = new SpawnEntry();
            entry.entity = regName(entity.get());
            entry.weight = weight;
            entry.min_count = min; entry.max_count = max;
            entry.life_time = 1200;
            entry.effects = RAGE;
            return entry;
        }
        @Override public String getName() { return "EPCA Biomass Spawns"; }
    }

    // ═══════════════════ 6. Altar Points ═══════════════════

    public static class AltarPointDataProvider implements DataProvider {
        private final PackOutput out;
        public AltarPointDataProvider(PackOutput out) { this.out = out; }

        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            JsonObject root = new JsonObject();
            var p = new JsonObject(); p.addProperty("points", 30);
            var a = new JsonObject(); a.addProperty("points", 20);
            root.add("epca:packed_mud_pedestal", p);
            root.add("epca:packed_mud_altar_stone", a);
            return saveStable(cache, root, dataPath(out, "altar_points", "epca_altar_points"));
        }
        @Override public String getName() { return "EPCA Altar Points"; }
    }
}
