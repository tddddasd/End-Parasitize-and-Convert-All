package org.tdddd.epca.impl.datagen;

import com.google.gson.*;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEntities;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.data.DataProvider.saveStable;


public class CustomDataProviders {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    
    private static Path dataPath(PackOutput out, String subfolder, String file) {
        return out.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(epca.MODID + "/" + subfolder + "/" + file + ".json");
    }

    
    private static String regName(EntityType<?> type) {
        return Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type)).toString();
    }

    // ═══════════════════ 1. Entity Conversions ═══════════════════

    public static class EntityConversionRule {
        public String from, to, fins_to, mozzie_to;
        public boolean small_entity_priority = true;
        public int priority = 0;
        public Map<String, Object> nbt_conditions;
        
        public Boolean meat_particles;
    }

    
    private static final Set<String> NO_MEAT_PARTICLES = Set.of(
            "skeleton", "bat", "villager", "pillager", "vindcator", "enderman", "endermite",
            "silverfish", "slime_size0", "slime_size1", "slime_size3", "creeper", "iron_golem",
            "guardian", "elder_guardian", "warden", "magma_cube", "vex", "allay", "blaze",
            "ender_dragon", "fox", "fox_baby", "wolf", "wolf_baby");

    public static class EntityConversionDataProvider implements DataProvider {
        private final PackOutput out;
        public EntityConversionDataProvider(PackOutput out) { this.out = out; }

        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            List<CompletableFuture<?>> tasks = new ArrayList<>();

            
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

            
            convNbt(cache, tasks, "slime_size0", EntityType.SLIME, ModEntities.INFESTED_SLIME_SIZE0, false, 1, Map.of("Size", 0));
            convNbt(cache, tasks, "slime_size1", EntityType.SLIME, ModEntities.INFESTED_SLIME_SIZE1, true,  1, Map.of("Size", 1));
            convNbt(cache, tasks, "slime_size3", EntityType.SLIME, ModEntities.INFESTED_SLIME_SIZE3, true,  1, Map.of("Size", 3));

            
            conv(cache, tasks, "cow_baby",      EntityType.COW,      ModEntities.INFESTED_COW,      true,  0);
            conv(cache, tasks, "chicken_baby",  EntityType.CHICKEN,  ModEntities.INFESTED_CHICKEN,  false, 0);
            conv(cache, tasks, "pig_baby",      EntityType.PIG,      ModEntities.INFESTED_PIG,      true,  0);
            conv(cache, tasks, "sheep_baby",    EntityType.SHEEP,    ModEntities.INFESTED_SHEEP,    true,  0);
            conv(cache, tasks, "fox_baby",      EntityType.FOX,      ModEntities.INFESTED_FOX,      true,  0);
            conv(cache, tasks, "wolf_baby",     EntityType.WOLF,     ModEntities.INFESTED_WOLF,     true,  0);
            conv(cache, tasks, "zombie_baby",   EntityType.ZOMBIE,   ModEntities.INFESTED_ZOMBIE,   true,  0);
            conv(cache, tasks, "husk_baby",     EntityType.HUSK,     ModEntities.INFESTED_HUSK,     true,  0);
            conv(cache, tasks, "drowned_baby",  EntityType.DROWNED,  ModEntities.INFESTED_DROWNED,  true,  0);

            
            convBoss(cache, tasks, "ender_dragon",      EntityType.ENDER_DRAGON,    true, 1);

            
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

        

        /** from=EntityType, to=DeferredHolder */
        private void conv(CachedOutput c, List<CompletableFuture<?>> tasks,
                          String file, EntityType<?> from, DeferredHolder<EntityType<?>, ? extends EntityType<?>> to,
                          boolean smallPrio, int prio) {
            tasks.add(write(c, file, regName(from), regName(to.get()), smallPrio, prio, null));
        }
        
        private void conv(CachedOutput c, List<CompletableFuture<?>> tasks,
                          String file, DeferredHolder<EntityType<?>, ? extends EntityType<?>> from,
                          DeferredHolder<EntityType<?>, ? extends EntityType<?>> to,
                          boolean smallPrio, int prio) {
            tasks.add(write(c, file, regName(from.get()), regName(to.get()), smallPrio, prio, null));
        }
        
        private void convBoss(CachedOutput c, List<CompletableFuture<?>> tasks,
                              String file, EntityType<?> from, boolean smallPrio, int prio) {
            tasks.add(write(c, file, regName(from), null, smallPrio, prio, null));
        }
        
        private void convNbt(CachedOutput c, List<CompletableFuture<?>> tasks,
                             String file, EntityType<?> from, DeferredHolder<EntityType<?>, ? extends EntityType<?>> to,
                             boolean smallPrio, int prio, Map<String, Object> nbt) {
            tasks.add(write(c, file, regName(from), regName(to.get()), smallPrio, prio, nbt));
        }
        
        private void convMozzie(CachedOutput c, List<CompletableFuture<?>> tasks,
                                String file, EntityType<?> from) {
            EntityConversionRule rule = new EntityConversionRule();
            rule.from = regName(from);
            rule.to = null;
            rule.fins_to = null;
            rule.mozzie_to = regName(ModEntities.SMALL_INCOMPLETE_FORM.get());
            rule.small_entity_priority = true;
            rule.priority = 0;
            if (NO_MEAT_PARTICLES.contains(file)) rule.meat_particles = Boolean.FALSE;
            tasks.add(saveStable(c, JsonParser.parseString(GSON.toJson(rule)),
                    dataPath(out, "entity_conversions", file)));
        }

        
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
            if (NO_MEAT_PARTICLES.contains(file)) rule.meat_particles = Boolean.FALSE;
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
        private List<String> names(DeferredHolder<EntityType<?>, ? extends EntityType<?>>... entities) {
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
            put(general, "minecraft:muddy_mangrove_roots", "epca:infested_muddy_mangrove_roots");
            put(general, "minecraft:gravel", "epca:infested_dirt");
            put(general, "minecraft:suspicious_gravel", "epca:infested_dirt");
            put(general, "minecraft:clay", "epca:infested_dirt");
            put(general, "minecraft:mycelium", "epca:infested_dirt");
            put(general, "minecraft:dirt_path", "epca:infested_dirt");
            put(general, "minecraft:farmland", "epca:infested_dirt");

            
            for (String wood : Arrays.asList("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry")) {
                put(general, "minecraft:" + wood + "_planks", "epca:infested_planks");
                put(general, "minecraft:" + wood + "_slab", "epca:infested_planks_slab");
                put(general, "minecraft:" + wood + "_stairs", "epca:infested_planks_stairs");
                put(general, "minecraft:" + wood + "_fence", "epca:infested_planks_fence");
            }

            
            for (String wood : Arrays.asList("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry")) {
                put(general, "minecraft:" + wood + "_log", "epca:infested_log");
                put(general, "minecraft:" + wood + "_wood", "epca:infested_wood");
                put(general, "minecraft:stripped_" + wood + "_log", "epca:infested_stripped_log");
                put(general, "minecraft:stripped_" + wood + "_wood", "epca:infested_stripped_wood");
                put(general, "minecraft:" + wood + "_leaves", "epca:infested_leaves");
            }

            
            put(general, "minecraft:sand", "epca:infested_sand");
            put(general, "minecraft:suspicious_sand", "epca:infested_sand");
            put(general, "minecraft:red_sand", "epca:infested_sand");

            
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

            
            put(general, "minecraft:stone_slab", "epca:infested_stone_slab");
            put(general, "minecraft:diorite_slab", "epca:infested_stone_slab");
            put(general, "minecraft:andesite_slab", "epca:infested_stone_slab");
            put(general, "minecraft:granite_slab", "epca:infested_stone_slab");
            put(general, "minecraft:smooth_stone_slab", "epca:infested_polished_stone_slab");
            put(general, "minecraft:polished_diorite_slab", "epca:infested_polished_stone_slab");
            put(general, "minecraft:polished_andesite_slab", "epca:infested_polished_stone_slab");
            put(general, "minecraft:polished_granite_slab", "epca:infested_polished_stone_slab");

            
            put(general, "minecraft:stone_stairs", "epca:infested_stone_stairs");
            put(general, "minecraft:diorite_stairs", "epca:infested_stone_stairs");
            put(general, "minecraft:andesite_stairs", "epca:infested_stone_stairs");
            put(general, "minecraft:granite_stairs", "epca:infested_stone_stairs");
            put(general, "minecraft:polished_diorite_stairs", "epca:infested_polished_stone_stairs");
            put(general, "minecraft:polished_andesite_stairs", "epca:infested_polished_stone_stairs");
            put(general, "minecraft:polished_granite_stairs", "epca:infested_polished_stone_stairs");

            
            put(general, "minecraft:diorite_wall", "epca:infested_stone_wall");
            put(general, "minecraft:andesite_wall", "epca:infested_stone_wall");
            put(general, "minecraft:granite_wall", "epca:infested_stone_wall");

            
            put(general, "minecraft:cobblestone", "epca:infested_cobblestone");
            put(general, "minecraft:mossy_cobblestone", "epca:infested_cobblestone");
            put(general, "minecraft:cobblestone_slab", "epca:infested_cobblestone_slab");
            put(general, "minecraft:mossy_cobblestone_slab", "epca:infested_cobblestone_slab");
            put(general, "minecraft:cobblestone_stairs", "epca:infested_cobblestone_stairs");
            put(general, "minecraft:mossy_cobblestone_stairs", "epca:infested_cobblestone_stairs");
            put(general, "minecraft:cobblestone_wall", "epca:infested_cobblestone_wall");
            put(general, "minecraft:mossy_cobblestone_wall", "epca:infested_cobblestone_wall");

            
            put(general, "minecraft:stone_bricks", "epca:infested_stone_bricks");
            put(general, "minecraft:mossy_stone_bricks", "epca:infested_stone_bricks");
            put(general, "minecraft:stone_brick_slab", "epca:infested_stone_bricks_slab");
            put(general, "minecraft:mossy_stone_brick_slab", "epca:infested_stone_bricks_slab");
            put(general, "minecraft:stone_brick_stairs", "epca:infested_stone_bricks_stairs");
            put(general, "minecraft:mossy_stone_brick_stairs", "epca:infested_stone_bricks_stairs");
            put(general, "minecraft:stone_brick_wall", "epca:infested_stone_bricks_wall");
            put(general, "minecraft:mossy_stone_brick_wall", "epca:infested_stone_bricks_wall");
            put(general, "minecraft:cracked_stone_bricks", "epca:infested_cracked_stone_bricks");
            put(general, "minecraft:chiseled_stone_bricks", "epca:infested_chiseled_stone_bricks");

            
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

            
            put(general, "minecraft:coal_ore", "epca:infested_coal_ore");
            put(general, "minecraft:copper_ore", "epca:infested_copper_ore");
            put(general, "minecraft:iron_ore", "epca:infested_iron_ore");
            put(general, "minecraft:gold_ore", "epca:infested_gold_ore");
            put(general, "minecraft:lapis_ore", "epca:infested_lapis_ore");
            put(general, "minecraft:redstone_ore", "epca:infested_redstone_ore");
            put(general, "minecraft:emerald_ore", "epca:infested_emerald_ore");
            put(general, "minecraft:diamond_ore", "epca:infested_diamond_ore");

            
            put(general, "minecraft:snow", "epca:infested_snow");
            put(general, "minecraft:snow_block", "epca:infested_snow_block");

            
            put(general, "minecraft:infested_cobblestone", "epca:infested_infested_cobblestone");
            put(general, "minecraft:infested_stone", "epca:infested_infested_stone");
            put(general, "minecraft:infested_stone_bricks", "epca:infested_infested_stone_bricks");
            put(general, "minecraft:infested_mossy_stone_bricks", "epca:infested_infested_stone_bricks");
            put(general, "minecraft:infested_cracked_stone_bricks", "epca:infested_infested_cracked_stone_bricks");
            put(general, "minecraft:infested_chiseled_stone_bricks", "epca:infested_infested_chiseled_stone_bricks");

            
            put(general, "caerula_arbor:sea_trail_grown", "epca:infested_nethersea_brand_grown");
            put(general, "caerula_arbor:sea_trail_solid", "epca:infested_nethersea_brand_solid");

            
            put(general, "minecraft:pointed_dripstone", "epca:infested_pointed_dripstone");

            
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
            put(general, "minecraft:deepslate_brick_slab", "epca:infested_heavy_bricks_slab");
            put(general, "minecraft:deepslate_brick_stairs", "epca:infested_heavy_bricks_stairs");
            put(general, "minecraft:deepslate_brick_wall", "epca:infested_heavy_bricks_wall");
            put(general, "minecraft:carved_pumpkin", "epca:infested_carved_pumpkin");
            put(general, "minecraft:jack_o_lantern", "epca:infested_carved_pumpkin");
            put(general, "minecraft:pumpkin", "epca:infested_pumpkin");
            put(general, "minecraft:cactus", "epca:infested_cactus");
            put(general, "minecraft:sugar_cane", "epca:infested_sugar_cane");
            put(general, "minecraft:cobweb", "epca:infested_spider_web");
            put(general, "minecraft:deepslate_tiles", "epca:infested_heavy_tiles");
            put(general, "minecraft:cracked_deepslate_tiles", "epca:infested_cracked_heavy_tiles");
            put(general, "minecraft:deepslate_tile_slab", "epca:infested_heavy_tiles_slab");
            put(general, "minecraft:deepslate_tile_stairs", "epca:infested_heavy_tiles_stairs");
            put(general, "minecraft:deepslate_tile_wall", "epca:infested_heavy_tiles_wall");
            put(general, "minecraft:mangrove_roots", "epca:infested_mangrove_roots");
            put(general, "minecraft:short_grass", "epca:infested_grass");
            put(general, "minecraft:dead_bush", "epca:infested_dead_bush");
            put(general, "minecraft:fern", "epca:infested_fern");
            put(general, "minecraft:tall_grass", "epca:infested_tall_grass");
            put(general, "minecraft:large_fern", "epca:infested_tall_fern");
            put(general, "minecraft:chiseled_deepslate", "epca:infested_chiseled_deepslate");

            
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
        private SpawnEntry e(DeferredHolder<EntityType<?>, ? extends EntityType<?>> entity, int weight, int min, int max) {
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
}
