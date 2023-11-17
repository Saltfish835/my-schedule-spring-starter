package com.example.middleware.schedule.test;

import com.example.middleware.schedule.annotation.scheduleAnnotation.CronSchedule;
import com.example.middleware.schedule.annotation.scheduleAnnotation.SimpleSchedule;
import org.springframework.stereotype.Component;

@Component
public class TaskTest {

    @SimpleSchedule(taskName = "simple_task_1",startTime = "2023-11-16 16:15:42")
    public void test1() {
        System.out.println("this is test simple task...");
    }

    @CronSchedule(taskName = "cron_task_2",cron = "0/5 * *  * * *", isAutoStartup = true)
    public void test2() {
        System.out.println("this is test cron task...");
    }

}
