package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.SopExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * SOP执行记录Mapper接口
 * 
 * 提供SOP执行记录的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * 记录SOP任务的执行情况，包括待执行、执行中、已完成等状态。
 */
@Mapper
public interface SopExecutionMapper extends BaseMapper<SopExecution> {

    /**
     * 统计已逾期任务数量
     * 
     * @param now 当前时间
     * @return 已逾期且状态为待执行的任务数量
     */
    @Select("SELECT COUNT(*) FROM sop_execution WHERE status = 'pending' AND deadline < NOW()")
    int countOverdue(@Param("now") LocalDateTime now);

    /**
     * 统计今日创建的任务总数
     * 
     * @return 今日创建的任务数量
     */
    @Select("SELECT COUNT(*) FROM sop_execution WHERE DATE(created_at) = CURDATE()")
    int countTodayTotal();

    /**
     * 统计今日已完成的任务数量
     * 
     * @return 今日已完成的任务数量
     */
    @Select("SELECT COUNT(*) FROM sop_execution WHERE DATE(completed_at) = CURDATE() AND status = 'completed'")
    int countTodayCompleted();

    /**
     * 统计今日逾期完成的任务数量
     * 
     * @return 今日逾期完成的任务数量（完成时间晚于截止时间）
     */
    @Select("SELECT COUNT(*) FROM sop_execution WHERE DATE(completed_at) = CURDATE() AND status = 'completed' AND deadline < completed_at")
    int countTodayOverdue();

    /**
     * 统计活跃成员数量
     * 
     * @return 当前有待执行或已完成任务的执行人数量
     */
    @Select("SELECT COUNT(DISTINCT executor_id) FROM sop_execution WHERE status IN ('pending', 'completed')")
    int countActiveMembers();

    /**
     * 获取完成趋势数据
     * 
     * @param days 统计天数
     * @return 包含日期、总任务数、已完成数的列表
     */
    @Select("SELECT DATE(completed_at) as date, COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) as completed " +
            "FROM sop_execution " +
            "WHERE completed_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(completed_at) ORDER BY date ASC")
    List<Map<String, Object>> getCompletionTrend(@Param("days") int days);

    /**
     * 获取成员排行榜
     * 
     * @return 执行人排行榜，包含完成数量和逾期数量，按完成数量降序
     */
    @Select("SELECT executor_id, COUNT(*) as completed_count, " +
            "SUM(CASE WHEN deadline < completed_at THEN 1 ELSE 0 END) as overdue_count " +
            "FROM sop_execution " +
            "WHERE status = 'completed' AND completed_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY executor_id ORDER BY completed_count DESC LIMIT 10")
    List<Map<String, Object>> getMemberLeaderboard();
}
