package com.biaofan.service;

import com.biaofan.entity.SopVersion;
import java.util.List;

public interface SopVersionService {
    List<SopVersion> getVersions(Long sopId);
    SopVersion getVersion(Long sopId, Integer version);
    void rollback(Long sopId, Long userId, Integer targetVersion);
}
