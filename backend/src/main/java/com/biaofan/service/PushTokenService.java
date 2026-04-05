package com.biaofan.service;

import com.biaofan.entity.PushToken;
import com.biaofan.mapper.PushTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PushTokenService {

    private final PushTokenMapper pushTokenMapper;

    /**
     * 注册/更新推送CID：同一用户多次注册则更新CID
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
