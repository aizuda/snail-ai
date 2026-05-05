package com.aizuda.snail.ai.common.util;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.aizuda.snail.ai.common.constants.SystemConstants.YYYY_MM_DD;
import static com.aizuda.snail.ai.common.constants.SystemConstants.YYYY_MM_DD_HH_MM_SS;

/**
 * (复制自 snail-job)
 */
public class JsonUtil {

    public static String toJsonString(Object object) {
        return JsonMapper.toJsonString(object);
    }

    public static <T> T parseObject(String jsonString, Class<T> clazz) {
        return JsonMapper.toJavaObject(jsonString, clazz);
    }

    public static <T> T parseObject(String jsonString, Type type) {
        JavaType javaType = JsonMapper.getByType(type);
        return (T) JsonMapper.toJavaObject(jsonString, javaType);
    }

    public static <T> T parseObject(InputStream inputStream, Class<T> clazz) {
        return JsonMapper.toJavaObject(inputStream, clazz);
    }

    public static <T> List<T> parseList(String jsonString, Class<T> clazz) {
        JavaType javaType = JsonMapper.getCollectionType(ArrayList.class, clazz);
        return (List<T>) JsonMapper.toJavaObject(jsonString, javaType);
    }

    public static <K, V> Map<K, V> parseHashMap(String jsonString) {
        return JsonMapper.toJavaObject(jsonString, HashMap.class);
    }

    public static <K, V> Map<K, V> parseConcurrentHashMap(String jsonString) {
        return JsonMapper.toJavaObject(jsonString, ConcurrentHashMap.class);
    }

    public static <T> Map<String, T> parseHashMap(String jsonString, Class<T> clazz) {
        return (Map<String, T>) JsonMapper.toJavaObject(jsonString, new TypeReference<HashMap<String, T>>() {
        });
    }

    public static <T> T parseObject(String jsonString, TypeReference<T> reference) {
        return (T) JsonMapper.toJavaObject(jsonString, reference);
    }

    public static JsonNode toJson(String jsonString) {
        return JsonMapper.toJson(jsonString);
    }

    public static JsonNode toJson(Object object) {
        return JsonMapper.toJson(object);
    }

    public static JsonNode toJson(byte[] jsonBytes) {
        return JsonMapper.toJson(jsonBytes);
    }

    public static boolean isValidJson(String jsonString) {
        try {
            JsonMapper.objectMapper.readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String toJSONString() {
        ObjectNode objectNode = JsonMapper.objectMapper.createObjectNode();
        try {
            return JsonMapper.objectMapper.writeValueAsString(objectNode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 判断 Json 是否为空
     */
    public static boolean isEmptyJson(String json){
        JsonNode jsonNode = JsonMapper.objectMapper.readTree(json);
        return jsonNode.isEmpty();
    }

    public static class JsonMapper {

        private static final ObjectMapper objectMapper = jacksonObjectMapper();

        public static ObjectMapper jacksonObjectMapper() {
            DateTimeFormatter localDateTimeFormatter = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);
            DateTimeFormatter localDateFormatter = DateTimeFormatter.ofPattern(YYYY_MM_DD);
            // 初始化时间序列化模块
            SimpleModule simpleModule = new SimpleModule();
            simpleModule.addSerializer(LocalDate.class, new LocalDateSerializer(localDateFormatter));
            simpleModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(localDateTimeFormatter));
            simpleModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(localDateFormatter));
            simpleModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(localDateTimeFormatter));
            // 初始化全局Jackson 序列化工具
            return tools.jackson.databind.json.JsonMapper.builder()
                    // 忽略未知属性
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    // 忽略无法序列化的属性
                    .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                    // 序列化是忽略空值字段
                    .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                    // 注册时间序列化模块
                    .addModule(simpleModule)
                    .build();
        }
        public static ObjectMapper objectMapper() {
            return objectMapper;
        }

        private static JavaType getByType(Type type) {
            return objectMapper.getTypeFactory().constructType(type);
        }

        private static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
            return objectMapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
        }

        private static String toJsonString(Object object) {
            try {
                return objectMapper.writeValueAsString(object);
            } catch (Exception e) {
                throw new SnailAiCommonException("Object to Json conversion failed!", e);
            }
        }

        private static <T> T toJavaObject(String jsonString, Class<T> clazz) {
            try {
                return objectMapper.readValue(jsonString, clazz);
            } catch (Exception e) {
                throw new SnailAiCommonException("Json to object conversion failed Parameters: {}", jsonString, e);
            }
        }

        public static <T> T toJavaObject(InputStream inputStream, Class<T> clazz) {
            try {
                return objectMapper.readValue(inputStream, clazz);
            } catch (Exception e) {
                throw new SnailAiCommonException("Json to object conversion failed", e);
            }
        }

        public static Object toJavaObject(String jsonString, TypeReference typeReference) {
            try {
                return objectMapper.readValue(jsonString, typeReference);
            } catch (Exception e) {
                throw new SnailAiCommonException("Json to object conversion failed Parameters: {}", jsonString, e);
            }
        }

        private static Object toJavaObject(String jsonString, JavaType javaType) {
            try {
                return objectMapper.readValue(jsonString, javaType);
            } catch (Exception e) {
                throw new SnailAiCommonException("Json to object conversion failed Parameters: {}", jsonString, e);
            }
        }

        private static JsonNode toJson(String jsonString) {
            try {
                return objectMapper.readTree(jsonString);
            } catch (Exception e) {
                throw new SnailAiCommonException("Json to object conversion failed Parameters: {}", jsonString, e);
            }
        }

        private static JsonNode toJson(byte[] jsonBytes) {
            try {
                return objectMapper.readTree(jsonBytes);
            } catch (Exception e) {
                throw new SnailAiCommonException("Json to object conversion failed!", e);
            }
        }

        private static JsonNode toJson(Object object) {
            try {
                return objectMapper.valueToTree(object);
            } catch (Exception e) {
                throw new SnailAiCommonException("Json to object conversion failed!", e);
            }
        }
    }
}
