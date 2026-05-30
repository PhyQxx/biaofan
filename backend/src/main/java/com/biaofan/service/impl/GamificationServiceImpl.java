package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.*;
import com.biaofan.mapper.*;
import com.biaofan.service.GamificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 积分化服务实现类
 * 提供用户等级、经验值、积分、排行榜、徽章、商城兑换等功能
 * 动态加载 gamification_growth_rules 表中的规则
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
    private final GamificationGrowthRuleMapper ruleMapper;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final SopExecutionMapper executionMapper;
    private final ObjectMapper objectMapper;

    // === 动态规则获取 (带缓存) ===

    private JsonNode getRuleConfig(String type, String key) {
        GamificationGrowthRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<GamificationGrowthRule>()
                .eq(GamificationGrowthRule::getRuleType, type)
                .eq(GamificationGrowthRule::getRuleKey, key)
                .eq(GamificationGrowthRule::getIsActive, true));
        if (rule == null || rule.getRuleValue() == null) return null;
        try {
            return objectMapper.readTree(rule.getRuleValue());
        } catch (Exception e) {
            log.error("[Gamification] 解析规则失败 type={}, key={}", type, key, e);
            return null;
        }
    }

    private int getExpNeededForNextLevel(int currentLevel) {
        // 从 L1, L2... 规则中获取
        JsonNode config = getRuleConfig("level_exp", "L" + currentLevel);
        if (config != null && config.has("exp_to_next")) {
            return config.get("exp_to_next").asInt();
        }
        return currentLevel * 100; // 兜底
    }

    private String calculateRank(int level, int currentExp) {
        // 从 segment_threshold 中获取
        List<GamificationGrowthRule> segments = ruleMapper.selectList(new LambdaQueryWrapper<GamificationGrowthRule>()
                .eq(GamificationGrowthRule::getRuleType, "segment_threshold")
                .eq(GamificationGrowthRule::getIsActive, true));
        
        String bestRank = "bronze";
        int maxMinExp = -1;

        for (GamificationGrowthRule rule : segments) {
            try {
                JsonNode node = objectMapper.readTree(rule.getRuleValue());
                int minExp = node.get("min_exp").asInt();
                // 注意：这里由于等级系统可能重置经验，计算段位时通常用总经验或当前等级综合判断
                // 这里暂时简化为：如果当前等级规则的 total_exp 加上当前 exp 超过阈值
                if (currentExp >= minExp && minExp > maxMinExp) {
                    maxMinExp = minExp;
                    bestRank = rule.getRuleKey();
                }
            } catch (Exception ignored) {}
        }
        return bestRank;
    }

    // === Rank Display ===
    
    private String rankName(String r) {
        return switch (r) { 
            case "king" -> "王者"; 
            case "diamond" -> "钻石"; 
            case "gold" -> "黄金"; 
            case "silver" -> "白银"; 
            default -> "青铜"; 
        };
    }
    
    private String rankIcon(String r) {
        return switch (r) { 
            case "king" -> "👑"; 
            case "diamond" -> "💎"; 
            case "gold" -> "🥇"; 
            case "silver" -> "🥈"; 
            default -> "🥉"; 
        };
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
        
        int next = getExpNeededForNextLevel(s.getLevel());
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
        int rankIdx = 1;
        for (GamificationUserStats s : top) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", s.getUserId());
            User user = userMap.get(s.getUserId());
            m.put("username", user != null ? user.getUsername() : "用户" + s.getUserId());
            m.put("score", s.getTotalScore());
            m.put("rank", rankIdx++);
            m.put("level", s.getLevel());
            m.put("rankTitle", rankName(s.getRank()));
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
        return buildStats(s);
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
    public List<Map<String, Object>> getTeamLeaderboard(Long rootOrgId) {
        Organization root = organizationMapper.selectById(rootOrgId);
        if (root == null) return Collections.emptyList();

        // 1. 获取所有子组织
        List<Organization> orgs = organizationMapper.selectList(new LambdaQueryWrapper<Organization>()
                .likeRight(Organization::getPath, root.getPath()));
        
        if (orgs.isEmpty()) return Collections.emptyList();

        // 2. 聚合每个组织的积分
        List<Map<String, Object>> results = new ArrayList<>();
        for (Organization org : orgs) {
            // 查询该组织下所有成员的积分总和
            List<GamificationUserStats> statsList = statsMapper.selectList(
                new LambdaQueryWrapper<GamificationUserStats>().eq(GamificationUserStats::getOrgId, org.getId())
            );
            int teamScore = statsList.stream().mapToInt(GamificationUserStats::getTotalScore).sum();
            
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orgId", org.getId());
            m.put("orgName", org.getName());
            m.put("score", teamScore);
            results.add(m);
        }

        // 3. 排序
        results.sort((a, b) -> ((Integer) b.get("score")).compareTo((Integer) a.get("score")));
        
        return results;
    }

    @Override
    @Transactional
    public void onExecutionCompleted(Long userId, Long orgId, Long sopId) {
        if (userId == null) return;
        log.info("[Gamification] 触发执行奖励 userId={}, orgId={}", userId, orgId);
        GamificationUserStats s = getOrCreateStats(userId, orgId);

        // 获取奖励规则
        int addExp = 10;
        int addScore = 5;
        JsonNode rewardRule = getRuleConfig("score_rule", "basic_execution");
        if (rewardRule != null) {
            addExp = rewardRule.has("reward_exp") ? rewardRule.get("reward_exp").asInt() : addExp;
            addScore = rewardRule.has("reward_score") ? rewardRule.get("reward_score").asInt() : addScore;
        }

        s.setExp(s.getExp() + addExp);
        s.setTotalScore(s.getTotalScore() + addScore);
        s.setAvailableScore(s.getAvailableScore() + addScore);

        s.setLastActiveAt(LocalDateTime.now());

        // 升级逻辑
        int needed = getExpNeededForNextLevel(s.getLevel());
        while (s.getExp() >= needed && s.getLevel() < 100) {
            s.setExp(s.getExp() - needed);
            s.setLevel(s.getLevel() + 1);
            needed = getExpNeededForNextLevel(s.getLevel());
        }
        
        // 更新段位 (根据当前等级主键 L1, L2... 对应的累计经验或直接等级划分)
        s.setRank(calculateRank(s.getLevel(), s.getExp()));
        
        s.setUpdatedAt(LocalDateTime.now());
        statsMapper.updateById(s);

        // 记录历史
        GamificationScoreHistory h = new GamificationScoreHistory();
        h.setUserId(userId); h.setType("earn"); h.setAmount(addScore);
        h.setReason("完成 SOP 执行奖励"); h.setCreatedAt(LocalDateTime.now());
        scoreHistoryMapper.insert(h);

        // 勋章检查
        checkAndGrantBadge(userId, "beginner", s);
        
        // 查询累计执行次数
        Long execCount = executionMapper.selectCount(new LambdaQueryWrapper<SopExecution>()
                .eq(SopExecution::getExecutorId, userId)
                .eq(SopExecution::getStatus, "completed"));
        if (execCount >= 50) checkAndGrantBadge(userId, "process_expert", s);
        
        if (s.getStreakDays() >= 7) checkAndGrantBadge(userId, "streak_lover", s);
    }

    private void checkAndGrantBadge(Long userId, String badgeKey, GamificationUserStats s) {
        GamificationBadgeDefinition def = badgeDefMapper.selectOne(
            new LambdaQueryWrapper<GamificationBadgeDefinition>().eq(GamificationBadgeDefinition::getBadgeKey, badgeKey)
        );
        if (def == null) return;
        
        // 检查是否已拥有
        Long count = userBadgeMapper.selectCount(new LambdaQueryWrapper<GamificationUserBadge>()
                .eq(GamificationUserBadge::getUserId, userId)
                .eq(GamificationUserBadge::getBadgeId, def.getId()));
        if (count > 0) return;

        GamificationUserBadge ub = new GamificationUserBadge();
        ub.setUserId(userId); ub.setBadgeId(def.getId());
        ub.setUnlockedAt(LocalDateTime.now());
        userBadgeMapper.insert(ub);
        
        log.info("[Gamification] 授予勋章: userId={}, badge={}", userId, def.getName());
        
        // 勋章奖励 (尝试从 rules 表获取奖励数值)
        JsonNode badgeReward = getRuleConfig("score_rule", badgeKey);
        if (badgeReward != null) {
            int rx = badgeReward.has("reward_exp") ? badgeReward.get("reward_exp").asInt() : 0;
            int rs = badgeReward.has("reward_score") ? badgeReward.get("reward_score").asInt() : 0;
            if (rx > 0 || rs > 0) {
                s.setExp(s.getExp() + rx);
                s.setAvailableScore(s.getAvailableScore() + rs);
                s.setTotalScore(s.getTotalScore() + rs);
                statsMapper.updateById(s);
            }
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
