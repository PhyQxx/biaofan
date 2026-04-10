package com.biaofan.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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

    // === EXP / Level helpers ===
    private int expForLevel(int level) { return level * 100; }
    private String rankForLevel(int level) {
        if (level >= 80) return "diamond";
        if (level >= 60) return "platinum";
        if (level >= 40) return "gold";
        if (level >= 20) return "silver";
        return "bronze";
    }
    private String rankName(String r) {
        return switch (r) { case "diamond" -> "钻石"; case "platinum" -> "铂金"; case "gold" -> "黄金"; case "silver" -> "白银"; default -> "青铜"; };
    }
    private String rankIcon(String r) {
        return switch (r) { case "diamond" -> "💎"; case "platinum" -> "🏆"; case "gold" -> "🥇"; case "silver" -> "🥈"; default -> "🥉"; };
    }

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

    @Override
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

    @Override
    public List<Map<String, Object>> getBadges(Long userId) {
        List<GamificationBadgeDefinition> all = badgeDefMapper.selectList(null);
        Set<Long> unlocked = new HashSet<>();
        userBadgeMapper.selectList(new LambdaQueryWrapper<GamificationUserBadge>()
            .eq(GamificationUserBadge::getUserId, userId))
            .forEach(b -> unlocked.add(b.getBadgeId()));

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
            m.put("unlocked", unlocked.contains(def.getId()));
            if (unlocked.contains(def.getId())) {
                GamificationUserBadge ub = userBadgeMapper.selectList(
                    new LambdaQueryWrapper<GamificationUserBadge>()
                        .eq(GamificationUserBadge::getUserId, userId)
                        .eq(GamificationUserBadge::getBadgeId, def.getId())
                        .last("LIMIT 1")
                ).stream().findFirst().orElse(null);
                m.put("unlockedAt", ub != null ? ub.getUnlockedAt() : null);
            }
            result.add(m);
        }
        return result;
    }

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

    @Override
    public List<Map<String, Object>> getLeaderboard(String period) {
        // Directly query gamification_user_stats, ordered by total_score desc
        List<GamificationUserStats> top = statsMapper.selectList(
            new LambdaQueryWrapper<GamificationUserStats>()
                .orderByDesc(GamificationUserStats::getTotalScore)
                .last("LIMIT 20"));
        List<Map<String, Object>> r = new ArrayList<>();
        int rank = 1;
        for (GamificationUserStats s : top) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", s.getUserId());
            // Try to get real username from user table
            String username = "用户" + s.getUserId();
            try {
                var user = userMapper.selectById(s.getUserId());
                if (user != null && user.getUsername() != null) {
                    username = user.getUsername();
                }
            } catch (Exception ignored) {}
            m.put("username", username);
            m.put("score", s.getTotalScore());
            m.put("rank", rank++);
            m.put("level", s.getLevel());
            m.put("rankTitle", s.getRank());
            r.add(m);
        }
        return r;
    }

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

    @Override
    public List<Map<String, Object>> getScoreHistory(Long userId, int page, int size) {
        Page<GamificationScoreHistory> p = new Page<>(page, size);
        userBadgeMapper.selectList(new LambdaQueryWrapper<GamificationUserBadge>()); // init
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

    @Override
    public List<Map<String, Object>> getStore() {
        List<GamificationStoreProduct> products = storeProductMapper.selectList(
            new LambdaQueryWrapper<GamificationStoreProduct>()
                .eq(GamificationStoreProduct::getActive, true)
                .orderByAsc(GamificationStoreProduct::getPrice));
        List<Map<String, Object>> r = new ArrayList<>();
        for (GamificationStoreProduct p : products) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId()); m.put("name", p.getName()); m.put("icon", p.getIcon());
            m.put("description", p.getDescription()); m.put("price", p.getPrice());
            m.put("stock", p.getStock()); m.put("active", p.getActive());
            r.add(m);
        }
        return r;
    }

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

    @Override
    @Transactional
    public void onExecutionCompleted(Long userId, Long sopId) {
        if (userId == null) return;
        GamificationUserStats s = getOrCreateStats(userId);

        // Award: +10 exp, +5 score
        s.setExp(s.getExp() + 10);
        s.setTotalScore(s.getTotalScore() + 5);
        s.setAvailableScore(s.getAvailableScore() + 5);
        s.setLastActiveAt(LocalDateTime.now());

        // Level up check
        int needed = expForLevel(s.getLevel() + 1);
        while (s.getExp() >= needed) {
            s.setExp(s.getExp() - needed);
            s.setLevel(s.getLevel() + 1);
            s.setRank(rankForLevel(s.getLevel()));
            needed = expForLevel(s.getLevel() + 1);
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

    private void checkAndGrantBadge(Long userId, String badgeKey, GamificationUserStats s) {
        GamificationBadgeDefinition def = badgeDefMapper.selectList(
            new LambdaQueryWrapper<GamificationBadgeDefinition>()
                .eq(GamificationBadgeDefinition::getBadgeKey, badgeKey)
        ).stream().findFirst().orElse(null);
        if (def == null) return;
        boolean has = userBadgeMapper.selectCount(
            new LambdaQueryWrapper<GamificationUserBadge>()
                .eq(GamificationUserBadge::getUserId, userId)
                .eq(GamificationUserBadge::getBadgeId, def.getId())) > 0;
        if (!has) {
            GamificationUserBadge ub = new GamificationUserBadge();
            ub.setUserId(userId); ub.setBadgeId(def.getId());
            ub.setUnlockedAt(LocalDateTime.now());
            userBadgeMapper.insert(ub);
        }
    }

    private void checkAndGrantStreakBadge(Long userId, GamificationUserStats s) {
        // Streak logic: if last active was yesterday
        GamificationBadgeDefinition def = badgeDefMapper.selectList(
            new LambdaQueryWrapper<GamificationBadgeDefinition>()
                .eq(GamificationBadgeDefinition::getBadgeKey, "streak_7")
        ).stream().findFirst().orElse(null);
        if (def == null) return;
        boolean has = userBadgeMapper.selectCount(
            new LambdaQueryWrapper<GamificationUserBadge>()
                .eq(GamificationUserBadge::getUserId, userId)
                .eq(GamificationUserBadge::getBadgeId, def.getId())) > 0;
        if (!has && s.getStreakDays() >= 7) {
            GamificationUserBadge ub = new GamificationUserBadge();
            ub.setUserId(userId); ub.setBadgeId(def.getId());
            ub.setUnlockedAt(LocalDateTime.now());
            userBadgeMapper.insert(ub);
        }
    }

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

    private String levelName(int level) {
        if (level >= 80) return "王者";
        if (level >= 60) return "钻石";
        if (level >= 40) return "铂金";
        if (level >= 20) return "黄金";
        if (level >= 10) return "白银";
        return "入门";
    }
}
