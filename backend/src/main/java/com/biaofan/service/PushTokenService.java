package com.biaofan.service;

import com.biaofan.entity.PushToken;
import com.biaofan.mapper.PushTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * 推送Token服务
 * 管理极光等第三方推送平台的客户端标识（CID），支持用户设备的推送注册
 */
@Service
@RequiredArgsConstructor
public class PushTokenService {

    private final PushTokenMapper pushTokenMapper;

    /**
     * 注册/更新推送CID
     * 同一用户多次注册则更新CID，确保推送能送达最新设备
     * @param userId 用户ID
     * @param clientId 极光等推送平台的客户端ID（CID）
     * @param platform 平台类型（如android、ios）
     */
    public void registerToken(Long userId, String clientId, String platform) {
        PushToken existing = pushTokenMapper.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();

        if (existing != null) {
            existing.setClientId(clientId);
            existing.setPlatform(platform);
            existing.setUpdatedAt(now);
            pushTokenMapper.updateById(existing);
        } else {
            PushToken token = new PushToken();
            token.setUserId(userId);
            token.setClientId(clientId);
            token.setPlatform(platform);
            token.setCreatedAt(now);
            token.setUpdatedAt(now);
            pushTokenMapper.insert(token);
        }
    }
}
