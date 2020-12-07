package com.example.demo.utils;

import java.util.Random;

/**
 * Created by wang ming on 2019/4/23.
 */
public class KeyUtil {

    public static synchronized String genUniqueKey(){
        Random random = new Random();
        Integer num = random.nextInt(900000) + 100000;
        return System.currentTimeMillis() + String.valueOf(num);
    }
}
