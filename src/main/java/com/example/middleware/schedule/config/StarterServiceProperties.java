package com.example.middleware.schedule.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 与项目配置文件对应的配置类
 * @author yuhe
 */
// 此注解可以将配置文件中对应开头的配置项的内容自动注入到配置类中
@ConfigurationProperties("example.middleware.schedule")
public class StarterServiceProperties {
    private String zkAddress;

    public String getZkAddress() {
        return zkAddress;
    }

    public void setZkAddress(String zkAddress) {
        this.zkAddress = zkAddress;
    }
}
