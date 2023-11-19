package com.example.middleware.schedule.config;

import com.example.middleware.schedule.common.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 创建Spring提供的，真正用于用于任务调度的TaskScheduler类的对象
 */
@Configuration("middleware-mySchedule-schedulingConfig")
public class SchedulingConfig {

    /**
     * 创建Spring提供的真正用于执行定时任务的TaskScheduler类的对象
     * @return
     */
    @Bean("middleware-mySchedule-springTaskScheduler")
    public TaskScheduler taskScheduler() {
        final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(Constants.Global.schedulePoolSize);
        taskScheduler.setRemoveOnCancelPolicy(true);
        taskScheduler.setThreadNamePrefix("myScheduleThreadPool-");
        return taskScheduler;
    }
}
