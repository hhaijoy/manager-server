package com.example.demo.thread;


import com.alibaba.fastjson.JSONObject;
import com.example.demo.dao.TaskDao;
import com.example.demo.domain.Scheduler.Task;
import com.example.demo.domain.xml.Action;
import com.example.demo.domain.xml.ActionParam;
import com.example.demo.domain.xml.DataProcessing;
import com.example.demo.domain.xml.DataTemplate;
import com.example.demo.utils.ApplicationContextProvider;
import com.example.demo.utils.MyFileUtils;
import com.example.demo.utils.MyHttpUtils;
import com.example.demo.utils.XmlParseUtils;
import net.bytebuddy.dynamic.loading.MultipleParentClassLoader;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class DataServiceTask {

    @Value("111.229.14.128:8898")
    private String dataServerIp = "111.229.14.128:8898";

    @Async
    public Future<String> getDataServiceResult(DataProcessing dataProcessing, int param , Task task) throws IOException, URISyntaxException, DocumentException {
        String baseUrl = "http://"+dataServerIp;

        String token = dataProcessing.getToken();
        String service = dataProcessing.getService();

        String data = null;
        String paramStr = "";
        List<DataTemplate> inputs = dataProcessing.getInputData().getInputs();
        List<ActionParam> params = new ArrayList<>();
        if(dataProcessing.getInputData().getParams()!=null){
            params = dataProcessing.getInputData().getParams();
        }

        JSONObject urls = new JSONObject();
        for(int i=0;i<inputs.size();i++){
            data = inputs.get(i).getDataContent().getValue();
            urls.put(inputs.get(i).getEvent(),data);
        }

        for(int i=0;i<params.size();i++){
            String value = params.get(i).getValue();
            paramStr += value ;
            if(i<params.size()-1){
                paramStr += "," ;
            }
        }

        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(6000);// 设置超时
        requestFactory.setReadTimeout(6000);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("token", URLEncoder.encode(token));
        requestBody.add("pcsId",service);
        requestBody.add("urls",urls);
        if(!paramStr.equals("")){
            requestBody.add("params",paramStr);
        }
        HttpEntity<MultiValueMap> requestEntity = new HttpEntity<MultiValueMap>(requestBody, headers);
        String url = baseUrl + "/invokeUrlsDataPcsWithKey";
        String result = null;

        Map<String,String> header = new HashMap<>();
        header.put("Content-Type", "application/json");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", URLEncoder.encode(token));
        jsonObject.put("pcsId", service);
        jsonObject.put("urls", urls);

        try{
            result = MyHttpUtils.POSTWithJSON(url,"UTF-8",header,jsonObject);
        }catch (Exception e){
            dataProcessing.setStatus(0);
            dataProcessing.setRemark("running");
            dataProcessing.setRunTimes(dataProcessing.getRunTimes()+1);
            if(dataProcessing.getRunTimes()==5){//超时、不在线超过5次，视为失败
                dataProcessing.setStatus(-1);
            }

            return new AsyncResult<>(null);
        }
//        params.put("params","Processing");
        JSONObject j_result =  JSONObject.parseObject(result);
        int code = j_result.getInteger("code");

        dataProcessing.setStatus(0);
        dataProcessing.setRemark("running");
        dataProcessing.setRunTimes(dataProcessing.getRunTimes()+1);

        if(code==-1){
            String message = j_result.getString("message");
            if(message.equals("node offline")){
                dataProcessing.setRemark("offLine");
                if(dataProcessing.getRunTimes()==5){
                    dataProcessing.setStatus(-1);
                }
            }else if(message.equals("err")){
                dataProcessing.setStatus(-1);
            }
        }else {
            dataProcessing.setStatus(1);
            dataProcessing.setRemark("completed");
            JSONObject dataResult = j_result.getJSONObject("urls");
            List<DataTemplate> outputs = dataProcessing.getOutputData().getOutputs();

            dataResult.forEach((key,value)->{
                for(DataTemplate output:outputs){
                    if(key.equals(output.getEvent())){
                        output.getDataContent().setValue((String) value);

                    }
                }
            });

        }
        TaskDao taskDao = ApplicationContextProvider.getBean(TaskDao.class);
        taskDao.save(task);


        return new AsyncResult<>(result);
    }
}
