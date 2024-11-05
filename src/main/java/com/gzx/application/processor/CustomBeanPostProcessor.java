package com.gzx.application.processor;

import com.gzx.spring.annotation.Component;
import com.gzx.spring.factory.BeanPostProcessor;

import java.lang.reflect.Proxy;

@Component
public class CustomBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("Before Initialization...");

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 实际上, spring返回的bean是经过代理的对象，使用代理可以在不更改class的同时为对象赋予更多功能
        Object o1 = Proxy.newProxyInstance(bean.getClass().getClassLoader(), bean.getClass().getInterfaces(), (proxy, method, args) -> {
            System.out.println("before method: " + method.getName());
            // 如果不进行额外判断，这里会将对象o的所有方法都进行代理
            Object result = method.invoke(bean, args);
            System.out.println("after method: " + method.getName());

            return result;
        });
        return o1;
    }
}
