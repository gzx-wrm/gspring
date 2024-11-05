package com.gzx.application;

import com.gzx.application.service.OrderService;
import com.gzx.application.service.UserService;
import com.gzx.spring.annotation.ComponentScan;
import com.gzx.spring.annotation.Configuration;
import com.gzx.spring.context.ApplicationContext;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@Configuration
@ComponentScan(value = {"com.gzx.application.service.impl", "com.gzx.application.processor"})
public class Application {

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ApplicationContext(Application.class);
//        System.out.println("hello spring");
        UserService userService = (UserService) applicationContext.getBean("userService");
        OrderService orderService = (OrderService) applicationContext.getBean("orderService");
        System.out.println(userService);
        System.out.println(orderService);
        System.out.println(userService.getOrderService());
    }
}
