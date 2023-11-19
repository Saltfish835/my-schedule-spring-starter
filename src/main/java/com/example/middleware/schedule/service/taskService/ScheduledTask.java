package com.example.middleware.schedule.service.taskService;

import java.util.concurrent.ScheduledFuture;

/**
 * 保存任务调度的返回值
 */
public class ScheduledTask {
    volatile ScheduledFuture<?> future;

    /**
     * 取消定时任务
     */
    public void cancel() {
        ScheduledFuture<?> future = this.future;
        if(future == null) {
            return;
        }
        future.cancel(true);
    }


    /**
     * 判断定时任务是否取消
     * @return
     */
    public boolean isCancelled() {
        ScheduledFuture<?> future = this.future;
        if(future == null) {
            return true;
        }
        return future.isCancelled();
    }
}
