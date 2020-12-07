package com.example.demo.thread;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.utils.MyHttpUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractTask {

    public JSONObject getRecord(JSONObject params) throws Exception{
        String taskIp = params.getString("taskIp");
        String taskId = params.getString("taskId");
        int port = params.getInteger("port");

        String taskQueryUrl = "http://" + taskIp + ":" + port + "/task/" + taskId;
        String taskResult = MyHttpUtils.GET(taskQueryUrl, "UTF-8",null);
        JSONObject taskResultResponse = JSONObject.parseObject(taskResult);

        return taskResultResponse;

    }
}

