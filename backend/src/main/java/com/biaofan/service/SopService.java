package com.biaofan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.biaofan.entity.Sop;
import com.biaofan.dto.SopRequest;

public interface SopService {
    IPage<Sop> getMySops(Long userId, int page, int size);
    Sop getById(Long id, Long userId);
    Sop create(Long userId, SopRequest req);
    void update(Long id, Long userId, SopRequest req);
    void delete(Long id, Long userId);
    void publish(Long id, Long userId);
    void publish(Long id, Long userId, String changeSummary);
}
