package com.gzx.spring.factory;

/**
 * 实现该接口可以在bean实例化后完成一些用户自定义的逻辑
 */
public interface InitializingBean {
    void afterPropertiesSet() throws Exception;
}
