package com.biaofan.service.impl;


/**
 * 游戏化服务实现（用户端）
 * - 用户积分增减（执行完成奖励、异常扣分等）
 * - 等级计算：根据积分判断用户等级
 * - 徽章判定：检查用户是否满足徽章获取条件
 * - 排行榜查询：按积分降序，支持日/周/月/总榜
 * - 积分历史记录
 */
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.*;
import com.biaofan.mapper.GamificationUserStatsMapper;
import com.biaofan.mapper.GamificationBadgeDefinitionMapper;
import com.biaofan.mapper.GamificationUserBadgeMapper;
import com.biaofan.mapper.GamificationScoreHistoryMapper;
import com.biaofan.mapper.GamificationStoreProductMapper;
import com.biaofan.mapper.GamificationUserProductMapper;
import com.biaofan.mapper.GamificationLeaderboardCacheMapper;
import com.biaofan.mapper.UserMapper;
import com.biaofan.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 积分化服务实现类
 * 提供用户等级、经验值、积分、排行榜、徽章、商城兑换等功能
 * SOP执行完成后自动增加经验和积分，检查并授予徽章
 *
 * @author biaofan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationServiceImpl implements GamificationService {

    private final GamificationUserStatsMapper statsMapper;
    private final GamificationBadgeDefinitionMapper badgeDefMapper;
    private final GamificationUserBadgeMapper userBadgeMapper;
    private final GamificationScoreHistoryMapper scoreHistoryMapper;
    private final GamificationStoreProductMapper storeProductMapper;
    private final GamificationUserProductMapper userProductMapper;
    private final GamificationLeaderboardCacheMapper leaderboardCacheMapper;
    private final UserMapper userMapper;

    /**
     * 计算指定等级所需的经验值
     */
    // === EXP / Level helpers ===
    private static final int MAX_LEVEL = 999;
    private int expForLevel(int level) { return level * 100; }
    
    /**
     * 根据等级获取段位标识
     */
    private String rankForLevel(int level) {
        if (level >= 80) return "diamond";
        if (level >= 60) return "platinum";
        if (level >= 40) return "gold";
        if (level >= 20) return "silver";
        return "bronze";
    }
    
    /**
     * 获取段位中文名称
     */
    private String rankName(String r) {
        return switch (r) { case "diamond" -> "钻石"; case "platinum" -> "铂金"; case "gold" -> "黄金"; case "silver" -> "白银"; default -> "青铜"; };
    }
    
    /**
     * 获取段位图标Emoji
     */
    private String rankIcon(String r) {
        return switch (r) { case "diamond" -> "💎"; case "platinum" -> "🏆"; case "gold" -> "🥇"; case "silver" -> "🥈"; default -> "🥉"; };
    }

    /**
     * 构建用户积分化统计Map
     */
    private Map<String, Object> buildStats(GamificationUserStats s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", s.getUserId());
        m.put("level", s.getLevel());
        m.put("exp", s.getExp());
        m.put("totalScore", s.getTotalScore());
        m.put("rank", s.getRank());
        m.put("rankName", rankName(s.getRank()));
        m.put("rankIcon", rankIcon(s.getRank()));
        m.put("streakDays", s.getStreakDays());
        m.put("lastActiveAt", s.getLastActiveAt());
        int next = expForLevel(s.getLevel() + 1);
        m.put("expToNext", next);
        m.put("expProgress", next > 0 ? Math.round((double) s.getExp() / next * 100) : 0);
        return m;
    }

    /**
     * 获取用户积分化档案
     * @param userId 用户ID
     * @return 包含等级、积分、段位、徽章等信息
     */
    @Override
    @Cacheable(value = "gamificationProfile", key = "#userId")
    public Map<String, Object> getProfile(Long userId) {
        GamificationUserStats s = getOrCreateStats(userId);
        Map<String, Object> p = buildStats(s);

        // Unlocked badges
        List<Map<String, Object>> badges = getBadges(userId);
        p.put("badges", badges);
        p.put("totalBadges", badges.size());
        p.put("unlockedBadges", badges.size());

        // Recent score history (last 5)
        List<Map<String, Object>> hist = getScoreHistory(userId, 1, 5);
        p.put("recentHistory", hist);

        return p;
    }

    /**
     * 获取用户的所有徽章（含已解锁和未解锁）
     * @param userId 用户ID
     * @return 徽章列表
     */
    @Override
    public List<Map<String, Object>> getBadges(Long userId) {
        List<GamificationBadgeDefinition> all = badgeDefMapper.selectList(null);
        // 预抓取该用户所有已解锁徽章，一次查询替代循环内查询
        List<GamificationUserBadge> unlockedBadges = userBadgeMapper.selectList(
            new LambdaQueryWrapper<GamificationUserBadge>()
                .eq(GamificationUserBadge::getUserId, userId));
        // 构建badgeId -> unlockedAt 的Map用于快速查找
        java.util.Map<Long, GamificationUserBadge> unlockedMap = unlockedBadges.stream()
            .collect(java.util.stream.Collectors.toMap(GamificationUserBadge::getBadgeId, b -> b));

        List<Map<String, Object>> result = new ArrayList<>();
        for (GamificationBadgeDefinition def : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", def.getId());
            m.put("badgeKey", def.getBadgeKey());
            m.put("name", def.getName());
            m.put("icon", def.getIcon());
            m.put("rarity", def.getRarity());
            m.put("description", def.getDescription());
            m.put("condition", def.getCondition());
            m.put("rewardExp", def.getRewardExp());
            m.put("rewardScore", def.getRewardScore());
            boolean isUnlocked = unlockedMap.containsKey(def.getId());
            m.put("unlocked", isUnlocked);
            if (isUnlocked) {
                GamificationUserBadge ub = unlockedMap.get(def.getId());
                m.put("unlockedAt", ub != null ? ub.getUnlockedAt() : null);
            }
            result.add(m);
        }
        return result;
    }

    /**
     * 获取徽章详情
     * @param userId 用户ID
     * @param badgeId 徽章ID
     * @return 徽章详情
     */
    @Override
    public Map<String, Object> getBadgeDetail(Long userId, Long badgeId) {
        GamificationBadgeDefinition def = badgeDefMapper.selectById(badgeId);
        if (def == null) return Map.of("error", "Badge not found");
        boolean unlocked = userBadgeMapper.selectCount(
            new LambdaQueryWrapper<GamificationUserBadge>()
                .eq(GamificationUserBadge::getUserId, userId)
                .eq(GamificationUserBadge::getBadgeId, badgeId)) > 0;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", def.getId()); m.put("badgeKey", def.getBadgeKey());
        m.put("name", def.getName()); m.put("icon", def.getIcon());
        m.put("rarity", def.getRarity()); m.put("description", def.getDescription());
        m.put("condition", def.getCondition());
        m.put("rewardExp", def.getRewardExp()); m.put("rewardScore", def.getRewardScore());
        m.put("unlocked", unlocked);
        return m;
    }

    /**
     * 获取排行榜
     * @param period 周期（weekly/monthly/all）
     * @return 排名前20的用户列表
     */
    @Override
    public List<Map<String, Object>> getLeaderboard(String period) {
        // Directly query gamification_user_stats, ordered by total_score desc
        List<GamificationUserStats> top = statsMapper.selectList(
            new LambdaQueryWrapper<GamificationUserStats>()
                .orderByDesc(GamificationUserStats::getTotalScore)
                .last("LIMIT 20"));
        if (top.isEmpty()) return Collections.emptyList();

        // 批量获取用户信息，一次查询替代循环内N次查询
        List<Long> userIds = top.stream().map(GamificationUserStats::getUserId).toList();
        java.util.Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> r = new ArrayList<>();
        int rank = 1;
        for (GamificationUserStats s : top) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", s.getUserId());
            // 从预加载的Map中获取用户名
            User user = userMap.get(s.getUserId());
            String username = user != null && user.getUsername() != null
                ? user.getUsername() : "用户" + s.getUserId();
            m.put("username", username);
            m.put("score", s.getTotalScore());
            m.put("rank", rank++);
            m.put("level", s.getLevel());
            m.put("rankTitle", s.getRank());
            r.add(m);
        }
        return r;
    }

    /**
     * 获取排行榜概览
     * @return 包含周榜、月榜、总榜前三名
     */
    @Override
    public Map<String, Object> getLeaderboardOverview() {
        List<Map<String, Object>> weekly = getLeaderboard("weekly");
        List<Map<String, Object>> monthly = getLeaderboard("monthly");
        List<Map<String, Object>> all = getLeaderboard("all");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("weekly", weekly.stream().limit(3).toList());
        r.put("monthly", monthly.stream().limit(3).toList());
        r.put("all", all.stream().limit(3).toList());
        return r;
    }

    /**
     * 获取用户积分余额信息
     * @param userId 用户ID
     * @return 积分余额、累计获得、累计消费
     */
    @Override
    public Map<String, Object> getScore(Long userId) {
        GamificationUserStats s = getOrCreateStats(userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("balance", s.getAvailableScore());
        m.put("totalEarned", s.getTotalScore());
        m.put("totalSpent", s.getTotalScore() - s.getAvailableScore());
        m.put("thisMonth", s.getAvailableScore());
        return m;
    }

    /**
     * 获取用户积分历史记录
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 积分变动记录列表
     */
    @Override
    public List<Map<String, Object>> getScoreHistory(Long userId, int page, int size) {
        Page<GamificationScoreHistory> p = new Page<>(page, size);
        List<GamificationScoreHistory> records = scoreHistoryMapper.selectPage(p,
            new LambdaQueryWrapper<GamificationScoreHistory>()
                .eq(GamificationScoreHistory::getUserId, userId)
                .orderByDesc(GamificationScoreHistory::getCreatedAt)
        ).getRecords();
        List<Map<String, Object>> r = new ArrayList<>();
        for (GamificationScoreHistory h : records) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId()); m.put("type", h.getType());
            m.put("amount", h.getAmount()); m.put("reason", h.getReason());
            m.put("createdAt", h.getCreatedAt());
            r.add(m);
        }
        return r;
    }

    /**
     * 兑换商城商品
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 兑换结果信息
     */
    @Override
    @Transactional
    public Map<String, Object> redeemProduct(Long userId, Long productId) {
        GamificationStoreProduct product = storeProductMapper.selectById(productId);
        if (product == null || !product.getActive()) throw new RuntimeException("商品不存在或已下架");

        boolean already = userProductMapper.selectCount(
            new LambdaQueryWrapper<GamificationUserProduct>()
                .eq(GamificationUserProduct::getUserId, userId)
                .eq(GamificationUserProduct::getProductId, productId)) > 0;
        if (already) throw new RuntimeException("您已拥有此商品");

        // H-10: 原子扣减积分，防止并发超额使用
        int deducted = statsMapper.deductScore(userId, product.getPrice());
        if (deducted == 0) {
            throw new RuntimeException("积分不足");
        }

        // Record history
        GamificationScoreHistory h = new GamificationScoreHistory();
        h.setUserId(userId); h.setType("redeem"); h.setAmount(-product.getPrice());
        h.setReason("兑换商品: " + product.getName());
        h.setCreatedAt(LocalDateTime.now());
        scoreHistoryMapper.insert(h);

        // Grant product
        GamificationUserProduct up = new GamificationUserProduct();
        up.setUserId(userId); up.setProductId(productId); up.setEquipped(false);
        up.setRedeemedAt(LocalDateTime.now());
        userProductMapper.insert(up);

        // 重新获取最新积分
        GamificationUserStats updatedStats = statsMapper.selectOne(
            new LambdaQueryWrapper<GamificationUserStats>().eq(GamificationUserStats::getUserId, userId));

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("productId", productId); r.put("productName", product.getName());
        r.put("spent", product.getPrice()); r.put("remainingScore", updatedStats != null ? updatedStats.getAvailableScore() : 0);
        r.put("equipped", false);
        return r;
    }

    /**
     * 获取积分商城商品列表
     * @param page 页码
     * @param size 每页数量
     * @return 上架商品分页列表
     */
    @Override
    public Map<String, Object> getStore(int page, int size) {
        Page<GamificationStoreProduct> p = new Page<>(page, size);
        Page<GamificationStoreProduct> resultPage = storeProductMapper.selectPage(p,
            new LambdaQueryWrapper<GamificationStoreProduct>()
                .eq(GamificationStoreProduct::getActive, true)
                .orderByAsc(GamificationStoreProduct::getPrice));
        List<Map<String, Object>> r = new ArrayList<>();
        for (GamificationStoreProduct prod : resultPage.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", prod.getId()); m.put("name", prod.getName()); m.put("icon", prod.getIcon());
            m.put("description", prod.getDescription()); m.put("price", prod.getPrice());
            m.put("stock", prod.getStock()); m.put("active", prod.getActive());
            r.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", r);
        result.put("total", resultPage.getTotal());
        result.put("pages", resultPage.getPages());
        result.put("current", resultPage.getCurrent());
        result.put("size", resultPage.getSize());
        return result;
    }

    /**
     * 获取用户成长进度
     * @param userId 用户ID
     * @return 等级、段位、经验进度
     */
    @Override
    public Map<String, Object> getProgress(Long userId) {
        GamificationUserStats s = getOrCreateStats(userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level", s.getLevel());
        m.put("levelName", levelName(s.getLevel()));
        m.put("rank", s.getRank());
        m.put("rankName", rankName(s.getRank()));
        m.put("rankIcon", rankIcon(s.getRank()));
        m.put("currentExp", s.getExp());
        int next = expForLevel(s.getLevel() + 1);
        m.put("nextExp", next);
        m.put("expProgress", next > 0 ? Math.round((double) s.getExp() / next * 100) : 0);
        m.put("totalExp", s.getExp());
        return m;
    }

    /**
     * 获取等级排行榜
     * @return 前10名用户
     */
    @Override
    public List<Map<String, Object>> getLevelRanking() {
        List<GamificationUserStats> top = statsMapper.selectList(
            new LambdaQueryWrapper<GamificationUserStats>()
                .orderByDesc(GamificationUserStats::getLevel)
                .orderByDesc(GamificationUserStats::getExp)
                .last("LIMIT 10"));
        List<Map<String, Object>> r = new ArrayList<>();
        int rank = 1;
        for (GamificationUserStats s : top) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", s.getUserId()); m.put("username", "用户" + s.getUserId());
            m.put("level", s.getLevel()); m.put("exp", s.getExp());
            m.put("rank", rank++);
            r.add(m);
        }
        return r;
    }

    /**
     * SOP执行完成回调
     * 增加经验值和积分，检查并授予徽章
     * @param userId 用户ID
     * @param sopId SOP ID
     */
    @Override
    @Transactional
    public void onExecutionCompleted(Long userId, Long sopId) {
        if (userId == null) return;
        GamificationUserStats s = getOrCreateStats(userId);

        // Award: +10 exp, +5 score
        s.setExp(s.getExp() + 10);
        s.setTotalScore(s.getTotalScore() + 5);
        s.setAvailableScore(s.getAvailableScore() + 5);

        // === FIX: Update streak days ===
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate lastActive = s.getLastActiveAt() != null ? s.getLastActiveAt().toLocalDate() : null;

        if (lastActive == null) {
            // First activity ever
            s.setStreakDays(1);
        } else if (lastActive.equals(today.minusDays(1))) {
            // Consecutive day — increment streak
            s.setStreakDays(s.getStreakDays() + 1);
        } else if (!lastActive.equals(today)) {
            // Missed a day or same day — reset streak to 1 (same day doesn't increment)
            s.setStreakDays(1);
        }
        // If lastActive equals today, don't change streak (already counted today)

        s.setLastActiveAt(now);
        // ================================

        // Level up check (capped at MAX_LEVEL)
        if (s.getLevel() < MAX_LEVEL) {
            int needed = expForLevel(s.getLevel() + 1);
            while (s.getExp() >= needed && s.getLevel() < MAX_LEVEL) {
                s.setExp(s.getExp() - needed);
                s.setLevel(s.getLevel() + 1);
                s.setRank(rankForLevel(s.getLevel()));
                needed = expForLevel(s.getLevel() + 1);
            }
        }
        s.setUpdatedAt(LocalDateTime.now());
        statsMapper.updateById(s);

        // Score history
        GamificationScoreHistory h = new GamificationScoreHistory();
        h.setUserId(userId); h.setType("earn"); h.setAmount(5);
        h.setReason("完成 SOP 执行"); h.setCreatedAt(LocalDateTime.now());
        scoreHistoryMapper.insert(h);

        // Check badges
        checkAndGrantBadge(userId, "first_execution", s);
        checkAndGrantStreakBadge(userId, s);
    }

    /**
     * 检查并授予指定徽章
     */
    private void checkAndGrantBadge(Long userId, String badgeKey, GamificationUserStats s) {
        GamificationBadgeDefinition def = badgeDefMapper.selectList(
            new LambdaQueryWrapper<GamificationBadgeDefinition>()
                .eq(GamificationBadgeDefinition::getBadgeKey, badgeKey)
        ).stream().findFirst().orElse(null);
        if (def == null) return;
        
        // Use INSERT IGNORE to prevent race condition duplicates
        GamificationUserBadge ub = new GamificationUserBadge();
        ub.setUserId(userId); ub.setBadgeId(def.getId());
        ub.setUnlockedAt(LocalDateTime.now());
        int inserted = userBadgeMapper.insertIgnore(ub);
        if (inserted > 0) {
            log.info("解锁徽章: userId={}, badgeId={}", userId, def.getId());
        }
    }

    /**
     * 检查并授予连续签到徽章
     */
    private void checkAndGrantStreakBadge(Long userId, GamificationUserStats s) {
        // Streak logic: if last active was yesterday
        GamificationBadgeDefinition def = badgeDefMapper.selectList(
            new LambdaQueryWrapper<GamificationBadgeDefinition>()
                .eq(GamificationBadgeDefinition::getBadgeKey, "streak_7")
        ).stream().findFirst().orElse(null);
        if (def == null) return;
        if (s.getStreakDays() >= 7) {
            // Use INSERT IGNORE to prevent race condition duplicates
            GamificationUserBadge ub = new GamificationUserBadge();
            ub.setUserId(userId); ub.setBadgeId(def.getId());
            ub.setUnlockedAt(LocalDateTime.now());
            int inserted = userBadgeMapper.insertIgnore(ub);
            if (inserted > 0) {
                log.info("解锁连续签到徽章: userId={}, badgeId={}", userId, def.getId());
            }
        }
    }

    /**
     * 获取或创建用户统计记录
     */
    private GamificationUserStats getOrCreateStats(Long userId) {
        GamificationUserStats s = statsMapper.selectOne(
            new LambdaQueryWrapper<GamificationUserStats>()
                .eq(GamificationUserStats::getUserId, userId));
        if (s == null) {
            s = new GamificationUserStats();
            s.setUserId(userId); s.setLevel(1); s.setExp(0);
            s.setTotalScore(0); s.setAvailableScore(0);
            s.setRank("bronze"); s.setStreakDays(0);
            s.setCreatedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            statsMapper.insert(s);
        }
        return s;
    }

    /**
     * 获取等级对应的中文名称
     */
    private String levelName(int level) {
        if (level >= 80) return "王者";
        if (level >= 60) return "钻石";
        if (level >= 40) return "铂金";
        if (level >= 20) return "黄金";
        if (level >= 10) return "白银";
        return "入门";
    }
}
