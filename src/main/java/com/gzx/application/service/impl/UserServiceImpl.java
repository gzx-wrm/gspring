package com.gzx.application.service.impl;

import com.gzx.application.service.OrderService;
import com.gzx.application.service.UserService;
import com.gzx.spring.annotation.Autowired;
import com.gzx.spring.annotation.Component;
import com.gzx.spring.factory.InitializingBean;

@Component("userService")
public class UserServiceImpl implements UserService, InitializingBean {

    @Autowired(name = "orderService")
    private OrderService orderService;

    public OrderService getOrderService() {
        return orderService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("afterPropertiesSet...");
    }
}
