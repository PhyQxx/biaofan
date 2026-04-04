package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.ScheduleTask;
import com.biaofan.entity.Sop;
import com.biaofan.mapper.ScheduleTaskMapper;
import com.biaofan.mapper.SopMapper;
import com.biaofan.service.ScheduleTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleTaskServiceImpl implements ScheduleTaskService {

    private final ScheduleTaskMapper scheduleTaskMapper;
    private final SopMapper sopMapper;

    @Override
    public ScheduleTask getBySopId(Long sopId, Long userId) {
        return scheduleTaskMapper.selectOne(new LambdaQueryWrapper<ScheduleTask>()
                .eq(ScheduleTask::getSopId, sopId)
                .eq(ScheduleTask::getUserId, userId));
    }

    @Override
    public void create(Long sopId, Long userId, String cronExpression) {
        // 删除旧的
        scheduleTaskMapper.delete(new LambdaQueryWrapper<ScheduleTask>()
                .eq(ScheduleTask::getSopId, sopId)
                .eq(ScheduleTask::getUserId, userId));

        ScheduleTask task = new ScheduleTask();
        task.setSopId(sopId);
        task.setUserId(userId);
        task.setCronExpression(cronExpression);
        task.setNextFireTime(calcNextFireTime(cronExpression));
        task.setEnabled(1);
        scheduleTaskMapper.insert(task);
    }

    @Override
    public void delete(Long sopId, Long userId) {
        scheduleTaskMapper.delete(new LambdaQueryWrapper<ScheduleTask>()
                .eq(ScheduleTask::getSopId, sopId)
                .eq(ScheduleTask::getUserId, userId));
    }

    @Override
    public void updateEnabled(Long id, Long userId, Integer enabled) {
        ScheduleTask task = scheduleTaskMapper.selectById(id);
        if (task == null || !task.getUserId().equals(userId)) return;
        task.setEnabled(enabled);
        if (enabled == 1) {
            task.setNextFireTime(calcNextFireTime(task.getCronExpression()));
        }
        scheduleTaskMapper.updateById(task);
    }

    @Override
    public List<ScheduleTask> getDueTasks() {
        return scheduleTaskMapper.findDueTasks(LocalDateTime.now());
    }

    @Override
    public List<ScheduleTask> getMyTasks(Long userId) {
        return scheduleTaskMapper.selectList(new LambdaQueryWrapper<ScheduleTask>()
                .eq(ScheduleTask::getUserId, userId)
                .orderByDesc(ScheduleTask::getCreatedAt));
    }

    @Override
    public void advanceNextFireTime(Long id, String cronExpression) {
        ScheduleTask task = scheduleTaskMapper.selectById(id);
        if (task != null) {
            task.setNextFireTime(calcNextFireTime(cronExpression));
            scheduleTaskMapper.updateById(task);
        }
    }

    private LocalDateTime calcNextFireTime(String cronExpression) {
        try {
            // cron格式: 分 时 日 月 周
            // 例如: 0 8 * * * = 每天8点
            // 0 8 ? * 1 = 每周一8点
            // 0 8 1 * * = 每月1号8点
            String[] parts = cronExpression.trim().split("\\s+");
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, 1);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            int minute = cal.get(Calendar.MINUTE);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            int month = cal.get(Calendar.MONTH) + 1;
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            // Calendar.DAY_OF_WEEK: 1=周日,2=周一...
            int weekDay = dayOfWeek == 1 ? 7 : dayOfWeek - 1; // 转换为周一=1,周日=7

            // 每分钟检查，最快匹配到下一分钟
            for (int i = 0; i < 525600; i++) { // 最多查1年
                boolean match = true;
                if (!parts[0].equals("*")) {
                    int cronMin = Integer.parseInt(parts[0]);
                    if (cronMin != minute) match = false;
                }
                if (match && !parts[1].equals("*")) {
                    int cronHour = Integer.parseInt(parts[1]);
                    if (cronHour != hour) match = false;
                }
                if (match && !parts[2].equals("*") && !parts[2].equals("?")) {
                    int cronDay = Integer.parseInt(parts[2]);
                    if (cronDay != dayOfMonth) match = false;
                }
                if (match && !parts[3].equals("*")) {
                    int cronMonth = Integer.parseInt(parts[3]);
                    if (cronMonth != month) match = false;
                }
                if (match && parts.length > 4 && !parts[4].equals("*") && !parts[4].equals("?")) {
                    int cronDow = Integer.parseInt(parts[4]);
                    if (cronDow != weekDay) match = false;
                }

                if (match) {
                    return LocalDateTime.of(cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH),
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE));
                }
                cal.add(Calendar.MINUTE, 1);
                minute = cal.get(Calendar.MINUTE);
                hour = cal.get(Calendar.HOUR_OF_DAY);
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                month = cal.get(Calendar.MONTH) + 1;
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                weekDay = dayOfWeek == 1 ? 7 : dayOfWeek - 1;
            }
        } catch (Exception e) {
            // fallback: 1小时后
        }
        return LocalDateTime.now().plusHours(1).withSecond(0).withNano(0);
    }
}
