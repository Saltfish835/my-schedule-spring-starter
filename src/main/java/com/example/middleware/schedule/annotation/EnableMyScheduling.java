package com.example.middleware.schedule.annotation;

import com.example.middleware.schedule.config.MyScheduleConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启自动调度
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({MyScheduleConfiguration.class})
public @interface EnableMyScheduling {
}
