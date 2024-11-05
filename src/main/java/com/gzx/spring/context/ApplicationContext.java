package com.gzx.spring.context;

import com.gzx.spring.annotation.Autowired;
import com.gzx.spring.annotation.Component;
import com.gzx.spring.annotation.ComponentScan;
import com.gzx.spring.bean.BeanDefinition;
import com.gzx.spring.enums.Scope;
import com.gzx.spring.factory.InitializingBean;

import java.beans.Introspector;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * spring 容器类，负责读取配置并且初始化bean
 */
public class ApplicationContext {

    private Class clazz;

    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionNameMap = new ConcurrentHashMap<>();
    
    public ConcurrentHashMap<String, Object> singletonBeanMap = new ConcurrentHashMap<>();

    public ApplicationContext(Class clazz) throws IOException, ClassNotFoundException {
        clazz = clazz;

        ClassLoader classLoader = ApplicationContext.class.getClassLoader();
        // 1. 包扫描
        if (clazz.isAnnotationPresent(ComponentScan.class)) {
            // 1.1 拿到ComponentScan注解标识的包名
            ComponentScan componentScan = (ComponentScan) clazz.getAnnotation(ComponentScan.class);
            String packageName = componentScan.value();

            // 1.2 通过报名获取包下的所有类文件 .class，这个过程借助类加载器实现
            String packagePath = packageName.replace('.', '/');
            URL scanPackage = classLoader.getResource(packagePath);
            File f = new File(scanPackage.getFile());
            if (!f.exists() || !f.isDirectory()) {
                throw new FileNotFoundException(f.getAbsolutePath() + "is not found or is not a directory");
            }
            File[] files = f.listFiles();
            // 1.2.1 扫描所有文件，找出.class文件
            for (File classFile : files) {
                if (!classFile.getName().endsWith(".class")) {
                    continue;
                }
                // 1.3 判断这些类是否是Component，如果是则初始化beanDefinition
                // todo: 考虑子包的情况
                LinkedList<String> classNameList = new LinkedList<>();
                classNameList.add(packageName);


                String className = classFile.getName().substring(0, classFile.getName().length() - 6);
                classNameList.add(className);

//                System.out.println(className);
                Class<?> c = classLoader.loadClass(classNameList.stream().collect(Collectors.joining(".")));
                if (!c.isAnnotationPresent(Component.class)) {
                    continue;
                }
                Component component = c.getAnnotation(Component.class);
                String componentName = component.value();
                if (componentName.isEmpty()) {
                    componentName = Introspector.decapitalize(c.getSimpleName());
//                    System.out.println("componentName: " + componentName);
                }
//                System.out.println(componentName);
                // 1.3.1 初始化beanDefinition，保存到容器中
                // todo: 这里默认是根据bean name获取，可以考虑根据name获取不到时根据type获取
                // todo: 名称重复的情况？
                BeanDefinition beanDefinition = new BeanDefinition(c, component.scope());
                beanDefinitionNameMap.put(componentName, beanDefinition);
            }
            
            // 1.4 根据是否是单例模式初始化bean，如果是多例模式的话则在get的时候再初始化
            beanDefinitionNameMap.forEach((k, v) -> {
                Scope scope = v.getScope();
                if (scope == Scope.PROTOTYPE) {
                    return;
                }


                Object bean = null;
                try {
                    bean = getBean(k);

                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                // 1.4.1 可以将构造出来的bean再重新以class分类装入容器中
//                loadBeanByClass(bean);
                singletonBeanMap.put(k, bean);
            });
        }
    }

    public Object getBean(String beanName) throws Exception {
        Object bean = singletonBeanMap.get(beanName);
        if (bean != null) {
            return bean;
        }

        BeanDefinition beanDefinition = beanDefinitionNameMap.get(beanName);
        if (beanDefinition == null) {
            throw new RuntimeException("Bean: " + beanName + " not found");
        }

        Object o = createBean(beanDefinition);
        return o;
    }

    private Object createBean(BeanDefinition beanDefinition) throws Exception {
        // 根据beanDefinition构造一个对象
        Class<?> beanClass = beanDefinition.getBeanClass();
        // todo: 根据不同的构造方法以及Autowired注解在构造bean时进行依赖注入
        Constructor<?>[] constructors = beanClass.getConstructors();
        // 这里直接使用默认的无参构造方法进行构造
        Constructor<?> defaultConstructor = null;
        for (Constructor<?> constructor : constructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 0) {
                defaultConstructor = constructor;
                break;
            }
        }
        if (defaultConstructor == null) {
            throw new RuntimeException("No default constructor found");
        }

        Object o = defaultConstructor.newInstance();

        // todo: 根据AutoWired注解注入属性
        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(Autowired.class)) {
                continue;
            }
            field.setAccessible(true);
            // 尝试从单例对象容器中获取，如果获取不到则进入递推步骤，从这里就有了“循环依赖”的问题
            String beanName = getAutowiredBeanName(field);
            field.set(o, getBean(beanName));
        }

        // 可选: 实例化bean之后的逻辑
        if (o instanceof InitializingBean) {
            ((InitializingBean) o).afterPropertiesSet();
        }

        // 实际上, spring返回的bean是经过代理的对象，使用代理可以在不更改class的同时为对象赋予更多功能
        Object o1 = Proxy.newProxyInstance(o.getClass().getClassLoader(), o.getClass().getInterfaces(), (proxy, method, args) -> {
            System.out.println("before method: " + method.getName());
            // 如果不进行额外判断，这里会将对象o的所有方法都进行代理
            Object result = method.invoke(o, args);
            System.out.println("after method: " + method.getName());

            return result;
        });
        return o1;
    }

    private String getAutowiredBeanName(Field field) {
        Autowired autowired = field.getAnnotation(Autowired.class);
        String name = autowired.name();
        if (!name.isEmpty()) {
            return name;
        }

        // 获取不到则直接获取这个类对应的java变量名
        Class<?> clazz = field.getType();
        return Introspector.decapitalize(clazz.getSimpleName());
    }
}
