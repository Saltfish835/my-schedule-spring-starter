package com.example.middleware.schedule.common;

import com.example.middleware.schedule.domain.task.base.BaseTask;
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

    // 记录当前项目中所有定时任务,<beanName, taskList>
    public static final Map<String, List<BaseTask>> execOrderMap = new ConcurrentHashMap<>();


    public static class Global {
        public static ApplicationContext applicationContext;
        public static String zkAddress;
        public static String ip;
        public static CuratorFramework client;
        public static String CHARSET_NAME = "utf-8";
        public static String path_root = "/my-scheduler"; // zk根路径
        public static String path_root_exec = path_root + "/exec"; // 任务都会监听此路来实现对任务的启停
        public static String path_root_tasks = path_root + "/tasks";
    }
}
