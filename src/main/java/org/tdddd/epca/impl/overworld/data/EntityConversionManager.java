package org.tdddd.epca.impl.overworld.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityConversionManager implements ResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceLocation, List<EntityConversionRule>> CONVERSION_RULES = new HashMap<>();

    public static class EntityConversionRule {
        public String from;
        public String to;
        public String fins_to;
        public String mozzie_to;
        public boolean small_entity_priority = true;
        public Map<String, Object> nbt_conditions; 
        public int priority = 0; 
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        CONVERSION_RULES.clear();

        resourceManager.listResources("entity_conversions", file -> file.getPath().endsWith(".json"))
                .forEach((resourceLocation, resource) -> {
                    try (InputStream stream = resource.open()) {
                        EntityConversionRule rule = GSON.fromJson(new InputStreamReader(stream), EntityConversionRule.class);
                        ResourceLocation fromLocation = new ResourceLocation(rule.from);
                        
                        CONVERSION_RULES.computeIfAbsent(fromLocation, k -> new ArrayList<>()).add(rule);
                    } catch (Exception e) {
                        e.printStackTrace(); 
                    }
                });

        
        for (List<EntityConversionRule> rules : CONVERSION_RULES.values()) {
            rules.sort(Comparator.comparingInt(rule -> rule.priority));
        }
    }

    public static EntityConversionRule getConversionRule(ResourceLocation entityType, CompoundTag nbt) {
        List<EntityConversionRule> rules = CONVERSION_RULES.get(entityType);
        if (rules != null) {
            
            for (EntityConversionRule rule : rules) {
                if (checkNBTConditions(rule, nbt)) {
                    return rule;
                }
            }
        }
        return null;
    }

    public static EntityConversionRule getConversionRule(EntityType<?> entityType, CompoundTag nbt) {
        return getConversionRule(ForgeRegistries.ENTITY_TYPES.getKey(entityType), nbt);
    }

    
    public static boolean checkNBTConditions(EntityConversionRule rule, CompoundTag entityNBT) {
        if (rule.nbt_conditions == null || rule.nbt_conditions.isEmpty()) {
            return true; 
        }

        if (entityNBT == null) {
            return false; 
        }

        for (Map.Entry<String, Object> condition : rule.nbt_conditions.entrySet()) {
            String nbtPath = condition.getKey();
            Object expectedValue = condition.getValue();

            if (!checkNBTCondition(entityNBT, nbtPath, expectedValue)) {
                return false;
            }
        }

        return true;
    }

    
    private static boolean checkNBTCondition(CompoundTag nbt, String path, Object expectedValue) {
        Tag actualTag = getTagAtPath(nbt, path);
        if (actualTag == null) {
            return false;
        }

        if (expectedValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> conditionMap = (Map<String, Object>) expectedValue;
            return compareTagValueWithOperator(actualTag, conditionMap);
        } else {
            return compareTagValue(actualTag, expectedValue);
        }
    }

    
    private static Tag getTagAtPath(CompoundTag root, String path) {
        String[] parts = path.split("\\.");
        CompoundTag current = root;
        Tag result = null;

        for (String part : parts) {
            if (part.contains("[") && part.contains("]")) {
                
                String key = part.substring(0, part.indexOf('['));
                int index = Integer.parseInt(part.substring(part.indexOf('[') + 1, part.indexOf(']')));
                Tag listTag = current.get(key);
                if (listTag instanceof ListTag list && index >= 0 && index < list.size()) {
                    result = list.get(index);
                    if (result instanceof CompoundTag) {
                        current = (CompoundTag) result;
                    } else {
                        
                        return result;
                    }
                } else {
                    return null;
                }
            } else {
                if (!current.contains(part)) {
                    return null;
                }
                result = current.get(part);
                if (result instanceof CompoundTag) {
                    current = (CompoundTag) result;
                } else {
                    
                    return result;
                }
            }
        }
        return result;
    }

    
    private static boolean compareTagValue(Tag actualTag, Object expectedValue) {
        if (expectedValue == null) {
            return actualTag == null;
        }

        
        if (expectedValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> conditionMap = (Map<String, Object>) expectedValue;
            return compareTagValueWithOperator(actualTag, conditionMap);
        }

        
        switch (actualTag.getId()) {
            case Tag.TAG_BYTE:
                if (expectedValue instanceof Number) {
                    return ((Number) expectedValue).byteValue() == ((net.minecraft.nbt.ByteTag) actualTag).getAsByte();
                }
                break;
            case Tag.TAG_SHORT:
                if (expectedValue instanceof Number) {
                    return ((Number) expectedValue).shortValue() == ((net.minecraft.nbt.ShortTag) actualTag).getAsShort();
                }
                break;
            case Tag.TAG_INT:
                if (expectedValue instanceof Number) {
                    return ((Number) expectedValue).intValue() == ((net.minecraft.nbt.IntTag) actualTag).getAsInt();
                }
                break;
            case Tag.TAG_LONG:
                if (expectedValue instanceof Number) {
                    return ((Number) expectedValue).longValue() == ((net.minecraft.nbt.LongTag) actualTag).getAsLong();
                }
                break;
            case Tag.TAG_FLOAT:
                if (expectedValue instanceof Number) {
                    return Float.compare(((Number) expectedValue).floatValue(), ((net.minecraft.nbt.FloatTag) actualTag).getAsFloat()) == 0;
                }
                break;
            case Tag.TAG_DOUBLE:
                if (expectedValue instanceof Number) {
                    return Double.compare(((Number) expectedValue).doubleValue(), ((net.minecraft.nbt.DoubleTag) actualTag).getAsDouble()) == 0;
                }
                break;
            case Tag.TAG_STRING:
                if (expectedValue instanceof String) {
                    return expectedValue.equals(((net.minecraft.nbt.StringTag) actualTag).getAsString());
                }
                break;
            case Tag.TAG_BYTE_ARRAY:
            case Tag.TAG_INT_ARRAY:
            case Tag.TAG_LONG_ARRAY:
                
                break;
            case Tag.TAG_COMPOUND:
                
                break;
        }

        return false;
    }

    
    private static boolean compareTagValueWithOperator(Tag actualTag, Map<String, Object> conditionMap) {
        String operator = (String) conditionMap.get("operator");
        Object value = conditionMap.get("value");

        if (operator == null || value == null) {
            return false;
        }

        
        switch (operator) {
            case "==":
            case "!=":
            case ">":
            case ">=":
            case "<":
            case "<=":
                
                switch (actualTag.getId()) {
                    case Tag.TAG_BYTE:
                        if (value instanceof Number) {
                            byte actual = ((ByteTag) actualTag).getAsByte();
                            byte expected = ((Number) value).byteValue();
                            return compareWithOperator(actual, expected, operator);
                        }
                        break;
                    case Tag.TAG_SHORT:
                        if (value instanceof Number) {
                            short actual = ((ShortTag) actualTag).getAsShort();
                            short expected = ((Number) value).shortValue();
                            return compareWithOperator(actual, expected, operator);
                        }
                        break;
                    case Tag.TAG_INT:
                        if (value instanceof Number) {
                            int actual = ((IntTag) actualTag).getAsInt();
                            int expected = ((Number) value).intValue();
                            return compareWithOperator(actual, expected, operator);
                        }
                        break;
                    case Tag.TAG_LONG:
                        if (value instanceof Number) {
                            long actual = ((LongTag) actualTag).getAsLong();
                            long expected = ((Number) value).longValue();
                            return compareWithOperator(actual, expected, operator);
                        }
                        break;
                    case Tag.TAG_FLOAT:
                        if (value instanceof Number) {
                            float actual = ((FloatTag) actualTag).getAsFloat();
                            float expected = ((Number) value).floatValue();
                            return compareWithOperator(actual, expected, operator);
                        }
                        break;
                    case Tag.TAG_DOUBLE:
                        if (value instanceof Number) {
                            double actual = ((DoubleTag) actualTag).getAsDouble();
                            double expected = ((Number) value).doubleValue();
                            return compareWithOperator(actual, expected, operator);
                        }
                        break;
                    case Tag.TAG_STRING:
                        if (value instanceof String) {
                            String actual = ((StringTag) actualTag).getAsString();
                            String expected = (String) value;
                            return compareWithOperator(actual, expected, operator);
                        }
                        break;
                    default:
                        return false;
                }
                return false;

            case "contains":
                return checkContains(actualTag, value);

            default:
                System.out.println("Unknown operator: " + operator);
                return false;
        }
    }

    
    private static <T extends Comparable<T>> boolean compareWithOperator(T actual, T expected, String operator) {
        switch (operator) {
            case "==":
                return actual.compareTo(expected) == 0;
            case "!=":
                return actual.compareTo(expected) != 0;
            case ">":
                return actual.compareTo(expected) > 0;
            case ">=":
                return actual.compareTo(expected) >= 0;
            case "<":
                return actual.compareTo(expected) < 0;
            case "<=":
                return actual.compareTo(expected) <= 0;
            default:
                System.out.println("Unknown operator: " + operator);
                return false;
        }
    }

    
    private static boolean checkContains(Tag actualTag, Object expected) {
        if (!(actualTag instanceof ListTag list)) {
            return false;
        }

        for (int i = 0; i < list.size(); i++) {
            Tag element = list.get(i);
            if (expected instanceof Map) {
                
                @SuppressWarnings("unchecked")
                Map<String, Object> expectedMap = (Map<String, Object>) expected;
                if (element instanceof CompoundTag comp) {
                    boolean match = true;
                    for (Map.Entry<String, Object> entry : expectedMap.entrySet()) {
                        Tag child = comp.get(entry.getKey());
                        if (!compareTagValue(child, entry.getValue())) {
                            match = false;
                            break;
                        }
                    }
                    if (match) return true;
                }
            } else {
                
                if (compareTagValue(element, expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    
    public static String getConversionTarget(EntityConversionRule rule, CompoundTag entityNBT) {
        if (!checkNBTConditions(rule, entityNBT)) {
            return null; 
        }
        
        if (rule.to == null || rule.to.isEmpty()) {
            return null;
        }
        
        
        return rule.to; 
    }

    
    public static String getFinsConversionTarget(EntityConversionRule rule, CompoundTag entityNBT) {
        if (!checkNBTConditions(rule, entityNBT)) {
            return null;
        }
        
        if (rule.fins_to == null || rule.fins_to.isEmpty()) {
            return null;
        }

        return rule.fins_to;
    }

    
    public static String getGnatConversionTarget(EntityConversionRule rule, CompoundTag entityNBT) {
        if (!checkNBTConditions(rule, entityNBT)) {
            return null;
        }
        
        if (rule.mozzie_to == null || rule.mozzie_to.isEmpty()) {
            return null;
        }

        return rule.mozzie_to;
    }
}