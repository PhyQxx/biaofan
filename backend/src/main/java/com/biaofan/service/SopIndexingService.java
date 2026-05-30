package com.biaofan.service;

import com.biaofan.entity.Sop;

/**
 * SOP 向量索引服务
 * 负责将 SOP 内容分块、向量化并存入向量数据库
 */
public interface SopIndexingService {

    /**
     * 索引单个 SOP
     * @param sopId SOP ID
     */
    void indexSop(Long sopId);

    /**
     * 移除 SOP 的所有索引
     * @param sopId SOP ID
     */
    void unindexSop(Long sopId);
    
    /**
     * 重新索引所有已发布的 SOP（后台维护任务）
     */
    void reindexAll();
}
