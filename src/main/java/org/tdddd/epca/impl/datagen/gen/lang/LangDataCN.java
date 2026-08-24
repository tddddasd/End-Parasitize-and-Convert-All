package org.tdddd.epca.impl.datagen.gen.lang;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import org.tdddd.epca.impl.epca;

public class LangDataCN extends LanguageProvider {
    public LangDataCN(PackOutput output, String locale) {
        super(output, epca.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        // 物品组 / 基本 UI
        add("itemGroup." + epca.MODID + ".main_tab", "终末-归寄万物");

        add("category.epca.altar_crafting", "祭坛合成");

        // 游戏规则
        add("gamerule.epca_hardnessConversionBlock", "根据硬度转化模组方块");
        add("gamerule.epca_hardnessConversionBlock.description", "当模组方块没有转化配置映射时，是否允许根据硬度将其转化为虫染残渣、类岩、类板等方块");

        // 工具提示
        add("tooltip.epca.max_damage_type", "最大受击倍率伤害类型: %s (\u00d7%s)");
        add("tooltip.epca.min_kill_count", "至少击杀数: %d");
        add("tooltip.epca.broken_adaptation_time", "破适应性剩余: %.1f秒");
        add("tooltip.epca.adaptation_level", "适应性等级: %d/%d");
        add("tooltip.epca.attack_count", "受击次数: %d/%d");
        add("tooltip.epca.block_count", "格挡次数: %d/%d");
        add("tooltip.epca.player_attack_count", "攻击次数: %d/%d");
        add("tooltip.epca.module_description", "模块信息：");
        add("tooltip.epca.defense", "防御：%s");
        add("tooltip.epca.attack", "攻击：%s");
        add("tooltip.epca.biomass", "消耗：%s");
        add("tooltip.epca.negative", "负面：%s");
        add("tooltip.epca.item_proficiency", "熟练度: %d/%d [伤害 +%s%%]");
        add("item.small_item_frame.added", "已记录：%s");
        add("item.small_item_frame.max_reached", "无法添加（已达上限或已存在）");
        add("item.small_item_frame.no_offhand", "副手无物品");
        add("item.small_item_frame.count", "已存 %d/%d");

        // Jade 插件
        add("config.jade.plugin_epca.damage_adaptation_info", "伤害适应性信息");
        add("config.jade.plugin_epca.kill_count_info", "击杀计数信息");

        // 进度
        add("advancements.epca.root.description", "末日的开始");
        add("advancements.epca.root.title", "如常？");
        add("advancements.epca.sense_of_crisis.title", "危机感");
        add("advancements.epca.sense_of_crisis.description", "对寄生体的攻击会吸引更多寄生体");
        add("advancements.epca.master_difficulty.title", "只奉给献的上的人...");
        add("advancements.epca.master_difficulty.description", "在未开启作弊的情况下进入困难模式-大师难度的世界");

        // 生物群系
        add("biome.epca.parasite_biome", "寄生体生物群系");

        // 创造模式物品分类
        add("item.epca.category.spawn_eggs", "\u00a7l刷怪蛋");
        add("item.epca.category.materials_gear", "\u00a7l材料与装备");
        add("item.epca.category.blocks", "\u00a7l方块");
        add("item.epca.category_onesent", "种阶：初探");
        add("item.epca.category_poverty", "种阶：粗制");
        add("item.epca.category_infested", "种阶：虫染");
        add("item.epca.category_reshape", "种阶：重塑");
        add("item.epca.category_link", "种阶：连结");

        // 图鉴
        add("epca.tab.parent.main_tab", "寄生体图鉴");
        add("epca.tab.child.onesent", "初探种");

        // 实体名称
        add("entity.epca.yawning_nya.join", "\u00a7eYawning_Nya加入了世界");
        add("entity.epca.yawning_nya", "Yawning_Nya");
        add("entity.epca.curbug", "诅虫");
        add("entity.epca.ripper", "裂兽");
        add("entity.epca.small_incomplete_form", "小块未成形寄生体");
        add("entity.epca.medium_incomplete_form", "中块未成形寄生体");
        add("entity.epca.large_incomplete_form", "大块未成形寄生体");
        add("entity.epca.infested_zombie", "虫染僵尸");
        add("entity.epca.walking_zombie_head", "虫染僵尸头颅");
        add("entity.epca.infested_husk", "虫染尸壳");
        add("entity.epca.walking_husk_head", "虫染尸壳头颅");
        add("entity.epca.infested_drowned", "虫染溺尸");
        add("entity.epca.walking_drowned_head", "虫染溺尸头颅");
        add("entity.epca.biomass_small", "生物质");
        add("entity.epca.biomass_medium", "生物质");
        add("entity.epca.stage_i_beckon", "一阶召唤柱");
        add("entity.epca.stage_ii_beckon", "二阶召唤柱");
        add("entity.epca.viral_bomb", "病毒炸弹");
        add("entity.epca.viral_bomb_ii", "病毒炸弹");
        add("entity.epca.infested_pillager", "虫染掠夺者");
        add("entity.epca.walking_pillager_head", "虫染掠夺者头颅");
        add("entity.epca.infested_vindicator", "虫染卫道士");
        add("entity.epca.walking_vindicator_head", "虫染卫道士头颅");
        add("entity.epca.infested_villager", "虫染村民");
        add("entity.epca.walking_villager_head", "虫染村民头颅");
        add("entity.epca.infested_zombie_villager", "虫染僵尸村民");
        add("entity.epca.walking_zombie_villager_head", "虫染僵尸村民头颅");
        add("entity.epca.fins", "鳍兽");
        add("entity.epca.infested_pig", "虫染猪");
        add("entity.epca.walking_pig_head", "虫染猪头颅");
        add("entity.epca.infested_sheep", "虫染羊");
        add("entity.epca.walking_sheep_head", "虫染羊头颅");
        add("entity.epca.infested_cow", "虫染牛");
        add("entity.epca.walking_cow_head", "虫染牛头颅");
        add("entity.epca.infested_wolf", "虫染狼");
        add("entity.epca.walking_wolf_head", "虫染狼头颅");
        add("entity.epca.infested_chicken", "虫染鸡");
        add("entity.epca.walking_chicken_head", "虫染鸡头颅");
        add("entity.epca.mozzie", "躁蚊");
        add("entity.epca.infested_slime_size0", "虫染史莱姆");
        add("entity.epca.infested_slime_size1", "虫染史莱姆");
        add("entity.epca.infested_slime_size3", "虫染史莱姆");
        add("entity.epca.living_flesh_size0", "活体肉块");
        add("entity.epca.living_flesh_size1", "活体肉块");
        add("entity.epca.living_flesh_size2", "活体肉块");
        add("entity.epca.living_flesh_size3", "活体肉块");
        add("entity.epca.living_flesh_size4", "活体肉块");
        add("entity.epca.reshape_longarms", "重塑体-长臂");
        add("entity.epca.flying_carrier", "飞行载体");
        add("entity.epca.infested_enderman", "虫染末影人");
        add("entity.epca.walking_enderman_head", "虫染末影人头颅");
        add("entity.epca.infested_endermite", "虫染末影螨");
        add("entity.epca.infested_silverfish", "虫染蠹虫");
        add("entity.epca.light_carrier", "轻型载体");
        add("entity.epca.infested_skeleton", "虫染骷髅");
        add("entity.epca.walking_skeleton_head", "虫染骷髅头颅");
        add("entity.epca.bone_fragment", "虫染骨碎片");
        add("entity.epca.bone_arrow", "虫染骨箭");
        add("entity.epca.infested_ender_pearl", "虫染末影珍珠");
        add("entity.epca.reshape_yelloweye", "重塑体-黄眸");
        add("entity.epca.infested_fox", "虫染狐狸");
        add("entity.epca.walking_fox_head", "虫染狐狸头颅");
        add("entity.epca.reshape_part", "重塑体-长臂");
        add("entity.epca.infested_pumpkin_head", "虫染南瓜头");

        // 投掷矛
        add("entity.epca.thrown_wooden_spear", "木矛");
        add("entity.epca.thrown_stone_spear", "石矛");
        add("entity.epca.thrown_flint_spear", "燧石矛");
        add("entity.epca.thrown_copper_spear", "铜矛");
        add("entity.epca.thrown_iron_spear", "铁矛");
        add("entity.epca.thrown_golden_spear", "金矛");
        add("entity.epca.thrown_diamond_spear", "钻石矛");
        add("entity.epca.thrown_netherite_spear", "下界合金矛");

        // 状态效果
        add("effect.epca.bleeding", "流血");
        add("effect.epca.viral", "病毒");
        add("effect.epca.fear", "恐慌");
        add("effect.epca.coth", "\u00a7c寄巢之唤");
        add("effect.epca.corrosive", "腐蚀");
        add("effect.epca.rage", "狂怒");
        add("effect.epca.needler", "穿刺");
        add("effect.epca.deep_sneak", "深潜");
        add("effect.epca.solidify", "固化");
        add("effect.epca.ender_erosion", "末影侵蚀");
        add("effect.epca.spirit", "灵体化");
        add("effect.epca.camouflage", "伪装");
        add("effect.epca.contempt_inorganic", "\u00a74蔑视无机");
        add("effect.epca.soul_protection", "灵魂格护");
        add("effect.epca.fear.message", "你正处于恐慌状态！");

        // 物品
        add("item.epca.wooden_spear", "木矛");
        add("item.epca.stone_spear", "石矛");
        add("item.epca.flint_spear", "燧石矛");
        add("item.epca.copper_spear", "铜矛");
        add("item.epca.iron_spear", "铁矛");
        add("item.epca.golden_spear", "金矛");
        add("item.epca.diamond_spear", "钻石矛");
        add("item.epca.netherite_spear", "下界合金矛");

        // 刷怪蛋
        add("item.epca.ripper_spawn_egg", "裂兽刷怪蛋");
        add("item.epca.curbug_spawn_egg", "诅虫刷怪蛋");
        add("item.epca.small_incomplete_form_spawn_egg", "小块未成形寄生体刷怪蛋");
        add("item.epca.medium_incomplete_form_spawn_egg", "中块未成形寄生体刷怪蛋");
        add("item.epca.large_incomplete_form_spawn_egg", "大块未成形寄生体刷怪蛋");
        add("item.epca.infested_zombie_spawn_egg", "虫染僵尸刷怪蛋");
        add("item.epca.walking_zombie_head_spawn_egg", "虫染僵尸头颅刷怪蛋");
        add("item.epca.infested_husk_spawn_egg", "虫染尸壳刷怪蛋");
        add("item.epca.walking_husk_head_spawn_egg", "虫染尸壳头颅刷怪蛋");
        add("item.epca.infested_drowned_spawn_egg", "虫染溺尸刷怪蛋");
        add("item.epca.walking_drowned_head_spawn_egg", "虫染溺尸头颅刷怪蛋");
        add("item.epca.stage_i_beckon_spawn_egg", "一阶召唤柱刷怪蛋");
        add("item.epca.stage_ii_beckon_spawn_egg", "二阶召唤柱刷怪蛋");
        add("item.epca.infested_pillager_spawn_egg", "虫染掠夺者刷怪蛋");
        add("item.epca.walking_pillager_head_spawn_egg", "虫染掠夺者头颅刷怪蛋");
        add("item.epca.infested_vindicator_spawn_egg", "虫染卫道士刷怪蛋");
        add("item.epca.walking_vindicator_head_spawn_egg", "虫染卫道士头颅刷怪蛋");
        add("item.epca.infested_villager_spawn_egg", "虫染村民刷怪蛋");
        add("item.epca.walking_villager_head_spawn_egg", "虫染村民头颅刷怪蛋");
        add("item.epca.infested_zombie_villager_spawn_egg", "虫染僵尸村民刷怪蛋");
        add("item.epca.walking_zombie_villager_head_spawn_egg", "虫染僵尸村民头颅刷怪蛋");
        add("item.epca.fins_spawn_egg", "鳍兽刷怪蛋");
        add("item.epca.infested_pig_spawn_egg", "虫染猪刷怪蛋");
        add("item.epca.walking_pig_head_spawn_egg", "虫染猪头颅刷怪蛋");
        add("item.epca.infested_sheep_spawn_egg", "虫染羊刷怪蛋");
        add("item.epca.walking_sheep_head_spawn_egg", "虫染羊头颅刷怪蛋");
        add("item.epca.infested_cow_spawn_egg", "虫染牛刷怪蛋");
        add("item.epca.walking_cow_head_spawn_egg", "虫染牛头颅刷怪蛋");
        add("item.epca.infested_wolf_spawn_egg", "虫染狼刷怪蛋");
        add("item.epca.walking_wolf_head_spawn_egg", "虫染狼头颅刷怪蛋");
        add("item.epca.infested_chicken_spawn_egg", "虫染鸡刷怪蛋");
        add("item.epca.walking_chicken_head_spawn_egg", "虫染鸡头颅刷怪蛋");
        add("item.epca.mozzie_spawn_egg", "躁蚊刷怪蛋");
        add("item.epca.infested_slime_spawn_egg", "虫染史莱姆刷怪蛋");
        add("item.epca.living_flesh_spawn_egg", "活体肉块刷怪蛋");
        add("item.epca.reshape_longarms_spawn_egg", "重塑体-长臂刷怪蛋");
        add("item.epca.flying_carrier_spawn_egg", "飞行载体刷怪蛋");
        add("item.epca.infested_enderman_spawn_egg", "虫染末影人刷怪蛋");
        add("item.epca.walking_enderman_head_spawn_egg", "虫染末影人头颅刷怪蛋");
        add("item.epca.infested_endermite_spawn_egg", "虫染末影螨刷怪蛋");
        add("item.epca.infested_silverfish_spawn_egg", "虫染蠹虫刷怪蛋");
        add("item.epca.light_carrier_spawn_egg", "轻型载体刷怪蛋");
        add("item.epca.infested_skeleton_spawn_egg", "虫染骷髅刷怪蛋");
        add("item.epca.walking_skeleton_head_spawn_egg", "虫染骷髅头颅刷怪蛋");
        add("item.epca.reshape_yelloweye_spawn_egg", "重塑体-黄眸刷怪蛋");
        add("item.epca.infested_fox_spawn_egg", "虫染狐狸刷怪蛋");
        add("item.epca.walking_fox_head_spawn_egg", "虫染狐狸头颅刷怪蛋");

        // 材料与特殊物品
        add("item.epca.parasite_viscera", "寄体内脏");
        add("item.epca.infested_bone", "虫染骨");
        add("item.epca.weird_minced_flesh", "怪异肉沫");
        add("item.epca.diseased_heart", "患疾心脏");
        add("item.epca.infested_flesh", "虫染肉");
        add("item.epca.fins_fin", "鳍兽的鳍");
        add("item.epca.reshape_flesh", "重塑体肉排");
        add("item.epca.reshape_shell", "重塑体甲壳");
        add("item.epca.twisted_bone", "扭曲骨头");
        add("item.epca.tight_tendons", "紧绷肌腱");
        add("item.epca.gasbag_debris", "气囊残片");
        add("item.epca.beckon_membrane", "召唤柱肉膜");
        add("item.epca.erosion_clock", "侵蚀刻钟");
        add("item.epca.living_armor_box", "活体盔甲盒");
        add("item.epca.feeding_module_i", "[一阶自动喂食模块]");
        add("item.epca.instinct_module_i", "[一阶血性本能模块]");
        add("item.epca.flesh_armor_module_i", "[一阶肉甲模块]");
        add("item.epca.netherite_module_i", "[一阶下界合金化模块]");
        add("item.epca.flight_module_i", "[一阶飞行模块]");
        add("item.epca.living_armor_box_module", "盔甲盒模块");
        add("item.epca.infested_sweet_berries", "虫染甜浆果");

        // 活体盔甲盒提示
        add("item.epca.living_armor_box.tooltip.storage_info", "存储信息:");
        add("item.epca.living_armor_box.tooltip.storage_space", "  %s/%s 个物品");
        add("item.epca.living_armor_box.tooltip.stored_items", "存储的物品:");
        add("item.epca.living_armor_box.tooltip.empty", "  空");
        add("item.epca.living_armor_box.tooltip.more_items", "  ... 还有 %s 个物品");
        add("item.epca.living_armor_box.tooltip.usage", "使用方法:");
        add("item.epca.living_armor_box.tooltip.use_right_click", "  右键: 装备/卸下活体盔甲");
        add("item.epca.living_armor_box.tooltip.use_sneak_right_click_with_item", "  潜行+右键(手持物品): 存入模块");
        add("item.epca.living_armor_box.tooltip.use_sneak_right_click_empty", "  潜行+右键(空手): 取出模块");
        add("item.epca.living_armor_box.tooltip.biomass", "生物质: %s / %s");
        add("item.epca.living_armor_box.tooltip.biomass_usage", "  手持食物和盒子按V键增加生物质");
        add("item.epca.living_armor_box.tooltip.adaptation_level", "适应次数: %s");
        add("item.epca.living_armor_box.tooltip.damage_reduction", "伤害减免: %s%%");
        add("item.epca.living_armor_box.tooltip.current_state", "当前状态: %s");
        add("item.epca.living_armor_box.tooltip.state_equipped", "已装备");
        add("item.epca.living_armor_box.tooltip.state_unequipped", "未装备");

        // 活体盔甲
        add("item.epca.living_helmet", "活体头盔");
        add("item.epca.living_chestplate", "活体胸甲");
        add("item.epca.living_leggings", "活体护腿");
        add("item.epca.living_boots", "活体靴子");

        // 其他物品
        add("item.epca.acid_bucket", "酸液桶");
        add("item.epca.infested_rubbish", "虫染垃圾");
        add("item.epca.infested_stick", "虫染木棍");
        add("item.epca.infested_slime_ball", "虫染粘液球");
        add("item.epca.endless_wand", "穷尽灭杖");
        add("item.epca.infested_coal", "虫染煤炭");
        add("item.epca.infested_raw_copper", "虫染粗铜");
        add("item.epca.infested_raw_iron", "虫染粗铁");
        add("item.epca.infested_raw_gold", "虫染粗金");
        add("item.epca.infested_lapis_lazuli", "虫染青金石");
        add("item.epca.infested_emerald", "虫染绿宝石");
        add("item.epca.infested_redstone", "虫染红石");
        add("item.epca.infested_diamond", "虫染钻石");
        add("item.epca.infested_nethersea_brand_mor", "虫染深溟腐质");
        add("item.epca.infested_nethersea_icecream", "虫染溟痕冰淇淋");
        add("item.epca.epca_note", "寄巢笔记");
        add("item.epca.infested_ender_pearl", "虫染末影珍珠");
        add("item.epca.ender_blade_scrap", "影刃残片");
        add("item.epca.small_item_frame", "小型物品过滤展示框");

        // 方块
        add("block.epca.infested_dirt", "虫染泥土");
        add("block.epca.infested_sand", "虫染沙子");
        add("block.epca.infested_leaves", "虫染树叶");
        add("block.epca.infested_flowering_leaves", "虫染盛开的树叶");
        add("block.epca.infested_vine", "虫染藤蔓");
        add("block.epca.infested_log", "虫染原木");
        add("block.epca.infested_wood", "虫染木头");
        add("block.epca.infested_stripped_log", "虫染去皮原木");
        add("block.epca.infested_stripped_wood", "虫染去皮木头");
        add("block.epca.infested_planks", "虫染木板");
        add("block.epca.infested_planks_stairs", "虫染木板楼梯");
        add("block.epca.infested_planks_slab", "虫染木板台阶");
        add("block.epca.infested_residue", "虫染残渣");
        add("block.epca.infested_grass", "虫染草");
        add("block.epca.infested_fern", "虫染蕨");
        add("block.epca.infested_sweet_berry_bush", "虫染甜浆果丛");
        add("block.epca.infested_remains_small", "小堆虫染遗骸");
        add("block.epca.infested_remains_medium", "中堆虫染遗骸");
        add("block.epca.infested_remains_large", "大堆虫染遗骸");
        add("block.epca.infested_stone", "虫染石头");
        add("block.epca.infested_stone_slab", "虫染石头台阶");
        add("block.epca.infested_stone_stairs", "虫染石头楼梯");
        add("block.epca.infested_stone_wall", "虫染石头墙");
        add("block.epca.infested_cobblestone", "虫染圆石");
        add("block.epca.infested_cobblestone_slab", "虫染圆石台阶");
        add("block.epca.infested_cobblestone_stairs", "虫染圆石楼梯");
        add("block.epca.infested_cobblestone_wall", "虫染圆石墙");
        add("block.epca.infested_stone_bricks", "虫染石砖");
        add("block.epca.infested_stone_bricks_slab", "虫染石砖台阶");
        add("block.epca.infested_stone_bricks_stairs", "虫染石砖楼梯");
        add("block.epca.infested_stone_bricks_wall", "虫染石砖墙");
        add("block.epca.infested_cracked_stone_bricks", "虫染裂纹石砖");
        add("block.epca.infested_chiseled_stone_bricks", "虫染雕纹石砖");
        add("block.epca.infested_polished_stone", "虫染磨制石头");
        add("block.epca.infested_polished_stone_slab", "虫染磨制石头台阶");
        add("block.epca.infested_polished_stone_stairs", "虫染磨制石头楼梯");
        add("block.epca.infested_sandstone", "虫染砂岩");
        add("block.epca.infested_sandstone_slab", "虫染砂岩台阶");
        add("block.epca.infested_sandstone_stairs", "虫染砂岩楼梯");
        add("block.epca.infested_sandstone_wall", "虫染砂岩墙");
        add("block.epca.infested_chiseled_sandstone", "虫染雕纹砂岩");
        add("block.epca.infested_chiseled_red_sandstone", "虫染雕纹红砂岩");
        add("block.epca.infested_smooth_sandstone", "虫染平滑砂岩");
        add("block.epca.infested_smooth_sandstone_slab", "虫染平滑砂岩台阶");
        add("block.epca.infested_smooth_sandstone_stairs", "虫染平滑砂岩楼梯");
        add("block.epca.infested_cut_sandstone", "虫染切制砂岩");
        add("block.epca.infested_cut_sandstone_slab", "虫染切制砂岩台阶");
        add("block.epca.infested_coal_ore", "虫染煤矿石");
        add("block.epca.infested_copper_ore", "虫染铜矿石");
        add("block.epca.infested_iron_ore", "虫染铁矿石");
        add("block.epca.infested_gold_ore", "虫染金矿石");
        add("block.epca.infested_lapis_ore", "虫染青金石矿石");
        add("block.epca.infested_redstone_ore", "虫染红石矿石");
        add("block.epca.infested_emerald_ore", "虫染绿宝石矿石");
        add("block.epca.infested_diamond_ore", "虫染钻石矿石");
        add("block.epca.infested_snow_block", "虫染雪块");
        add("block.epca.infested_snow", "虫染雪");
        add("block.epca.infested_infested_cobblestone", "虫染虫蚀圆石");
        add("block.epca.infested_infested_stone", "虫染虫蚀石头");
        add("block.epca.infested_infested_stone_bricks", "虫染虫蚀石砖");
        add("block.epca.infested_infested_cracked_stone_bricks", "虫染虫蚀裂纹石砖");
        add("block.epca.infested_infested_chiseled_stone_bricks", "虫染虫蚀雕纹石砖");
        add("block.epca.packed_mud_pedestal", "泥坯祭台");
        add("block.epca.packed_mud_altar_stone", "泥坯祭坛石");
        add("block.epca.swallow_cyst", "吞食囊包");
        add("block.epca.infested_nethersea_brand_grown", "虫染涌动的溟痕");
        add("block.epca.infested_nethersea_brand_solid", "虫染深蚀的溟痕");
        add("block.epca.infested_planks_fence", "虫染木栅栏");
        add("block.epca.infested_pointed_dripstone", "虫染滴水石锥");
        add("block.epca.beckon_core", "召唤柱核心");
        add("block.epca.infested_heavy_stone", "虫染重质石头");
        add("block.epca.infested_infested_heavy_stone", "虫染虫蚀重质石头");
        add("block.epca.infested_heavy_coal_ore", "虫染重质煤矿石");
        add("block.epca.infested_heavy_copper_ore", "虫染重质铜矿石");
        add("block.epca.infested_heavy_iron_ore", "虫染重质铁矿石");
        add("block.epca.infested_heavy_gold_ore", "虫染重质金矿石");
        add("block.epca.infested_heavy_lapis_ore", "虫染重质青金石矿石");
        add("block.epca.infested_heavy_redstone_ore", "虫染重质红石矿石");
        add("block.epca.infested_heavy_emerald_ore", "虫染重质绿宝石矿石");
        add("block.epca.infested_heavy_diamond_ore", "虫染重质钻石矿石");
        add("block.epca.infested_dustlike", "虫染类尘");
        add("block.epca.infested_plankslike", "虫染类板");
        add("block.epca.infested_rocklike", "虫染类岩");
        add("block.epca.infested_metallike", "虫染类钢");
        add("block.epca.infested_hardlike", "虫染类固");
        add("block.epca.infested_heavy_cobblestone", "虫染重质圆石");
        add("block.epca.infested_heavy_cobblestone_stairs", "虫染重质圆石楼梯");
        add("block.epca.infested_heavy_cobblestone_slab", "虫染重质圆石台阶");
        add("block.epca.infested_heavy_cobblestone_wall", "虫染重质圆石墙");
        add("block.epca.infested_chiseled_deepslate", "虫染雕纹深板岩");
        add("block.epca.infested_polished_heavy_stone", "虫染磨制重质石头");
        add("block.epca.infested_polished_heavy_stone_stairs", "虫染磨制重质石头楼梯");
        add("block.epca.infested_polished_heavy_stone_slab", "虫染磨制重质石头台阶");
        add("block.epca.infested_polished_heavy_stone_wall", "虫染磨制重质石头墙");
        add("block.epca.infested_heavy_bricks", "虫染重质砖");
        add("block.epca.infested_cracked_heavy_bricks", "虫染裂纹重质砖");
        add("block.epca.infested_heavy_bricks_stairs", "虫染重质砖楼梯");
        add("block.epca.infested_heavy_bricks_slab", "虫染重质砖台阶");
        add("block.epca.infested_heavy_bricks_wall", "虫染重质砖墙");
        add("block.epca.infested_lily_pad", "虫染睡莲");
        add("block.epca.infested_carved_pumpkin", "虫染雕刻南瓜");
        add("block.epca.infested_pumpkin", "虫染南瓜");
        add("block.epca.infested_short_grass", "虫染矮草丛");
        add("block.epca.infested_tall_grass", "虫染高草丛");
        add("block.epca.infested_tall_fern", "虫染大型蕨");
        add("block.epca.infested_cactus", "虫染仙人掌");
        add("block.epca.infested_sugar_cane", "虫染甘蔗");
        add("block.epca.acid_solution", "酸液");
        add("fluid_type.epca.acid_solution", "酸液");

        // 命令
        add("commands.negativedamage.damage.success", "成功使用 %3$s 对 %1$s 造成了 %2$s 点伤害");
        add("commands.negativedamage.damage.success.unknown", "成功对 %1$s 造成了 %2$s 点伤害");
        add("commands.negativedamage.damage.success.simple", "对 %1$s 造成了 %2$s 点伤害");
        add("commands.negativedamage.damage.failed", "无法使用 %2$s 伤害 %1$s（实体可能免疫伤害）");
        add("commands.negativedamage.damage.failed.unknown", "无法伤害 %1$s（实体可能免疫伤害）");
        add("commands.negativedamage.damage.failed.simple", "无法伤害 %1$s（实体可能免疫伤害）");
        add("commands.negativedamage.heal.success", "成功使用 %3$s 对 %1$s 造成了 %2$s 点伤害");
        add("commands.negativedamage.heal.success.unknown", "成功对 %1$s 造成了 %2$s 点伤害");
        add("commands.negativedamage.heal.success.simple", "对 %1$s 造成了 %2$s 点伤害");
        add("commands.negativedamage.no_entities", "未找到有效的生物实体");

        // 世界阶段
        add("epca.stage.-2", "世界结界完好无损");
        add("epca.stage.-1", "世界结界完好无损");
        add("epca.stage.0", "世界结界出现松动...");
        add("epca.stage.1", "世界结界完整度90%");
        add("epca.stage.2", "世界结界完整度80%");
        add("epca.stage.3", "世界结界完整度70%");
        add("epca.stage.4", "世界结界完整度60%");
        add("epca.stage.5", "世界结界完整度50%");
        add("epca.stage.6", "世界结界完整度40%");
        add("epca.stage.7", "世界结界完整度30%");
        add("epca.stage.8", "世界结界完整度20%");
        add("epca.stage.9", "世界结界完整度10%");
        add("epca.stage.10", "世界结界完整度1%");
        add("epca.stage.11", "世界结界完整度<1%");
        add("epca.stage.12", "世界结界完整度<1%");
        add("epca.stage.13", "世界结界已完全损坏...");

        // 难度
        add("epca.difficulty.button", "额外难度 - %s");
        add("epca.difficulty.easy", "简单");
        add("epca.difficulty.normal", "普通");
        add("epca.difficulty.expert", "专家");
        add("epca.difficulty.master", "大师");
        add("epca.difficulty.custom", "自定义");
        add("epca.difficulty.legendary", "§5传说");

        // 笔记
        add("epca.note.title", "寄巢笔记");
        add("epca.message.stage_too_low", "这里的侵蚀阶段小于3级...");

        // 图鉴内容（含换行与图像占位符）
        add("epca.content.onesent",
                "§l§0初探种§r\n$[page]$\n§0诅虫\n${img:epca:textures/gui/note_pic/curbug0.png,64}$\n§r上图中是一只诅虫。");
        add("epca.content.test3",
                "\n${img:epca:textures/gui/note_pic/yawning_neko.png,512}$\n作者\n${img:epca:textures/gui/note_pic/xiao_ku_kmc.png,512}$\n美术\n${img:epca:textures/gui/note_pic/thomas_lovlin.png,512}$\n音效师");
    }
}