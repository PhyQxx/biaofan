package com.biaofan.service;

import com.biaofan.entity.SopInstance;
import java.util.List;

public interface SopInstanceService {
    List<SopInstance> getMyInstances(Long userId, String status);
    SopInstance getInstance(Long instanceId);
    void activateInstance(Long userId, Long instanceId);
    boolean completeStep(Long userId, Long instanceId, int stepIndex, String notes, java.util.Map<String, Object> checkData);
    void finishInstance(Long userId, Long instanceId);
    void generatePeriodicInstances();
    void markOverdueInstances();
}
