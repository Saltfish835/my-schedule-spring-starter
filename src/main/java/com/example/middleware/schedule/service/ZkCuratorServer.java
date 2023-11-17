package com.example.middleware.schedule.service;


import com.example.middleware.schedule.common.Constants;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

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
        }
    }


    /**
     * 在exec节点上添加监听器
     * 用于监听节点的变化后对任务进行启停
     * @param applicationContext
     * @param client
     */
    public static void addTreeCacheListenerForExec(final ApplicationContext applicationContext, final CuratorFramework client) throws Exception {
        TreeCache treeCache = new TreeCache(client, Constants.Global.path_root_exec);
        treeCache.start();
        treeCache.getListenable().addListener(((curatorFramework, event) -> {
            if(event.getData() == null) {
                return;
            }
            byte[] eventData = event.getData().getData();
            if(eventData == null || eventData.length < 1) {
                return;
            }
            // 获取到事件携带的信息
            String json = new String(eventData, Constants.Global.CHARSET_NAME);


        }));
    }


}
