package com.example.middleware.schedule.domain.task.base;

/**
 * 任务的基类
 * @author yuhe
 */
public abstract class BaseTask {

    // 任务名称
    protected String taskName;

    // 任务描述
    protected String desc;

    // 任务运行时间
    protected Long runTime;

    // 任务状态
    protected String status;

    // 是否自启
    protected Boolean isAutoStartup;

    public Boolean getAutoStartup() {
        return isAutoStartup;
    }

    public void setAutoStartup(Boolean autoStartup) {
        isAutoStartup = autoStartup;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Long getRunTime() {
        return runTime;
    }

    public void setRunTime(Long runTime) {
        this.runTime = runTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
