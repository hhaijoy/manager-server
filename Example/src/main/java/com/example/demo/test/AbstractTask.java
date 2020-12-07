package com.example.demo.test;

import java.util.Random;

/**
 * Created by wang ming on 2019/2/19.
 */
public abstract class AbstractTask {

    private static Random random = new Random();

    public void doTaskOne() throws Exception{
        System.out.println("开始做任务1");
        long start = System.currentTimeMillis();
        Thread.sleep(random.nextInt(3000));
        long end = System.currentTimeMillis();
        System.out.println("完成任务1，耗时：" + (end - start) + "毫秒");
    }

    public void doTaskTwo() throws Exception{
        System.out.println("开始做任务2");
        long start = System.currentTimeMillis();
        Thread.sleep(random.nextInt(3000));
        long end = System.currentTimeMillis();
        System.out.println("完成任务2，耗时：" + (end - start) + "毫秒");
    }

    public void doTaskThree() throws Exception{
        System.out.println("开始做任务3");
        long start = System.currentTimeMillis();
        Thread.sleep(random.nextInt(3000));
        long end = System.currentTimeMillis();
        System.out.println("完成任务3，耗时：" + (end - start) + "毫秒");
    }


}
