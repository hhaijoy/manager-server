package com.example.demo.test;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

/**
 * Created by wang ming on 2019/2/19.
 */
@Component
public class AsyncCallBackTask extends AbstractTask {

    @Async("asyncPoolTaskExecutor")
    public Future<String> doTaskOneCallback() throws  Exception{
        super.doTaskOne();
        return new AsyncResult<>("任务一完成");
    }

    @Async("asyncPoolTaskExecutor")
    public Future<String> doTaskTwoCallback() throws  Exception{
        super.doTaskTwo();
        return new AsyncResult<>("任务二完成");
    }

    @Async("asyncPoolTaskExecutor")
    public Future<String> doTaskThreeCallback() throws  Exception{
        super.doTaskThree();
        return new AsyncResult<>("任务三完成");
    }


}
