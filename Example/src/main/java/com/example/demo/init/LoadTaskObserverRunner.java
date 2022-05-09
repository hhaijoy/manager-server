package com.example.demo.init;

import com.example.demo.DemoApplication;
import com.example.demo.domain.Scheduler.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @Author: wangming
 * @Date: 2019-11-15 14:07
 */
@Component
@Slf4j
public class LoadTaskObserverRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //开辟一个线程，监听消息队列中的任务
        log.info("初始化任务监听线程");
        new Thread(new TaskScheduler(),"TaskObserver").start();
        log.info("任务监听线程已经启动");
    }
}
