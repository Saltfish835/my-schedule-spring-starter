package com.example.middleware.schedule.annotation;

import com.example.middleware.schedule.config.MyScheduleConfiguration;
import com.example.middleware.schedule.config.SchedulingConfig;
import com.example.middleware.schedule.service.taskService.TaskRegister;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启自动调度
 * @author yuhe
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({MyScheduleConfiguration.class})
@ImportAutoConfiguration({SchedulingConfig.class, TaskRegister.class})
@ComponentScan("com.example.middleware.*")
public @interface EnableMyScheduling {
}
