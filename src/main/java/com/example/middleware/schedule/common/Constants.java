package com.example.middleware.schedule.common;

import com.example.middleware.schedule.domain.task.base.BaseTask;
import com.example.middleware.schedule.domain.task.base.MethodTask;
import com.example.middleware.schedule.service.taskService.ScheduledTask;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局通用常量
 * @author yuhe
 */
public class Constants {

    // 本地任务列表 beanName - methodTaskList
    public static final Map<String, List<MethodTask>> execOrderMap = new ConcurrentHashMap<>();
    // 已经被调度的任务列表 taskName - scheduledTask
    public static final Map<String, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>(16);


    public static class Global {
        public static ApplicationContext applicationContext;
        public static String zkAddress;
        public static String ip;
        public static CuratorFramework client;
        public static final String LINE = "/";
        public static final String LEADER = "leader";
        public static final String EXEC = "exec"; // 任务都会监听此路来实现对任务的启停
        public static final String STATUS = "status";
        public static final String PREPARED = "1";
        public static final String RUNNING = "2";
        public static final String STOPPED = "3";
        public static final String CHARSET_NAME = "utf-8";
        public static int schedulePoolSize = 8; // 定时任务执行线程池的核心线程数
        public static String path_root = "/my_scheduler"; // zk根路径
        public static String path_root_tasks = path_root + "/tasks";
    }
}
