package com.biaofan.ai;

import java.util.List;
import java.util.Map;

/**
 * 向量数据库存储接口
 * 用于存储 SOP 分块向量并进行相似度检索
 */
public interface VectorStore {

    /**
     * 存储向量数据
     * @param id 唯一标识（通常是 chunkId）
     * @param vector 向量数据
     * @param metadata 元数据（如 sopId, content, orgId 等）
     */
    void save(String id, List<Double> vector, Map<String, Object> metadata);

    /**
     * 搜索相似向量
     * @param queryVector 查询向量
     * @param orgId 组织过滤 ID
     * @param topK 返回数量
     * @return 搜索结果列表
     */
    List<SearchResult> search(List<Double> queryVector, Long orgId, int topK);

    /**
     * 删除特定 SOP 的所有向量
     * @param sopId SOP ID
     */
    void deleteBySopId(Long sopId);

    @lombok.Data
    @lombok.Builder
    class SearchResult {
        private String id;
        private Double score;
        private String content;
        private Map<String, Object> metadata;
    }
}
