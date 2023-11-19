package com.example.middleware.schedule.config;

import com.example.middleware.schedule.annotation.scheduleAnnotation.CronSchedule;
import com.example.middleware.schedule.annotation.scheduleAnnotation.SimpleSchedule;
import com.example.middleware.schedule.common.Constants;
import com.example.middleware.schedule.domain.task.CronMethodTask;
import com.example.middleware.schedule.domain.task.SimpleMethodTask;
import com.example.middleware.schedule.domain.task.base.BaseTask;
import com.example.middleware.schedule.domain.task.base.MethodTask;
import com.example.middleware.schedule.service.taskService.SchedulingRunnable;
import com.example.middleware.schedule.service.taskService.TaskRegister;
import com.example.middleware.schedule.service.ZkCuratorServer;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 分布式任务调度的核心配置
 * 用于扫描出项目中定义的任务
 * 连接zookeeper
 * 挂载任务节点
 * @author yuhe
 */
public class MyScheduleConfiguration implements ApplicationContextAware, BeanPostProcessor, ApplicationListener<ContextRefreshedEvent> {

    private final Logger logger = LoggerFactory.getLogger(MyScheduleConfiguration.class);

    private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));


    /**
     * MyScheduleConfiguration在初始化时会执行这个方法
     * 通过此方法拿到IoC容器，保存到全局变量中
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Constants.Global.applicationContext = applicationContext;
    }


    /**
     * 每个bean在初始化的时候都会执行这个方法
     * 在此方法中可以扫描每个bean的每个方法，将带有Schedule注解的方法保存起来
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 获取一个代理对象的最终对象类型
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        // 如果这个bean已经被扫描过了就直接跳过
        if(this.nonAnnotatedClasses.contains(targetClass)) {
            return bean;
        }
        // 拿到这个类的所有方法
        Method[] methods = targetClass.getMethods();
        // 遍历这个类中的所有方法
        for(Method method: methods) {
            // 判断这个方法上是否有Schedule相关的注解
            CronSchedule cronSchedule = AnnotationUtils.findAnnotation(method, CronSchedule.class);
            SimpleSchedule simpleSchedule = AnnotationUtils.findAnnotation(method, SimpleSchedule.class);
            // 当前方法没有注解，或者没有Scheduler相关的注解
            if(method.getDeclaredAnnotations().length ==0 || (cronSchedule == null && simpleSchedule == null)) {
                continue;
            }
            // 当前这个方法的头上有Schedule相关的注解
            List<MethodTask> taskList = Constants.execOrderMap.computeIfAbsent(beanName, k -> new ArrayList<>());
            // 从注解中获取描述信息，封装定时任务
            if(cronSchedule != null) {
                // 当前是cron任务
                CronMethodTask task = new CronMethodTask();
                task.setBean(bean);
                task.setBeanName(beanName);
                task.setMethodName(method.getName());
                task.setTaskName(cronSchedule.taskName());
                task.setCron(cronSchedule.cron());
                task.setDesc(cronSchedule.desc());
                task.setAutoStartup(cronSchedule.isAutoStartup());
                taskList.add(task);
            }else if (simpleSchedule != null) {
                // 当前是简单任务
                SimpleMethodTask task = new SimpleMethodTask();
                task.setBean(bean);
                task.setBeanName(beanName);
                task.setMethodName(method.getName());
                task.setTaskName(simpleSchedule.taskName());
                task.setStartTime(simpleSchedule.startTime());
                task.setDesc(simpleSchedule.desc());
                task.setAutoStartup(true); // 简单任务只有自启
                taskList.add(task);
            }
            // 当前这个类已经扫描过了，就记下来，避免下次被重复处理
            this.nonAnnotatedClasses.add(targetClass);
        }
        return bean;
    }


    /**
     * 当所有bean都加载完后，也就代表项目中定义的任务都扫描出来了
     * 然后就可以开始调度任务
     * @param contextRefreshedEvent
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        ApplicationContext applicationContext = contextRefreshedEvent.getApplicationContext();
        // 初始化配置
        initConfig(applicationContext);
        // 初始化服务
        initServer(applicationContext);
        // 启动这个项目中包含的定时任务
        initTask(applicationContext);
    }

    /**
     * 初始化配置
     * 包括zookeeper配置
     * @param applicationContext
     */
    private void initConfig(ApplicationContext applicationContext) {
        try {
            // 获取配置文件
            StarterServiceProperties properties = applicationContext
                    .getBean("middleware-mySchedule-starterAutoConfig", StarterAutoConfig.class).getProperties();
            // 拿到配置文件中的信息，并保存到全局常量中
            Constants.Global.zkAddress = properties.getZkAddress();
            InetAddress inetAddress = InetAddress.getLocalHost();
            Constants.Global.ip = inetAddress.getHostAddress();
        }catch (Exception e) {
            logger.error("my schedule  init config error!",e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化服务
     * @param applicationContext
     */
    private void initServer(ApplicationContext applicationContext) {
        try {
            // 获取zookeeper客户端
            CuratorFramework client = ZkCuratorServer.getClient(Constants.Global.zkAddress);
            // 创建tasks节点
            ZkCuratorServer.createNode(client, Constants.Global.path_root_tasks, "");
        }catch (Exception e) {
            logger.error("my schedule init server error!",e);
        }
    }


    /**
     * 初始化任务
     * @param applicationContext
     */
    private void initTask(ApplicationContext applicationContext) {
        // 获取操作控制任务启停的对象
        final TaskRegister taskRegister = applicationContext.getBean("middleware-mySchedule-taskRegister", TaskRegister.class);
        // 从当前项目中扫出来的任务
        final Set<String> beanNames = Constants.execOrderMap.keySet();
        // 遍历这个项目中包含定时任务的bean
        for(String beanName: beanNames) {
            // 根据beanName拿到这个bean中声明的所有任务
            final List<MethodTask> taskList = Constants.execOrderMap.get(beanName);
            // 遍历这个bean下的所有任务
            for(MethodTask methodTask : taskList) {
                // 将任务封装成Spring可以调度的形式
                final SchedulingRunnable springTask = new SchedulingRunnable(methodTask);
                // 使用Spring提供的TaskScheduler类来实现正在的调度执行
                taskRegister.initSpringTask(springTask);
            }
        }
    }

}
