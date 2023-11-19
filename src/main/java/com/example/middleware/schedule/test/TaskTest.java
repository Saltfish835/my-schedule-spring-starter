package com.example.middleware.schedule.test;

import com.example.middleware.schedule.annotation.scheduleAnnotation.CronSchedule;
import com.example.middleware.schedule.annotation.scheduleAnnotation.SimpleSchedule;
import org.springframework.stereotype.Component;

@Component
public class TaskTest {

    @SimpleSchedule(taskName = "simple_task_local",startTime = "2023-11-29 16:15:42")
    public void test1() {
        System.out.println("this is test simple task...");
    }

    @CronSchedule(taskName = "cron_task_local",cron = "0/10 * *  * * *", isAutoStartup = true)
    public void test2() {
        System.out.println("this is test cron task...");
    }

}
