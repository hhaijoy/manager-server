package com.example.demo.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicaitonContext) throws BeansException {
        this.applicationContext = applicaitonContext;
    }

    /**
     * 获取applicationContext
     *
     * @return
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 通过class获取Bean.
     *
     * @param classType
     * @param <T>
     * @return
     */
    public static <T> T getBean(Class<T> classType) {
        return getApplicationContext().getBean(classType);
    }

    /**
     * 通过name,以及Clazz返回指定的Bean
     *
     * @param name
     * @param classType
     * @param <T>
     * @return
     */
    public static <T> T getBean(String name, Class<T> classType) {
        return getApplicationContext().getBean(name, classType);
    }
}
