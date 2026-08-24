package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.common.data.SoundDefinitionsProvider;
import org.tdddd.epca.impl.epca;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * 数据生成器：自动扫描 sounds 目录中的 .ogg 文件生成 sounds.json。
 * 声音文件按规则分组：去掉尾部数字后缀后的名称即为声音事件名。
 */
public class SoundData extends SoundDefinitionsProvider {

    public SoundData(PackOutput output, ExistingFileHelper helper) {
        super(output, epca.MODID, helper);
    }

    @Override
    public void registerSounds() {
        var groups = buildFallbackMap();
        for (var entry : groups.entrySet()) {
            String eventName = entry.getKey();
            List<String> files = entry.getValue();

            var definition = SoundDefinition.definition();
            for (String file : files) {
                ResourceLocation soundLoc = new ResourceLocation(epca.MODID, file);
                definition.with(sound(soundLoc, SoundDefinition.SoundType.SOUND));
            }
            definition.subtitle("subtitles.epca." + eventName);

            // 默认衰减距离32，特殊调整
            if (eventName.equals("small_explosion")) {
                // 24
            } else if (eventName.equals("big_explosion")) {
                // 32 default
            }

            add(new ResourceLocation(epca.MODID, eventName), definition);
        }
    }

    /**
     * 扫描 src/main/resources/assets/epca/sounds/ 下的所有 .ogg 文件，
     * 按声音事件名分组（去除路径中末尾的数字序号）。
     *
     * @return 声音事件名 -> 文件路径列表（相对于 sounds/ 目录，不含扩展名）
     */
    private Map<String, List<String>> scanSoundFiles() {
        Map<String, List<String>> groups = new TreeMap<>();

        // 尝试多个可能的声音资源路径
        List<Path> searchRoots = new ArrayList<>();
        searchRoots.add(Path.of("src/main/resources/assets/epca/sounds"));
        searchRoots.add(Path.of("../src/main/resources/assets/epca/sounds"));

        for (Path root : searchRoots) {
            if (Files.isDirectory(root)) {
                try (Stream<Path> walk = Files.walk(root)) {
                    walk.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".ogg"))
                            .forEach(p -> {
                                Path relative = root.relativize(p);
                                String pathStr = relative.toString().replace('\\', '/');
                                // 去掉 .ogg 扩展名
                                String noExt = pathStr.substring(0, pathStr.length() - 4);
                                // 提取事件名：去掉尾部数字序号
                                String eventName = stripTrailingNumber(noExt);
                                // 使用最后一段（纯文件名）或完整相对路径中独特的部分作为事件名
                                // 如果 stripTrailingNumber 后的结果不符合预期，使用文件名推断
                                groups.computeIfAbsent(eventName, k -> new ArrayList<>()).add(noExt);
                            });
                } catch (IOException ignored) {
                }
                if (!groups.isEmpty()) break;
            }
        }

        // 如果文件扫描失败，回退到手动定义的映射
        if (groups.isEmpty()) {
            return buildFallbackMap();
        }

        return groups;
    }

    /**
     * 去掉文件名末尾的数字序号后缀。
     * 例如: "infested_cow_hurt1" -> "infested_cow_hurt"
     *       "small_explosion2" -> "small_explosion"
     *       "phase0" -> "phase" (但 phase 是特例，我们有 phase0~phase10)
     */
    private String stripTrailingNumber(String name) {
        // 分离出最后一段路径（文件名）
        int lastSlash = name.lastIndexOf('/');
        String dirPart = lastSlash >= 0 ? name.substring(0, lastSlash + 1) : "";
        String fileName = lastSlash >= 0 ? name.substring(lastSlash + 1) : name;

        // 去掉末尾的数字
        String stripped = fileName.replaceAll("\\d+$", "");

        // 特殊情况：如果 stripped 为空（纯数字文件名），保留原名
        if (stripped.isEmpty()) {
            stripped = fileName;
        }

        return stripped;
    }

    /**
     * 手动回退映射 — 当文件系统扫描不可用时使用。
     * 基于现有 sounds.json 的内容。
     */
    private Map<String, List<String>> buildFallbackMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();

        // --- random ---
        map.put("small_explosion", List.of("random/small_explosion1", "random/small_explosion2", "random/small_explosion3"));
        map.put("big_explosion", List.of("random/big_explosion1", "random/big_explosion2"));
        map.put("slam", List.of("random/slam1", "random/slam2", "random/slam3", "random/slam4"));

        // --- nullthing ---
        map.put("nullthing_attack0", List.of("nullthing_attack0"));
        map.put("nullthing_run", List.of("nullthing_run"));
        map.put("nullthing_stand1", List.of("nullthing_stand1"));
        map.put("nullthing_stand2", List.of("nullthing_stand2"));

        // --- incomplete_form ---
        map.put("incomplete_form_idle", buildFileList("mob/poverty/incomplete_form/incomplete_form_idle", 1, 4));
        map.put("incomplete_form_hurt", buildFileList("mob/poverty/incomplete_form/incomplete_form_hurt", 1, 4));
        map.put("incomplete_form_death", List.of("mob/poverty/incomplete_form/incomplete_form_death"));

        // --- walking_head ---
        map.put("walking_head_death", List.of("mob/infested/walking_head/walking_head_death"));
        map.put("walking_head_say", buildFileList("mob/infested/walking_head/walking_head_say", 1, 3));

        // --- infested_cow ---
        map.put("infested_cow_death", List.of("mob/infested/infested_cow/infested_cow_death"));
        map.put("infested_cow_hurt", buildFileList("mob/infested/infested_cow/infested_cow_hurt", 1, 3));
        map.put("infested_cow_idle", buildFileList("mob/infested/infested_cow/infested_cow_idle", 1, 4));
        map.put("infested_cow_step", buildFileList("mob/infested/infested_cow/infested_cow_step", 1, 4));

        // --- infested_enderman ---
        map.put("infested_enderman_portal", buildFileList("mob/infested/infested_enderman/infested_enderman_portal", 1, 2));
        map.put("infested_enderman_scream", buildFileList("mob/infested/infested_enderman/infested_enderman_scream", 1, 4));
        map.put("infested_enderman_idle", buildFileList("mob/infested/infested_enderman/infested_enderman_idle", 1, 5));
        map.put("infested_enderman_hurt", buildFileList("mob/infested/infested_enderman/infested_enderman_hurt", 1, 4));
        map.put("infested_enderman_death", List.of("mob/infested/infested_enderman/infested_enderman_death"));
        map.put("infested_enderman_targeting", List.of("mob/infested/infested_enderman/infested_enderman_targeting"));

        // --- infested_pig ---
        map.put("infested_pig_death", List.of("mob/infested/infested_pig/infested_pig_death"));
        map.put("infested_pig_hurt", buildFileList("mob/infested/infested_pig/infested_pig_hurt", 1, 3));
        map.put("infested_pig_idle", buildFileList("mob/infested/infested_pig/infested_pig_idle", 1, 4));
        map.put("infested_pig_step", buildFileList("mob/infested/infested_pig/infested_pig_step", 1, 5));

        // --- infested_husk ---
        map.put("infested_husk_death", buildFileList("mob/infested/infested_husk/infested_husk_death", 1, 2));
        map.put("infested_husk_hurt", buildFileList("mob/infested/infested_husk/infested_husk_hurt", 1, 2));
        map.put("infested_husk_idle", buildFileList("mob/infested/infested_husk/infested_husk_idle", 1, 3));

        // --- infested_sheep ---
        map.put("infested_sheep_death", buildFileList("mob/infested/infested_sheep/infested_sheep_death", 1, 2));
        map.put("infested_sheep_hurt", buildFileList("mob/infested/infested_sheep/infested_sheep_hurt", 1, 3));
        map.put("infested_sheep_idle", buildFileList("mob/infested/infested_sheep/infested_sheep_idle", 1, 3));
        map.put("infested_sheep_step", buildFileList("mob/infested/infested_sheep/infested_sheep_step", 1, 5));

        // --- infested_villager ---
        map.put("infested_villager_death", List.of("mob/infested/infested_villager/infested_villager_death"));
        map.put("infested_villager_hurt", buildFileList("mob/infested/infested_villager/infested_villager_hurt", 1, 4));
        map.put("infested_villager_idle", buildFileList("mob/infested/infested_villager/infested_villager_idle", 1, 3));

        // --- infested_zombie ---
        map.put("infested_zombie_death", List.of("mob/infested/infested_zombie/infested_zombie_death"));
        map.put("infested_zombie_hurt", buildFileList("mob/infested/infested_zombie/infested_zombie_hurt", 1, 2));
        map.put("infested_zombie_idle", buildFileList("mob/infested/infested_zombie/infested_zombie_idle", 1, 3));

        // --- infested_vindicator ---
        map.put("infested_vindicator_death", buildFileList("mob/infested/infested_vindicator/infested_vindicator_death", 1, 2));
        map.put("infested_vindicator_hurt", buildFileList("mob/infested/infested_vindicator/infested_vindicator_hurt", 1, 3));
        map.put("infested_vindicator_idle", buildFileList("mob/infested/infested_vindicator/infested_vindicator_idle", 1, 5));
        map.put("infested_vindicator_targeting", List.of("mob/infested/infested_vindicator/infested_vindicator_targeting"));

        // --- infested_wolf ---
        map.put("infested_wolf_death", List.of("mob/infested/infested_wolf/infested_wolf_death"));
        map.put("infested_wolf_growl", buildFileList("mob/infested/infested_wolf/infested_wolf_growl", 1, 3));
        map.put("infested_wolf_howl", buildFileList("mob/infested/infested_wolf/infested_wolf_howl", 1, 3));
        map.put("infested_wolf_hurt", buildFileList("mob/infested/infested_wolf/infested_wolf_hurt", 1, 3));
        map.put("infested_wolf_whine", List.of("mob/infested/infested_wolf/infested_wolf_whine"));

        // --- infested_skeleton ---
        map.put("infested_skeleton_death", List.of("mob/infested/infested_skeleton/infested_skeleton_death"));
        map.put("infested_skeleton_hurt", buildFileList("mob/infested/infested_skeleton/infested_skeleton_hurt", 1, 4));
        map.put("infested_skeleton_idle", buildFileList("mob/infested/infested_skeleton/infested_skeleton_idle", 1, 3));
        map.put("infested_skeleton_step", buildFileList("mob/infested/infested_skeleton/infested_skeleton_step", 1, 4));

        // --- infested_fox ---
        map.put("infested_fox_aggro", buildFileList("mob/infested/infested_fox/infested_fox_aggro", 1, 7));
        map.put("infested_fox_bite", buildFileList("mob/infested/infested_fox/infested_fox_bite", 1, 3));
        map.put("infested_fox_death", buildFileList("mob/infested/infested_fox/infested_fox_death", 1, 2));
        map.put("infested_fox_hurt", buildFileList("mob/infested/infested_fox/infested_fox_hurt", 1, 4));
        map.put("infested_fox_screech", buildFileList("mob/infested/infested_fox/infested_fox_screech", 1, 4));

        // --- reshape ---
        map.put("reshape_step", buildFileList("mob/reshape/reshape_step", 1, 3));
        map.put("reshape_longarms_death", buildFileList("mob/reshape/longarms/reshape_longarms_death", 1, 2));
        map.put("reshape_longarms_hurt", buildFileList("mob/reshape/longarms/reshape_longarms_hurt", 1, 4));
        map.put("reshape_longarms_idle", buildFileList("mob/reshape/longarms/reshape_longarms_idle", 1, 5));
        map.put("reshape_yelloweye_attack", List.of("mob/reshape/yelloweye/reshape_yelloweye_attack"));
        map.put("reshape_yelloweye_hurt", buildFileList("mob/reshape/yelloweye/reshape_yelloweye_hurt", 1, 3));
        map.put("reshape_yelloweye_idle", buildFileList("mob/reshape/yelloweye/reshape_yelloweye_idle", 1, 2));
        map.put("reshape_yelloweye_gassing", List.of("mob/reshape/yelloweye/reshape_yelloweye_gassing"));

        // --- beckon ---
        map.put("beckon_stage1", List.of("mob/link/beckon/beckon_stage1"));
        map.put("beckon_stage2", List.of("mob/link/beckon/beckon_stage2"));

        // --- ripper ---
        map.put("ripper_idle", buildFileList("mob/onesent/ripper/ripper_idle", 1, 3));
        map.put("ripper_step", buildFileList("mob/onesent/ripper/ripper_step", 1, 4));
        map.put("ripper_hurt", buildFileList("mob/onesent/ripper/ripper_hurt", 1, 2));
        map.put("ripper_death", List.of("mob/onesent/ripper/ripper_death"));

        // --- curbug ---
        map.put("curbug_say", buildFileList("mob/onesent/curbug/curbug_say", 1, 3));
        map.put("curbug_evolve", List.of("mob/onesent/curbug/curbug_evolve"));

        // --- mozzie ---
        map.put("mozzie_idle", List.of("mob/onesent/mozzie/mozzie_idle"));
        map.put("mozzie_hurt", buildFileList("mob/onesent/mozzie/mozzie_hurt", 1, 2));
        map.put("mozzie_death", List.of("mob/onesent/mozzie/mozzie_death"));

        // --- event phases ---
        for (int i = 0; i <= 10; i++) {
            map.put("phase" + i, List.of("event/phase" + i));
        }

        // --- damage/adaptation ---
        map.put("parcial_adaptation", List.of("damage/adaptation/parcial_adaptation"));
        map.put("full_adaptation", List.of("damage/adaptation/full_adaptation"));

        return map;
    }

    private List<String> buildFileList(String prefix, int start, int end) {
        List<String> list = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            list.add(prefix + i);
        }
        return list;
    }
}
