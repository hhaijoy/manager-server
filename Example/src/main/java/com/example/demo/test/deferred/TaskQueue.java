package com.example.demo.test.deferred;


import com.example.demo.test.ResponseMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 任务队列
 * Created by wang ming on 2019/2/22.
 */
@Component
public class TaskQueue {

    private static final Logger log = LoggerFactory.getLogger(TaskQueue.class);

    private static final int QUEUE_LENGTH = 10;

    private BlockingQueue<Task> queue = new LinkedBlockingDeque<>(QUEUE_LENGTH);

    private int taskId = 0;

    /**
    * @Description: 加入任务
    * @Param: [deferredResult]
    * @return: void
    * @Author: WangMing
    * @Date: 2019/2/22
    */
    public void put(DeferredResult<ResponseMsg<String>> deferredResult){
        taskId++;
        log.info("任务加入队列，id为：{}",taskId);
        queue.offer(new Task(taskId,deferredResult));
    }

    /** 
    * @Description: 获取任务 
    * @Param: [] 
    * @return: com.example.demo.test.deferred.Task 
    * @Author: WangMing 
    * @Date: 2019/2/22 
    */
    public Task take() throws InterruptedException{
        Task task = queue.poll();
        log.info("获得任务：{}",task);
        return task;
    }
}
