package com.example.demo.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Created by wang ming on 2019/2/21.
 */
@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    public ResponseMsg<String> getResult(){
        log.info("任务开始执行，持续等待中...");
        try{
            Thread.sleep(1000L);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        log.info("任务处理完成");
        return new ResponseMsg<String>(0,"操作成功","success");
    }


}
