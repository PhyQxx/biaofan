package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.ai.AiModel;
import com.biaofan.ai.AiModelFactory;
import com.biaofan.ai.AiResult;
import com.biaofan.ai.VectorStore;
import com.biaofan.entity.AiModelConfig;
import com.biaofan.entity.Sop;
import com.biaofan.mapper.SopMapper;
import com.biaofan.service.AiService;
import com.biaofan.service.SopIndexingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SopIndexingServiceImpl implements SopIndexingService {

    private final SopMapper sopMapper;
    private final AiService aiService;
    private final AiModelFactory aiModelFactory;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void indexSop(Long sopId) {
        Sop sop = sopMapper.selectById(sopId);
        if (sop == null || !"published".equals(sop.getStatus())) {
            log.warn("SOP {} 不存在或未发布，跳过索引", sopId);
            return;
        }

        log.info("开始为 SOP {} 进行向量索引", sopId);
        // 1. 先删除旧索引
        vectorStore.deleteBySopId(sopId);

        // 2. 分块
        List<String> chunks = chunkSop(sop);

        // 3. 向量化并存储
        AiModelConfig config = aiService.getEffectiveConfig(sop.getUserId());
        AiModel model = aiModelFactory.getModel(config.getModelType());

        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);
            AiResult result = model.getEmbedding(content, config);
            
            if (result.isSuccess()) {
                String chunkId = "sop_" + sopId + "_chunk_" + i;
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("sopId", sopId);
                metadata.put("orgId", sop.getOrgId());
                metadata.put("title", sop.getTitle());
                metadata.put("content", content);
                
                vectorStore.save(chunkId, result.getEmbedding(), metadata);
            } else {
                log.error("SOP {} 分块 {} 向量化失败: {}", sopId, i, result.getError());
            }
        }
        log.info("SOP {} 索引完成，共 {} 个分块", sopId, chunks.size());
    }

    @Override
    public void unindexSop(Long sopId) {
        vectorStore.deleteBySopId(sopId);
    }

    @Override
    @Async
    public void reindexAll() {
        List<Sop> sops = sopMapper.selectList(new LambdaQueryWrapper<Sop>()
                .eq(Sop::getStatus, "published"));
        log.info("开始全量重新索引，共 {} 个 SOP", sops.size());
        for (Sop sop : sops) {
            indexSop(sop.getId());
        }
    }

    /**
     * 将 SOP 内容切分为分块
     * 策略：按标题+描述作为一个块，每个步骤作为一个块
     */
    private List<String> chunkSop(Sop sop) {
        List<String> chunks = new ArrayList<>();
        
        // 块 0：元数据
        StringBuilder meta = new StringBuilder();
        meta.append("SOP 标题：").append(sop.getTitle()).append("\n");
        meta.append("描述：").append(sop.getDescription()).append("\n");
        meta.append("分类：").append(sop.getCategory());
        chunks.add(meta.toString());

        // 解析步骤
        try {
            JsonNode root = objectMapper.readTree(sop.getContent());
            if (root.isArray()) {
                for (JsonNode step : root) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("SOP [").append(sop.getTitle()).append("] 步骤：");
                    sb.append(step.get("title").asText()).append("\n");
                    sb.append("详情：").append(step.get("description").asText());
                    
                    JsonNode checks = step.get("checkItems");
                    if (checks != null && checks.isArray()) {
                        sb.append("\n检查项：");
                        for (JsonNode check : checks) {
                            sb.append("- ").append(check.asText()).append(" ");
                        }
                    }
                    chunks.add(sb.toString());
                }
            }
        } catch (Exception e) {
            log.warn("SOP {} 内容解析失败，回退至全文索引", sop.getId());
            chunks.add(sop.getContent());
        }
        
        return chunks;
    }
}
