package com.example.middleware.schedule.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 封装配置文件中的内容
 */
// 使StarterServiceProperties类生效
@EnableConfigurationProperties(StarterServiceProperties.class)
@Configuration("example-middleware-schedule-starterAutoConfig")
public class StarterAutoConfig {

    // 将配置文件的内容注入进来
    @Autowired
    private StarterServiceProperties properties;

    public StarterServiceProperties getProperties() {
        return properties;
    }

    public void setProperties(StarterServiceProperties properties) {
        this.properties = properties;
    }
}
