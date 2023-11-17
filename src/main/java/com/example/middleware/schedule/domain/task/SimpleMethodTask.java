package com.example.middleware.schedule.domain.task;

import com.example.middleware.schedule.domain.task.base.MethodTask;

public class SimpleMethodTask extends MethodTask {

    // 开始调度执行的时间点
    private String startTime;

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
}


