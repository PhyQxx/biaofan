package com.biaofan.service;

import java.util.List;
import java.util.Map;

public interface GamificationService {
    Map<String, Object> getProfile(Long userId);
    List<Map<String, Object>> getBadges(Long userId);
    Map<String, Object> getBadgeDetail(Long userId, Long badgeId);
    List<Map<String, Object>> getLeaderboard(String period);
    Map<String, Object> getLeaderboardOverview();
    Map<String, Object> getScore(Long userId);
    List<Map<String, Object>> getScoreHistory(Long userId, int page, int size);
    Map<String, Object> redeemProduct(Long userId, Long productId);
    List<Map<String, Object>> getStore();
    Map<String, Object> getProgress(Long userId);
    List<Map<String, Object>> getLevelRanking();
    void onExecutionCompleted(Long userId, Long sopId);
}
