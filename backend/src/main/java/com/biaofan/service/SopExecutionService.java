package com.biaofan.service;

import com.biaofan.entity.ExecutionStepRecord;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopExecution;

import java.util.List;
import java.util.Map;

public interface SopExecutionService {
    SopExecution startExecution(Long userId, Long sopId);
    boolean completeStep(Long userId, Long executionId, int stepIndex, String notes,
                         Map<String, Object> checkData, String attachments);
    void finishExecution(Long userId, Long executionId);
    List<SopExecution> getMyExecutions(Long userId, String status);
    SopExecution getExecution(Long executionId);
    Sop getSopWithSteps(Long sopId);
    List<ExecutionStepRecord> getStepRecords(Long executionId);
    int getStepCount(Long executionId);
}
