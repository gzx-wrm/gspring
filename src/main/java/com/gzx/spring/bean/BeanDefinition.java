package com.gzx.spring.bean;

import com.gzx.spring.enums.Scope;

/**
 * 该类定义了bean的一些元属性
 */
public class BeanDefinition {
    private Class<?> beanClass;

    private Scope scope;

    public BeanDefinition(Class<?> beanClass, Scope scope) {
        this.beanClass = beanClass;
        this.scope = scope;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public Scope getScope() {
        return scope;
    }
}
