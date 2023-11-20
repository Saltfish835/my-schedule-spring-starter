package com.example.middleware.schedule.service.taskService;

import com.example.middleware.schedule.domain.task.base.MethodTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * 将用户定义的任务封装成Spring可以调度的类型
 * 也就是Runnable类型
 */
public class SchedulingRunnable implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(SchedulingRunnable.class);

    private MethodTask methodTask;

    /**
     * 需要传入标记有Schedule注解的方法
     * @param methodTask
     */
    public SchedulingRunnable(MethodTask methodTask) {
        this.methodTask = methodTask;
    }

    @Override
    public void run() {
        try {
            // 拿到任务对应的方法
            final Method method = methodTask.getBean().getClass().getDeclaredMethod(methodTask.getMethodName());
            // 通过反射的方法调用任务定义的方法
            ReflectionUtils.makeAccessible(method);
            method.invoke(methodTask.getBean());
        }catch (Exception e) {
            logger.error("middleware mySchedule error!",e);
        }
    }

    public MethodTask getMethodTask() {
        return methodTask;
    }
}
