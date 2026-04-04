package com.biaofan.service;

import com.biaofan.entity.Sop;
import com.biaofan.entity.SopExecution;
import java.util.List;

public interface SopExecutionService {
    SopExecution startExecution(Long userId, Long sopId);
    void completeStep(Long userId, Long executionId, int stepIndex, String notes);
    void finishExecution(Long userId, Long executionId);
    List<SopExecution> getMyExecutions(Long userId, String status);
    SopExecution getExecution(Long executionId);
    Sop getSopWithSteps(Long sopId);
}
