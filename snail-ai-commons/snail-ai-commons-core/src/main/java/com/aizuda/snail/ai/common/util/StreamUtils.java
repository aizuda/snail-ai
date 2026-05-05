package com.aizuda.snail.ai.common.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * stream 流工具类 (复制自 snail-job，使用 "" 替代 StringUtils.EMPTY)
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StreamUtils {

    public static final String SEPARATOR = ",";

    public static <E> List<E> filter(Collection<E> collection, Predicate<E> function) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection.stream().filter(function).collect(Collectors.toList());
    }

    public static <E> String join(Collection<E> collection, Function<E, String> function) {
        return join(collection, function, SEPARATOR);
    }

    public static <E> String join(Collection<E> collection, Function<E, String> function, CharSequence delimiter) {
        if (CollUtil.isEmpty(collection)) {
            return "";
        }
        return collection.stream().map(function).filter(Objects::nonNull).collect(Collectors.joining(delimiter));
    }

    public static <E> List<E> sorted(Collection<E> collection, Comparator<E> comparing) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection.stream().filter(Objects::nonNull).sorted(comparing).collect(Collectors.toList());
    }

    public static <V, K> Map<K, V> toIdentityMap(Collection<V> collection, Function<V, K> key) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream().filter(Objects::nonNull).collect(Collectors.toMap(key, Function.identity(), (l, r) -> l));
    }

    public static <E, K, V> Map<K, V> toMap(Collection<E> collection, Function<E, K> key, Function<E, V> value) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream().filter(Objects::nonNull).collect(Collectors.toMap(key, value, (l, r) -> l));
    }

    public static <E, K> Map<K, List<E>> groupByKey(Collection<E> collection, Function<E, K> key) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream().filter(Objects::nonNull)
                .collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
    }

    public static <E, K, U> Map<K, Map<U, List<E>>> groupBy2Key(Collection<E> collection, Function<E, K> key1, Function<E, U> key2) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream().filter(Objects::nonNull)
                .collect(Collectors.groupingBy(key1, LinkedHashMap::new, Collectors.groupingBy(key2, LinkedHashMap::new, Collectors.toList())));
    }

    public static <E, T, U> Map<T, Map<U, E>> group2Map(Collection<E> collection, Function<E, T> key1, Function<E, U> key2) {
        if (CollUtil.isEmpty(collection) || key1 == null || key2 == null) {
            return MapUtil.newHashMap();
        }
        return collection.stream().filter(Objects::nonNull)
                .collect(Collectors.groupingBy(key1, LinkedHashMap::new, Collectors.toMap(key2, Function.identity(), (l, r) -> l)));
    }

    public static <E, T> List<T> toList(Collection<E> collection, Function<E, T> function) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection.stream().map(function).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static <E, T> Set<T> toSet(Collection<E> collection, Function<E, T> function) {
        if (CollUtil.isEmpty(collection) || function == null) {
            return CollUtil.newHashSet();
        }
        return collection.stream().map(function).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public static <K, X, Y, V> Map<K, V> merge(Map<K, X> map1, Map<K, Y> map2, BiFunction<X, Y, V> merge) {
        if (MapUtil.isEmpty(map1) && MapUtil.isEmpty(map2)) {
            return MapUtil.newHashMap();
        } else if (MapUtil.isEmpty(map1)) {
            map1 = MapUtil.newHashMap();
        } else if (MapUtil.isEmpty(map2)) {
            map2 = MapUtil.newHashMap();
        }
        Set<K> key = new HashSet<>();
        key.addAll(map1.keySet());
        key.addAll(map2.keySet());
        Map<K, V> map = new HashMap<>();
        for (K t : key) {
            X x = map1.get(t);
            Y y = map2.get(t);
            V z = merge.apply(x, y);
            if (z != null) {
                map.put(t, z);
            }
        }
        return map;
    }

    public static <T, U, R> Set<R> toSetByFlatMap(Collection<T> collection,
                                                  Function<? super T, ? extends U> mapper,
                                                  Function<U, ? extends Stream<? extends R>> function) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newHashSet();
        }
        return collection.stream().map(mapper).filter(Objects::nonNull)
                .flatMap(function).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public static <T, U> Set<U> toSetByFlatMap(Collection<T> collection,
                                               Function<T, ? extends Stream<? extends U>> function) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newHashSet();
        }
        return collection.stream().filter(Objects::nonNull)
                .flatMap(function).filter(Objects::nonNull).collect(Collectors.toSet());
    }
}
