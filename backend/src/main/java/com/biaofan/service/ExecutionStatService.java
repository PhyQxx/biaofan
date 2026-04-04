package com.biaofan.service;

import com.biaofan.entity.ExecutionStat;
import java.util.List;

public interface ExecutionStatService {
    List<ExecutionStat> getStats(Long userId);
    List<ExecutionStat> getGlobalStats();
}
