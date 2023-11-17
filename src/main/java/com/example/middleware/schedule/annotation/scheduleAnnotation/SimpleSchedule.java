package com.example.middleware.schedule.annotation.scheduleAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 只执行一次
 * 按照指定时间点开始调度执行
 * @author yuhe
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SimpleSchedule{

    // 任务名称
    String taskName();

    // 开始调度执行的时间
    String startTime();

    // 任务描述
    String desc() default "";
}
