package com.rockwill.deploy.conf;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextHolder implements ApplicationContextAware {
    
    private static ApplicationContext context;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
    
    public static ApplicationContext getApplicationContext() {
        return context;
    }
    
    public static <T> T getBean(Class<T> clazz) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext is not initialized");
        }
        return context.getBean(clazz);
    }
    
    public static <T> T getBean(String name, Class<T> clazz) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext is not initialized");
        }
        return context.getBean(name, clazz);
    }
    
    public static Object getBean(String name) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext is not initialized");
        }
        return context.getBean(name);
    }
}