package com.example.demo.init;

import com.example.demo.DemoApplication;
import com.example.demo.domain.Scheduler.Task;
import com.example.demo.thread.TaskHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 主要任务循环，时刻监听任务队列里是否存在任务
 * @Author: wangming
 * @Date: 2019-11-15 15:31
 */
public class TaskScheduler implements Runnable {
    private static ExecutorService threadPool = Executors.newCachedThreadPool();
    LinkedBlockingDeque<Task> linkedBlockingDeque = DemoApplication.linkedBlockingDeque;
    @Override
    public void run() {
        System.out.println("准备监听任务队列任务");
        while (true){
//            try{
//                Task task = linkedBlockingDeque.take();
//                System.out.println("成功获取到一个计算任务");
//                TaskHandler taskHandler = new TaskHandler(task);
//                //开辟一个线程专门用来处理这个Task
//                threadPool.execute(taskHandler);
//            }catch (InterruptedException e){
//                e.printStackTrace();
//            }
        }
    }
}
