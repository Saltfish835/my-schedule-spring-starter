package com.example.middleware.schedule.service;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.middleware.schedule.common.Constants;
import com.example.middleware.schedule.service.taskService.ScheduledTask;
import com.example.middleware.schedule.service.taskService.SchedulingRunnable;
import com.example.middleware.schedule.service.taskService.TaskRegister;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;

/**
 * 操作zk的工具类
 * @author yuhe
 */
public class ZkCuratorServer {
    private static Logger logger = LoggerFactory.getLogger(ZkCuratorServer.class);

    /**
     * 获取操作zookeeper的客户端
     * @param connectString
     * @return
     */
    public static CuratorFramework getClient(String connectString) {
        if(Constants.Global.client != null) {
            return Constants.Global.client;
        }
        // 创建客户端
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(connectString, retryPolicy);
        // 添加重连监听
        client.getConnectionStateListenable().addListener((curatorFramework, connectionState) -> {
            switch (connectionState) {
                case CONNECTED:
                    logger.info("example middleware schedule init server connected {}", connectString);
                    break;
                case RECONNECTED:
                    logger.info("example middleware schedule reconnected {}", connectString);
                    break;
                default:
                    break;
            }
        });
        client.start();
        Constants.Global.client = client;
        return client;
    }

    /**
     * 创建多级持久节点
     * @param client
     * @param path
     */
    public static void createNode(CuratorFramework client, String path) throws Exception {
        if(client.checkExists().forPath(path) == null) {
            // 创建永久的多级节点
            client.create().creatingParentsIfNeeded().forPath(path);
            logger.debug("持久节点["+path+"]被创建");
        }
    }

    /**
     * 创建多级持久节点的同时为节点设置值
     * @param client
     * @param path
     * @param data
     * @throws Exception
     */
    public static void createNode(CuratorFramework client, String path, String data) throws Exception {
        if(data == null) {
            data = "";
        }
        if(client.checkExists().forPath(path) == null) {
            // 创建永久的多级节点
            client.create().creatingParentsIfNeeded().forPath(path,data.getBytes(Constants.Global.CHARSET_NAME));
            logger.debug("持久节点["+path+"]被创建，数据是["+data+"]");
        }
    }


    /**
     * 创建多级临时节点
     * @param client
     * @param path
     * @throws Exception
     */
    public static void createEphemeralNode(CuratorFramework client, String path) throws Exception {
        if(client.checkExists().forPath(path) == null) {
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
            logger.debug("临时节点["+path+"]被创建");
        }
    }


    /**
     * 床啊金多级临时节点的同时为节点设置值
     * @param client
     * @param path
     * @param data
     * @throws Exception
     */
    public static void createEphemeralNode(CuratorFramework client, String path, String data) throws Exception {
        if(data == null) {
            data = "";
        }
        if(client.checkExists().forPath(path) == null) {
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
                    .forPath(path, data.getBytes(Constants.Global.CHARSET_NAME));
            logger.debug("临时节点["+path+"]被创建，数据是["+data+"]");
        }
    }


    /**
     * 为节点设置值
     * @param client
     * @param path
     * @param data
     * @throws Exception
     */
    public static void setData(CuratorFramework client, String path, String data) throws Exception {
        if(client.checkExists().forPath(path) == null || data == null) {
            return;
        }
        client.setData().forPath(path,data.getBytes(Constants.Global.CHARSET_NAME));
        logger.debug("为["+path+"]节点设置值["+data+"]");
    }


    /**
     * 获取节点数据
     * @param client
     * @param path
     * @return
     * @throws Exception
     */
    public static String getData(CuratorFramework client, String path) throws Exception {
        if(client.checkExists().forPath(path) == null) {
            return null;
        }
        final byte[] bytes = client.getData().forPath(path);
        return new String(bytes,Constants.Global.CHARSET_NAME);
    }



    /**
     * 判断节点是否存在
     * @param client
     * @param path
     * @return
     */
    public static boolean isExists(CuratorFramework client, String path) throws Exception {
        if(client.checkExists().forPath(path) == null) {
            return false;
        }else {
            return true;
        }
    }

    /**
     * 删除节点
     * @param client
     * @param path
     * @throws Exception
     */
    public static void deleteZnode(CuratorFramework client, String path) throws Exception {
        if(client.checkExists().forPath(path) != null) {
            client.delete().forPath(path);
        }
    }


    /**
     * 给taskName节点加上监听器，用于实现故障转移
     * @param client
     * @param springTask
     * @throws Exception
     */
    public static void addPathChildrenCacheForTaskName(final CuratorFramework client, SchedulingRunnable springTask) throws Exception {
        final PathChildrenCache pathChildrenCache  = new PathChildrenCache(client, TaskRegister.getTaskNamePath(springTask),true);
        pathChildrenCache .getListenable().addListener((curatorFramework, pathChildrenEvent) -> {
            String path = null;
            final TaskRegister taskRegister = Constants.Global.applicationContext
                    .getBean("middleware-mySchedule-taskRegister", TaskRegister.class);
            switch (pathChildrenEvent.getType()) {
                case CHILD_REMOVED:
                    path = pathChildrenEvent.getData().getPath();
                    /**
                     * 当前任务的leader节点被移除了
                     * 需要进行故障转移
                     */
                    if(path.equals(TaskRegister.getTaskLeaderPath(springTask))) {
                        // 进行故障转移
                        logger.info("节点["+path+"]被移除，需要进行故障转移");
                        taskRegister.failover(springTask);
                    }
                    /**
                     * 当前任务的一个实例被移除了
                     * 需要删除在zookeeper中为其创建的节点(已经被删了)
                     * 以及停掉任务并从全局变量中删掉
                     */
                    if(path.equals(TaskRegister.getTaskInstancePath(springTask))) {
                        // 删除任务
                        logger.info("节点["+path+"]被移除，需要删除任务");
                        taskRegister.delete(springTask);
                    }
                    break;
                case CHILD_UPDATED:
                    path = pathChildrenEvent.getData().getPath();
                    /**
                     * exec节点有更新，需要对任务实例进行操作
                     * {"task_instance":"cron_task_1_192_168_122_101","status":"3"}
                     */
                    if(path.equals(TaskRegister.getTaskExecPath(springTask))) {
                        final byte[] data = pathChildrenEvent.getData().getData();
                        final JSONObject jsonObject = JSONObject.parseObject(new String(data, Constants.Global.CHARSET_NAME));
                        if(jsonObject.get("task_instance").toString().equals(TaskRegister.getTaskInstanceName(springTask))) {
                            switch (jsonObject.get("status").toString()) {
                                case Constants.Global.PREPARED:
                                    logger.info("任务["+jsonObject.get("task_instance").toString() + "]无法切换到就绪状态");
                                    break;
                                case Constants.Global.RUNNING:
                                    logger.info("任务["+jsonObject.get("task_instance").toString() + "]需要重启");
                                    taskRegister.restartSpringTask(springTask);
                                    break;
                                case Constants.Global.STOPPED:
                                    logger.info("任务["+jsonObject.get("task_instance").toString() + "]需要停止");
                                    taskRegister.stopSpringTask(springTask);
                                    break;
                            }
                        }
                    }
                    break;
            }
        });
        pathChildrenCache.start();
    }
}
