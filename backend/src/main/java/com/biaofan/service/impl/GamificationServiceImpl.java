package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.*;
import com.biaofan.mapper.*;
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
    private final UserMapper userMapper;

    // === EXP / Level helpers ===
    private static final int MAX_LEVEL = 999;
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
    @Cacheable(value = "gamificationProfile", key = "#userId + '_' + (#orgId != null ? #orgId : '0')")
    public Map<String, Object> getProfile(Long userId, Long orgId) {
        GamificationUserStats s = getOrCreateStats(userId, orgId);
        Map<String, Object> p = buildStats(s);

        List<Map<String, Object>> badges = getBadges(userId);
        p.put("badges", badges);
        p.put("totalBadges", badges.size());
        p.put("unlockedBadges", badges.stream().filter(b -> (boolean)b.get("unlocked")).count());

        List<Map<String, Object>> hist = getScoreHistory(userId, orgId, 1, 5);
        p.put("recentHistory", hist);

        return p;
    }

    @Override
    public List<Map<String, Object>> getBadges(Long userId) {
        List<GamificationBadgeDefinition> all = badgeDefMapper.selectList(null);
        List<GamificationUserBadge> unlockedBadges = userBadgeMapper.selectList(
            new LambdaQueryWrapper<GamificationUserBadge>().eq(GamificationUserBadge::getUserId, userId));
        Set<Long> unlockedIds = new HashSet<>();
        unlockedBadges.forEach(b -> unlockedIds.add(b.getBadgeId()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (GamificationBadgeDefinition def : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", def.getId());
            m.put("badgeKey", def.getBadgeKey());
            m.put("name", def.getName());
            m.put("icon", def.getIcon());
            m.put("rarity", def.getRarity());
            m.put("description", def.getDescription());
            m.put("unlocked", unlockedIds.contains(def.getId()));
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
        m.put("unlocked", unlocked);
        return m;
    }

    @Override
    public List<Map<String, Object>> getLeaderboard(String period, Long orgId) {
        LambdaQueryWrapper<GamificationUserStats> q = new LambdaQueryWrapper<GamificationUserStats>()
                .orderByDesc(GamificationUserStats::getTotalScore)
                .last("LIMIT 20");
        if (orgId == null) {
            q.isNull(GamificationUserStats::getOrgId);
        } else {
            q.eq(GamificationUserStats::getOrgId, orgId);
        }
        List<GamificationUserStats> top = statsMapper.selectList(q);
        if (top.isEmpty()) return Collections.emptyList();

        List<Long> userIds = top.stream().map(GamificationUserStats::getUserId).toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> r = new ArrayList<>();
        int rank = 1;
        for (GamificationUserStats s : top) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", s.getUserId());
            User user = userMap.get(s.getUserId());
            m.put("username", user != null ? user.getUsername() : "用户" + s.getUserId());
            m.put("score", s.getTotalScore());
            m.put("rank", rank++);
            m.put("level", s.getLevel());
            m.put("rankTitle", s.getRank());
            r.add(m);
        }
        return r;
    }

    @Override
    public Map<String, Object> getLeaderboardOverview(Long orgId) {
        List<Map<String, Object>> all = getLeaderboard("all", orgId);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("weekly", all.stream().limit(3).toList());
        r.put("monthly", all.stream().limit(3).toList());
        r.put("all", all.stream().limit(3).toList());
        return r;
    }

    @Override
    public Map<String, Object> getScore(Long userId, Long orgId) {
        GamificationUserStats s = getOrCreateStats(userId, orgId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("balance", s.getAvailableScore());
        m.put("totalEarned", s.getTotalScore());
        m.put("totalSpent", s.getTotalScore() - s.getAvailableScore());
        return m;
    }

    @Override
    public List<Map<String, Object>> getScoreHistory(Long userId, Long orgId, int page, int size) {
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

    @Override
    @Transactional
    public Map<String, Object> redeemProduct(Long userId, Long orgId, Long productId) {
        GamificationStoreProduct product = storeProductMapper.selectById(productId);
        if (product == null || !product.getActive()) throw new RuntimeException("商品不存在或已下架");

        GamificationUserStats s = getOrCreateStats(userId, orgId);
        if (s.getAvailableScore() < product.getPrice()) {
            throw new RuntimeException("积分不足");
        }
        
        s.setAvailableScore(s.getAvailableScore() - product.getPrice());
        statsMapper.updateById(s);

        GamificationScoreHistory h = new GamificationScoreHistory();
        h.setUserId(userId); h.setType("redeem"); h.setAmount(-product.getPrice());
        h.setReason("兑换商品: " + product.getName());
        h.setCreatedAt(LocalDateTime.now());
        scoreHistoryMapper.insert(h);

        GamificationUserProduct up = new GamificationUserProduct();
        up.setUserId(userId); up.setProductId(productId); up.setEquipped(false);
        up.setRedeemedAt(LocalDateTime.now());
        userProductMapper.insert(up);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("productId", productId); r.put("productName", product.getName());
        r.put("remainingScore", s.getAvailableScore());
        return r;
    }

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
        return result;
    }

    @Override
    public Map<String, Object> getProgress(Long userId, Long orgId) {
        GamificationUserStats s = getOrCreateStats(userId, orgId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level", s.getLevel());
        m.put("rank", s.getRank());
        m.put("currentExp", s.getExp());
        int next = expForLevel(s.getLevel() + 1);
        m.put("nextExp", next);
        return m;
    }

    @Override
    public List<Map<String, Object>> getLevelRanking(Long orgId) {
        LambdaQueryWrapper<GamificationUserStats> q = new LambdaQueryWrapper<GamificationUserStats>()
                .orderByDesc(GamificationUserStats::getLevel)
                .orderByDesc(GamificationUserStats::getExp)
                .last("LIMIT 10");
        if (orgId != null) q.eq(GamificationUserStats::getOrgId, orgId);
        else q.isNull(GamificationUserStats::getOrgId);
        
        List<GamificationUserStats> top = statsMapper.selectList(q);
        List<Map<String, Object>> r = new ArrayList<>();
        for (GamificationUserStats s : top) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", s.getUserId()); m.put("level", s.getLevel());
            r.add(m);
        }
        return r;
    }

    @Override
    @Transactional
    public void onExecutionCompleted(Long userId, Long orgId, Long sopId) {
        if (userId == null) return;
        GamificationUserStats s = getOrCreateStats(userId, orgId);

        s.setExp(s.getExp() + 10);
        s.setTotalScore(s.getTotalScore() + 5);
        s.setAvailableScore(s.getAvailableScore() + 5);

        LocalDateTime now = LocalDateTime.now();
        s.setLastActiveAt(now);

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

        GamificationScoreHistory h = new GamificationScoreHistory();
        h.setUserId(userId); h.setType("earn"); h.setAmount(5);
        h.setReason("完成 SOP 执行"); h.setCreatedAt(LocalDateTime.now());
        scoreHistoryMapper.insert(h);

        checkAndGrantBadge(userId, "first_execution", s);
        checkAndGrantStreakBadge(userId, s);
    }

    private void checkAndGrantBadge(Long userId, String badgeKey, GamificationUserStats s) {
        GamificationBadgeDefinition def = badgeDefMapper.selectList(
            new LambdaQueryWrapper<GamificationBadgeDefinition>().eq(GamificationBadgeDefinition::getBadgeKey, badgeKey)
        ).stream().findFirst().orElse(null);
        if (def == null) return;
        
        GamificationUserBadge ub = new GamificationUserBadge();
        ub.setUserId(userId); ub.setBadgeId(def.getId());
        ub.setUnlockedAt(LocalDateTime.now());
        try { userBadgeMapper.insert(ub); } catch (Exception ignored) {}
    }

    private void checkAndGrantStreakBadge(Long userId, GamificationUserStats s) {
        if (s.getStreakDays() >= 7) {
            checkAndGrantBadge(userId, "streak_7", s);
        }
    }

    private GamificationUserStats getOrCreateStats(Long userId, Long orgId) {
        LambdaQueryWrapper<GamificationUserStats> q = new LambdaQueryWrapper<GamificationUserStats>().eq(GamificationUserStats::getUserId, userId);
        if (orgId == null) q.isNull(GamificationUserStats::getOrgId);
        else q.eq(GamificationUserStats::getOrgId, orgId);
        
        GamificationUserStats s = statsMapper.selectOne(q);
        if (s == null) {
            s = new GamificationUserStats();
            s.setUserId(userId); s.setOrgId(orgId);
            s.setLevel(1); s.setExp(0); s.setTotalScore(0); s.setAvailableScore(0);
            s.setRank("bronze"); s.setStreakDays(0);
            s.setCreatedAt(LocalDateTime.now()); s.setUpdatedAt(LocalDateTime.now());
            statsMapper.insert(s);
        }
        return s;
    }
}
