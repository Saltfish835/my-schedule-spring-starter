package com.example.middleware.schedule.domain.task.base;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 需要定时执行的逻辑是被注解修饰的方法
 * @author yuhe
 */
public abstract class MethodTask extends BaseTask{
    // 方法所在的类对象
    @JSONField(serialize = false)
    protected Object bean;

    // 类对象的名称
    protected String beanName;

    // 方法名称
    protected String methodName;

    public Object getBean() {
        return bean;
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
