package com.biaofan.service;

import java.util.List;
import java.util.Map;

/**
 * 积分与成就服务接口
 * 提供用户成长体系、积分、徽章、排行榜等 gamification 功能
 */
public interface GamificationService {
    /**
     * 获取用户成长档案
     * @param userId 用户ID
     * @return 包含等级、积分、进度等信息的Map
     */
    Map<String, Object> getProfile(Long userId);

    /**
     * 获取用户的徽章列表
     * @param userId 用户ID
     * @return 徽章信息列表
     */
    List<Map<String, Object>> getBadges(Long userId);

    /**
     * 获取徽章详情
     * @param userId 用户ID
     * @param badgeId 徽章ID
     * @return 徽章详情信息
     */
    Map<String, Object> getBadgeDetail(Long userId, Long badgeId);

    /**
     * 获取排行榜
     * @param period 周期（如daily、weekly、monthly、all）
     * @return 排行榜列表
     */
    List<Map<String, Object>> getLeaderboard(String period);

    /**
     * 获取排行榜概览
     * @return 包含当前排名、人数等概览信息
     */
    Map<String, Object> getLeaderboardOverview();

    /**
     * 获取用户积分
     * @param userId 用户ID
     * @return 积分信息
     */
    Map<String, Object> getScore(Long userId);

    /**
     * 获取积分历史
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 积分变动历史列表
     */
    List<Map<String, Object>> getScoreHistory(Long userId, int page, int size);

    /**
     * 兑换商品
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 兑换结果信息
     */
    Map<String, Object> redeemProduct(Long userId, Long productId);

    /**
     * 获取积分商城商品列表
     * @param page 页码
     * @param size 每页数量
     * @return 商品列表及分页信息
     */
    Map<String, Object> getStore(int page, int size);

    /**
     * 获取用户成长进度
     * @param userId 用户ID
     * @return 进度信息（当前等级、距离下一级所需等）
     */
    Map<String, Object> getProgress(Long userId);

    /**
     * 获取等级排行榜
     * @return 按等级排序的用户列表
     */
    List<Map<String, Object>> getLevelRanking();

    /**
     * SOP执行完成事件触发
     * 记录用户完成SOP的积分/成就奖励
     * @param userId 用户ID
     * @param sopId SOP ID
     */
    void onExecutionCompleted(Long userId, Long sopId);
}
