package com.biaofan.constant;

/**
 * Status string constants to replace hardcoded magic strings throughout the codebase.
 * Using constants reduces typos and makes refactoring easier.
 * 
 * Usage: import static com.biaofan.constant.StatusConstants.*;
 */
public final class StatusConstants {

    private StatusConstants() {} // Prevent instantiation

    // SOP Status
    public static final String DRAFT = "draft";
    public static final String PUBLISHED = "published";
    public static final String ARCHIVED = "archived";

    // Execution Status
    public static final String PENDING = "pending";
    public static final String IN_PROGRESS = "in_progress";
    public static final String COMPLETED = "completed";
    public static final String OVERDUE = "overdue";
    public static final String CANCELLED = "cancelled";

    // Marketplace Template Status
    public static final String TEMPLATE_PENDING = "pending";
    public static final String TEMPLATE_APPROVED = "approved";
    public static final String TEMPLATE_REJECTED = "rejected";
    public static final String TEMPLATE_OFFLINE = "offline";

    // User Status
    public static final String USER_ACTIVE = "active";
    public static final String USER_INACTIVE = "inactive";
    public static final String USER_BANNED = "banned";

    // Gamification
    public static final String RANK_BRONZE = "bronze";
    public static final String RANK_SILVER = "silver";
    public static final String RANK_GOLD = "gold";
    public static final String RANK_PLATINUM = "platinum";
    public static final String RANK_DIAMOND = "diamond";

    // Notification Types
    public static final String NOTIF_SYSTEM = "system";
    public static final String NOTIF_SOP_ASSIGNED = "sop_assigned";
    public static final String NOTIF_SOP_COMPLETED = "sop_completed";
    public static final String NOTIF_SOP_OVERDUE = "sop_overdue";
    public static final String NOTIF_SCHEDULE_TRIGGERED = "schedule_triggered";

    // Score History Types
    public static final String SCORE_EARN = "earn";
    public static final String SCORE_SPEND = "spend";
    public static final String SCORE_REDEEM = "redeem";
    public static final String SCORE_BONUS = "bonus";
    public static final String SCORE_PENALTY = "penalty";
}
