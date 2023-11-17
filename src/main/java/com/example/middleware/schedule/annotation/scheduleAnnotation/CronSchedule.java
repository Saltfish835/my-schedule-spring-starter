package com.example.middleware.schedule.annotation.scheduleAnnotation;

import java.lang.annotation.*;

/**
 * 周期任务
 * 按照cron表达式来调度执行
 * @author yuhe
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CronSchedule{

    // 任务名称
    String taskName();

    // cron表达式
    String cron() default "";

    // 任务描述
    String desc() default "";

    // 是否自启
    boolean isAutoStartup() default true;

}
