package com.biaofan.ai.impl;

import com.biaofan.ai.VectorStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 基于 MySQL 的向量存储实现 (Fallback)
 * 使用 JSON 存储向量，通过余弦相似度进行检索
 * 注意：大数据量下性能有限，推荐升级至 RedisSearch 或 PGVector
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseVectorStore implements VectorStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(String id, List<Double> vector, Map<String, Object> metadata) {
        try {
            String vectorJson = objectMapper.writeValueAsString(vector);
            String metadataJson = objectMapper.writeValueAsString(metadata);
            Long sopId = ((Number) metadata.getOrDefault("sopId", 0L)).longValue();
            Long orgId = ((Number) metadata.getOrDefault("orgId", 0L)).longValue();
            String content = (String) metadata.getOrDefault("content", "");

            String sql = "REPLACE INTO sop_vector_store (id, sop_id, org_id, content, vector, metadata) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, id, sopId, orgId, content, vectorJson, metadataJson);
        } catch (Exception e) {
            log.error("Failed to save vector to database", e);
        }
    }

    @Override
    public List<SearchResult> search(List<Double> queryVector, Long orgId, int topK) {
        // 在 MySQL 中通过数学运算计算余弦相似度
        // Cosine Similarity = (A · B) / (||A|| * ||B||)
        // 简化版：如果向量已归一化，直接计算点积即可
        
        String sql = "SELECT id, content, metadata, vector FROM sop_vector_store WHERE org_id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, orgId);
        
        List<SearchResult> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            try {
                String vectorJson = (String) row.get("vector");
                List<Double> vector = objectMapper.readValue(vectorJson, new TypeReference<List<Double>>() {});
                
                double score = calculateCosineSimilarity(queryVector, vector);
                
                String metadataJson = (String) row.get("metadata");
                Map<String, Object> metadata = objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
                
                results.add(SearchResult.builder()
                        .id((String) row.get("id"))
                        .content((String) row.get("content"))
                        .score(score)
                        .metadata(metadata)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse vector or metadata for id: {}", row.get("id"));
            }
        }
        
        // 按分数排序并截取 topK
        results.sort((a, b) -> b.getScore().compareTo(a.getScore()));
        return results.size() > topK ? results.subList(0, topK) : results;
    }

    @Override
    public void deleteBySopId(Long sopId) {
        jdbcTemplate.update("DELETE FROM sop_vector_store WHERE sop_id = ?", sopId);
    }

    private double calculateCosineSimilarity(List<Double> vec1, List<Double> vec2) {
        if (vec1.size() != vec2.size()) return 0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            normA += Math.pow(vec1.get(i), 2);
            normB += Math.pow(vec2.get(i), 2);
        }
        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
