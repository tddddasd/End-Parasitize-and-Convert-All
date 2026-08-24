package org.tdddd.epca.impl.overworld.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EPCANoteTabData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static List<ParentTab> currentParentTabs = new ArrayList<>();

    private static class TabFile {
        List<ParentTab> parent_tabs;
    }

    public static class ChildTab {
        public String id;
        public String name;
        public String content;

        @SerializedName(value = "required_advancement", alternate = {"requiredAdvancement"})
        public String requiredAdvancement;

        public ChildTab() {}
        public ChildTab(String id, String name, String content, String requiredAdvancement) {
            this.id = id;
            this.name = name;
            this.content = content;
            this.requiredAdvancement = requiredAdvancement;
        }
    }

    public static class ParentTab {
        public String id;
        public String name;
        public List<ChildTab> child_tabs;

        @SerializedName(value = "required_advancement", alternate = {"requiredAdvancement"})
        public String requiredAdvancement;

        public List<ChildTab> getChildTabs() {
            return child_tabs == null ? List.of() : child_tabs;
        }

        public void addChildTab(ChildTab child) {
            if (child_tabs == null) child_tabs = new ArrayList<>();
            child_tabs.add(child);
        }
    }

    
    public static void reloadFromServerResources(MinecraftServer server) {
        ResourceManager resourceManager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources =
                resourceManager.listResources("epca_note", loc -> loc.getPath().endsWith(".json"));

        List<ResourceLocation> sorted = new ArrayList<>(resources.keySet());
        sorted.sort((a, b) -> {
            boolean aIsMain = a.getPath().equals("epca_note/main.json");
            boolean bIsMain = b.getPath().equals("epca_note/main.json");
            if (aIsMain && !bIsMain) return -1;
            if (!aIsMain && bIsMain) return 1;
            return a.toString().compareTo(b.toString());
        });

        Map<String, ParentTab> parentMap = new LinkedHashMap<>();
        List<ParentTab> baseOrder = new ArrayList<>();

        
        for (var loc : sorted) {
            String fileName = loc.getPath().substring(loc.getPath().lastIndexOf('/') + 1);
            if (fileName.endsWith("_add.json")) continue;
            Resource res = resources.get(loc);
            try (InputStreamReader reader = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                TabFile tf = GSON.fromJson(reader, TabFile.class);
                if (tf == null || tf.parent_tabs == null) continue;
                for (ParentTab pt : tf.parent_tabs) {
                    if (pt.id == null || pt.id.isEmpty()) continue;
                    if (!parentMap.containsKey(pt.id)) {
                        if (pt.child_tabs == null) pt.child_tabs = new ArrayList<>();
                        parentMap.put(pt.id, pt);
                        baseOrder.add(pt);
                    }
                }
            } catch (JsonSyntaxException ignored) {
            } catch (Exception ignored) {}
        }

        
        for (var loc : sorted) {
            String fileName = loc.getPath().substring(loc.getPath().lastIndexOf('/') + 1);
            if (!fileName.endsWith("_add.json")) continue;
            Resource res = resources.get(loc);
            try (InputStreamReader reader = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                TabFile tf = GSON.fromJson(reader, TabFile.class);
                if (tf == null || tf.parent_tabs == null) continue;
                for (ParentTab pt : tf.parent_tabs) {
                    if (pt.id == null || pt.id.isEmpty()) continue;
                    ParentTab existing = parentMap.get(pt.id);
                    if (existing != null && pt.child_tabs != null) {
                        for (ChildTab ct : pt.child_tabs) {
                            existing.addChildTab(ct);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        currentParentTabs = Collections.unmodifiableList(baseOrder);
    }

    
    public static void writeParentTabList(FriendlyByteBuf buf, List<ParentTab> list) {
        buf.writeInt(list.size());
        for (ParentTab pt : list) {
            buf.writeUtf(pt.id);
            buf.writeUtf(pt.name);
            buf.writeUtf(pt.requiredAdvancement == null ? "" : pt.requiredAdvancement);
            buf.writeInt(pt.child_tabs == null ? 0 : pt.child_tabs.size());
            if (pt.child_tabs != null) {
                for (ChildTab ct : pt.child_tabs) {
                    buf.writeUtf(ct.id);
                    buf.writeUtf(ct.name);
                    buf.writeUtf(ct.content == null ? "" : ct.content);
                    buf.writeUtf(ct.requiredAdvancement == null ? "" : ct.requiredAdvancement);
                }
            }
        }
    }

    public static List<ParentTab> readParentTabList(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ParentTab> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ParentTab pt = new ParentTab();
            pt.id = buf.readUtf(32767);
            pt.name = buf.readUtf(32767);
            String reqParent = buf.readUtf(32767);
            pt.requiredAdvancement = reqParent.isEmpty() ? null : reqParent;
            int childCount = buf.readInt();
            pt.child_tabs = new ArrayList<>(childCount);
            for (int j = 0; j < childCount; j++) {
                ChildTab ct = new ChildTab();
                ct.id = buf.readUtf(32767);
                ct.name = buf.readUtf(32767);
                ct.content = buf.readUtf(32767);
                String reqChild = buf.readUtf(32767);
                ct.requiredAdvancement = reqChild.isEmpty() ? null : reqChild;
                pt.child_tabs.add(ct);
            }
            list.add(pt);
        }
        return list;
    }

    
    public static List<ParentTab> getCurrentTabs() {
        return currentParentTabs;
    }

    public static void setClientTabs(List<ParentTab> tabs) {
        currentParentTabs = Collections.unmodifiableList(new ArrayList<>(tabs));
    }

    
    @OnlyIn(Dist.CLIENT)
    public static List<ParentTab> getVisibleTabsForPlayer() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return getCurrentTabs();

        ClientAdvancements advancements = null;
        var conn = Minecraft.getInstance().getConnection();
        if (conn != null) advancements = conn.getAdvancements();
        if (advancements == null) return getCurrentTabs();

        List<ParentTab> original = getCurrentTabs();
        List<ParentTab> result = new ArrayList<>();
        for (ParentTab parent : original) {
            if (!isAdvancementCompleted(advancements, parent.requiredAdvancement))
                continue;

            ParentTab visibleParent = new ParentTab();
            visibleParent.id = parent.id;
            visibleParent.name = parent.name;
            visibleParent.requiredAdvancement = parent.requiredAdvancement;
            visibleParent.child_tabs = new ArrayList<>();

            for (ChildTab child : parent.getChildTabs()) {
                if (isAdvancementCompleted(advancements, child.requiredAdvancement)) {
                    visibleParent.child_tabs.add(child);
                }
            }

            boolean originallyHadChildren = parent.child_tabs != null && !parent.child_tabs.isEmpty();
            if (!originallyHadChildren || !visibleParent.child_tabs.isEmpty()) {
                result.add(visibleParent);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean isAdvancementCompleted(ClientAdvancements advancements, String advancementId) {
        if (advancementId == null || advancementId.isEmpty()) return true;
        ResourceLocation id = ResourceLocation.tryParse(advancementId);
        if (id == null) return false;
        Advancement advancement = advancements.getAdvancements().get(id);
        if (advancement == null) return false;

        
        try {
            Field field = ClientAdvancements.class.getDeclaredField("advancements");
            field.setAccessible(true);
            Map<Advancement, AdvancementProgress> progressMap = (Map<Advancement, AdvancementProgress>) field.get(advancements);
            AdvancementProgress progress = progressMap.get(advancement);
            return progress != null && progress.isDone();
        } catch (Exception e) {
            
            return true;
        }
    }
}