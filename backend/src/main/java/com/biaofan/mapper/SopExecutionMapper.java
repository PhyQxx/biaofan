package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.SopExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface SopExecutionMapper extends BaseMapper<SopExecution> {

    @Select("SELECT COUNT(*) FROM sop_execution WHERE status = 'pending' AND deadline < NOW()")
    int countOverdue(@Param("now") LocalDateTime now);

    @Select("SELECT COUNT(*) FROM sop_execution WHERE DATE(created_at) = CURDATE()")
    int countTodayTotal();

    @Select("SELECT COUNT(*) FROM sop_execution WHERE DATE(completed_at) = CURDATE() AND status = 'completed'")
    int countTodayCompleted();

    @Select("SELECT COUNT(*) FROM sop_execution WHERE DATE(completed_at) = CURDATE() AND status = 'completed' AND deadline < completed_at")
    int countTodayOverdue();

    @Select("SELECT COUNT(DISTINCT executor_id) FROM sop_execution WHERE status IN ('pending', 'completed')")
    int countActiveMembers();

    @Select("SELECT DATE(completed_at) as date, COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) as completed " +
            "FROM sop_execution " +
            "WHERE completed_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(completed_at) ORDER BY date ASC")
    List<Map<String, Object>> getCompletionTrend(@Param("days") int days);

    @Select("SELECT executor_id, COUNT(*) as completed_count, " +
            "SUM(CASE WHEN deadline < completed_at THEN 1 ELSE 0 END) as overdue_count " +
            "FROM sop_execution " +
            "WHERE status = 'completed' AND completed_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY executor_id ORDER BY completed_count DESC LIMIT 10")
    List<Map<String, Object>> getMemberLeaderboard();
}
