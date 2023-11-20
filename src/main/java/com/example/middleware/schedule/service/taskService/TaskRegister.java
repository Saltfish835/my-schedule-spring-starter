package com.example.middleware.schedule.service.taskService;

import com.example.middleware.schedule.common.Constants;
import com.example.middleware.schedule.domain.task.CronMethodTask;
import com.example.middleware.schedule.domain.task.SimpleMethodTask;
import com.example.middleware.schedule.domain.task.base.MethodTask;
import com.example.middleware.schedule.service.ZkCuratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * 管理定时任务的工具类
 */
@Component("middleware-mySchedule-taskRegister")
public class TaskRegister implements DisposableBean {

    private static Logger logger = LoggerFactory.getLogger(TaskRegister.class);

    /**
     * 将真正执行定时任务的类注入进来
     */
    @Resource(name="middleware-mySchedule-springTaskScheduler")
    private TaskScheduler taskScheduler;


    /**
     * 往Spring的TaskScheduler对象中添加一个任务来调度执行
     * @param springTask
     */
    public void initSpringTask(SchedulingRunnable springTask) {
        try{
            // 先为该任务在zookeeper创建对应的节点
            createZnodeWithTask(springTask);
            // 将当前任务状态设置为就绪态
            setTaskStatus(springTask,Constants.Global.PREPARED);
            // 当前任务需要自启
            if(springTask.getMethodTask().getAutoStartup()) {
                runSpringTask(springTask);
            }
            // 如果当前任务已经有实例在被调度执行，那么就先监听着taskName节点被移除的事件，当leader被删除，那么该任务就可以被调度执行
            // 同时监听exec节点来控制任务启停
            ZkCuratorService.addPathChildrenCacheForTaskName(Constants.Global.client, springTask);
            logger.info("任务["+springTask.getMethodTask().getTaskName()+"]初始化成功");
        }catch (Exception e) {
            logger.error("middleware mySchedule init task error!",e);
        }
    }


    /**
     * 故障转移
     * @param springTask
     */
    public void failover(SchedulingRunnable springTask) {
        runSpringTask(springTask);
    }



    /**
     * 执行任务
     * @param springTask
     */
    public void runSpringTask(SchedulingRunnable springTask) {
        try {
            // 当前任务不在本地列表中，不能调度执行
            if(!isExistInLocal(springTask)) {
                logger.info("任务["+springTask.getMethodTask().getTaskName()+"]不在本地列表中，不能被调度执行");
                return;
            }
            // 当前leader不存在，可以进行调度执行
            if(!ZkCuratorService.isExists(Constants.Global.client,getTaskLeaderPath(springTask))) {
                // 如果这个任务已经在调度列表中了，就把这个任务先清除掉
                if(Constants.scheduledTasks.get(springTask.getMethodTask().getTaskName()) != null) {
                    removeTask(springTask.getMethodTask().getTaskName());
                }
                // 先在zookeeper中创建该任务对应的leader节点，代表该任务在调度执行
                ZkCuratorService.createEphemeralNode(Constants.Global.client,
                        getTaskLeaderPath(springTask),getTaskInstanceName(springTask));
                // 然后再把任务交给Spring真正调度执行
                ScheduledTask scheduledTask = scheduleTask(springTask);
                // 把当前调度执行的任务记录下来
                Constants.scheduledTasks.put(springTask.getMethodTask().getTaskName(), scheduledTask);
                // 把当前任务设置为运行态
                setTaskStatus(springTask,Constants.Global.RUNNING);
                logger.info("任务["+springTask.getMethodTask().getTaskName()+"]被调度执行");
            }else {
                logger.info("leader节点被["+ ZkCuratorService.getData(Constants.Global.client,getTaskLeaderPath(springTask))+"]注册，任务["+springTask.getMethodTask().getTaskName()+"]无法被调度执行");
            }
        }catch (Exception e) {
            logger.error("middleware mySchedule run task error!",e);
        }
    }


    /**
     * 重启任务
     * @param springTask
     */
    public void restartSpringTask(SchedulingRunnable springTask) {
        try{
            // 只有占有leader节点且处于停止状态的任务才能重启
            final String leader = ZkCuratorService.getData(Constants.Global.client, getTaskLeaderPath(springTask));
            final String status = ZkCuratorService.getData(Constants.Global.client, getTaskInstanceStatusPath(springTask));
            if((leader != null && leader.equals(getTaskInstanceName(springTask)) && (status != null && status.equals(Constants.Global.STOPPED)))) {
                // 从Spring中取消掉该任务
                if(Constants.scheduledTasks.get(springTask.getMethodTask().getTaskName()) != null) {
                    removeTask(springTask.getMethodTask().getTaskName());
                }
                // 然后再把任务交给Spring真正调度执行
                ScheduledTask scheduledTask = scheduleTask(springTask);
                // 把当前调度执行的任务记录下来
                Constants.scheduledTasks.put(springTask.getMethodTask().getTaskName(), scheduledTask);
                // 把当前任务设置为运行态
                setTaskStatus(springTask,Constants.Global.RUNNING);
                logger.info("任务["+springTask.getMethodTask().getTaskName()+"]重启成功");
            }else {
                logger.warn("任务["+springTask.getMethodTask().getTaskName()+"]无法重启，当前任务的leader是["+leader+"]，当前状态是["+status+"].");
            }
        }catch (Exception e) {
            logger.error("middleware mySchedule restart task error!",e);
        }
    }



    /**
     * 停止任务
     * @param springTask
     */
    public void stopSpringTask(SchedulingRunnable springTask) {
        try {
            // 当前处于运行状态的任务实例才能停止
            final String leader = ZkCuratorService.getData(Constants.Global.client, getTaskLeaderPath(springTask));
            final String status = ZkCuratorService.getData(Constants.Global.client, getTaskInstanceStatusPath(springTask));
            if((leader != null && leader.equals(getTaskInstanceName(springTask)) && (status != null && status.equals(Constants.Global.RUNNING)))) {
                // 从Spring中取消掉该任务
                if(Constants.scheduledTasks.get(springTask.getMethodTask().getTaskName()) != null) {
                    removeTask(springTask.getMethodTask().getTaskName());
                }
                // 把当前任务设置为停止状态
                setTaskStatus(springTask,Constants.Global.STOPPED);
                logger.info("当前任务["+springTask.getMethodTask().getTaskName()+"]停止成功");
            }else {
                logger.warn("当前任务无法停止，当前任务leader是["+leader+"]，当前状态是["+status+"].");
            }
        }catch (Exception e) {
            logger.error("middleware mySchedule stop task error!",e);
        }
    }



    /**
     * 删除任务
     * @param springTask
     */
    public void delete(SchedulingRunnable springTask) {
        try {
            // 如果这个任务已经在调度列表中了，就把这个任务先清除掉
            if(Constants.scheduledTasks.get(springTask.getMethodTask().getTaskName()) != null) {
                removeTask(springTask.getMethodTask().getTaskName());
            }
            // 然后把任务从本地任务列表中删除
            deleteTaskFromLocal(springTask);
            // 如果这个任务占有leader节点，还要把leader节点删掉
            if(ZkCuratorService.getData(Constants.Global.client,getTaskLeaderPath(springTask)).equals(getTaskInstanceName(springTask))) {
                ZkCuratorService.deleteZnode(Constants.Global.client,getTaskLeaderPath(springTask));
                logger.info("当前任务["+springTask.getMethodTask().getTaskName()+"]占用的leader节点被删除");
            }
        }catch (Exception e) {
            logger.info("当前任务["+springTask.getMethodTask().getTaskName()+"]占用的leader节点删除失败",e);
        }
    }


    /**
     * 从本地列表中删除任务
     * @param springTask
     */
    public void deleteTaskFromLocal(SchedulingRunnable springTask) {
        List<MethodTask> methodTasks = Constants.execOrderMap.get(springTask.getMethodTask().getBeanName());
        Iterator<MethodTask> methodTaskIterator = methodTasks.iterator();
        while(methodTaskIterator.hasNext()) {
            MethodTask methodTask = methodTaskIterator.next();
            if(methodTask.getTaskName().equals(springTask.getMethodTask().getTaskName())) {
                methodTaskIterator.remove();
                logger.info("从本地任务列表中删除任务["+methodTask.getTaskName()+"]成功");
            }
        }
    }


    /**
     * 判断任务是否在本地列表中存在
     * @param springTask
     * @return
     */
    public boolean isExistInLocal(SchedulingRunnable springTask) {
        List<MethodTask> methodTasks = Constants.execOrderMap.get(springTask.getMethodTask().getBeanName());
        Iterator<MethodTask> methodTaskIterator = methodTasks.iterator();
        while(methodTaskIterator.hasNext()) {
            MethodTask methodTask = methodTaskIterator.next();
            if(methodTask.getTaskName().equals(springTask.getMethodTask().getTaskName())) {
                return true;
            }
        }
        return false;
    }


    /**
     * 将任务交给Spring来实现真正的调度执行
     * @param springTask
     * @return
     * @throws ParseException
     */
    private ScheduledTask scheduleTask(SchedulingRunnable springTask) throws ParseException {
        ScheduledTask scheduledTask = new ScheduledTask();
        final MethodTask methodTask = springTask.getMethodTask();
        if(methodTask instanceof SimpleMethodTask) {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            final Date date = format.parse(((SimpleMethodTask) methodTask).getStartTime());
            // 将任务交给Spring来实现正在调度执行
            scheduledTask.future = this.taskScheduler.schedule(springTask,date);
        }else if(methodTask instanceof CronMethodTask) {
            final String cron = ((CronMethodTask) methodTask).getCron();
            final CronTask cronTask = new CronTask(springTask, cron);
            // 将任务将给Spring来实现真正的调度执行
            scheduledTask.future = this.taskScheduler.schedule(cronTask.getRunnable(),cronTask.getTrigger());
        }
        return scheduledTask;
    }


    /**
     * 取消任务
     * @param taskName
     */
    public void removeTask(String taskName) {
        // 先把该任务从调度列表中清除
        ScheduledTask scheduledTask = Constants.scheduledTasks.remove(taskName);
        // 然后把该任务从取消掉
        if(scheduledTask == null) {
            return;
        }
        scheduledTask.cancel();
        logger.info("从调度列表中删除任务["+taskName+"]成功");
    }



    /**
     * 把任务注册到zk中去
     * 在zk中创建对应的节点
     * @param springTask
     */
    public void createZnodeWithTask(SchedulingRunnable springTask) throws Exception {

        // 将任务名称注册到zk，并生成持久化节点
        // /my_scheduler/tasks/cron_task_1
        ZkCuratorService.createNode(Constants.Global.client, getTaskNamePath(springTask));

        // 生成一个任务对应的持久化实例节点
        // /my_scheduler/tasks/cron_task_1/cron_task_1_192_168_122_1
        ZkCuratorService.createNode(Constants.Global.client, getTaskInstancePath(springTask), null);

        // 生成一个任务对应的持久化命令节点，用于启停任务
        // /my_scheduler/tasks/cron_tas_1/exec
        ZkCuratorService.createNode(Constants.Global.client,getTaskExecPath(springTask),null);

        // 在任务实例下创建一个任务状态的临时节点
        ZkCuratorService.createEphemeralNode(Constants.Global.client, getTaskInstanceStatusPath(springTask), null);
    }


    /**
     * 为任务设置状态
     * @param springTask
     * @param status
     */
    public void setTaskStatus(SchedulingRunnable springTask, String status) {
        try {
            ZkCuratorService.setData(Constants.Global.client,getTaskInstanceStatusPath(springTask),status);
            springTask.getMethodTask().setStatus(status);
            logger.info("任务["+springTask.getMethodTask().getTaskName()+"]被设置成["+status+"]状态");
        } catch (Exception e) {
            logger.error("为任务["+springTask.getMethodTask().getTaskName()+"]设置状态["+status+"]出错！",e);
        }
    }


    public static String getTaskNamePath(SchedulingRunnable springTask) {
        return Constants.Global.path_root_tasks + Constants.Global.LINE + springTask.getMethodTask().getTaskName();
    }

    public static String getTaskLeaderPath(SchedulingRunnable springTask) {
        return getTaskNamePath(springTask) + Constants.Global.LINE + Constants.Global.LEADER;
    }

    public static String getTaskExecPath(SchedulingRunnable springTask) {
        return getTaskNamePath(springTask) + Constants.Global.LINE + Constants.Global.EXEC;
    }

    public static String getTaskInstanceName(SchedulingRunnable springTask) {
        return springTask.getMethodTask().getTaskName()+"_"+Constants.Global.ip.replace(".","_");
    }

    public static String getTaskInstancePath(SchedulingRunnable springTask) {
        return getTaskNamePath(springTask) + Constants.Global.LINE + getTaskInstanceName(springTask);
    }

    public static String getTaskInstanceStatusPath(SchedulingRunnable springTask) {
        return getTaskInstancePath(springTask) + Constants.Global.LINE + Constants.Global.STATUS;
    }


    /**
     * 容器关闭的时候
     * 将所有任务清空
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        for(ScheduledTask scheduledTask: Constants.scheduledTasks.values()) {
            scheduledTask.cancel();
        }
        Constants.scheduledTasks.clear();
    }
}
