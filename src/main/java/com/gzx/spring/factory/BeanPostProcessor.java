package com.gzx.spring.factory;

/**
 * 用户可以通过实现该接口自定义bean初始化后的逻辑，可以用来实现类似AOP的效果
 */
public interface BeanPostProcessor {

    Object postProcessBeforeInitialization(Object bean, String beanName);

    Object postProcessAfterInitialization(Object bean, String beanName);
}
