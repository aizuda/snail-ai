package com.aizuda.snail.ai.common.util;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 搜索结果重排序工具
 * <p>
 * Lost in the Middle 策略：将高相关性结果分布在首尾，低相关性放在中间，
 * 缓解 LLM 对长上下文中间部分注意力不足的问题。
 *
 * @author opensnail
 * @since 2026-05-05
 */
public final class RagResultReorderUtil {

    private RagResultReorderUtil() {
    }

    /**
     * 按 Lost in the Middle 策略重排：高相关性结果分布到首尾，低相关性置于中间
     *
     * @param ranked 已按相关性降序排列的结果列表
     * @return 重排后的结果列表；元素数不超过 2 时原样返回
     */
    public static List<SearchResult> reorderForLostInTheMiddle(List<SearchResult> ranked) {
        if (ranked == null || ranked.size() <= 2) {
            return ranked;
        }
        List<SearchResult> reordered = new ArrayList<>(ranked.size());
        List<SearchResult> tail = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            if (i % 2 == 0) {
                reordered.add(ranked.get(i));
            } else {
                tail.add(ranked.get(i));
            }
        }
        for (int i = tail.size() - 1; i >= 0; i--) {
            reordered.add(tail.get(i));
        }
        return reordered;
    }
}
