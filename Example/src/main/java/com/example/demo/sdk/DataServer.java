package com.example.demo.sdk;

/**
 * Created by wang ming on 2019/4/12.
 */
public interface DataServer {

    public Data upload(String dataPath, String tag)throws Exception;
}
