package com.example.demo.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Created by wang ming on 2019/2/18.
 */
@Service
public class AsyncService {

    @Async
    public void doNoReturn() {
        try {
            Thread.sleep(3000);
            throw new Exception("haha");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
