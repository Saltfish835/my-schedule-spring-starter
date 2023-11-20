package com.example.middleware.schedule.domain.task;

import com.example.middleware.schedule.domain.task.base.BaseTask;
import com.example.middleware.schedule.domain.task.base.MethodTask;

/**
 * 使用cron表达式来调度的任务
 * @author yuhe
 */
public class CronMethodTask extends MethodTask {

    /**
     * 任务的cron表达式
     */
    private String cron;

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }
}
