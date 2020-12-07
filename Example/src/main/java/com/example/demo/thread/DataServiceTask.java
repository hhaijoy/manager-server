package com.example.demo.thread;


import com.alibaba.fastjson.JSONObject;
import com.example.demo.dao.TaskDao;
import com.example.demo.domain.Scheduler.Task;
import com.example.demo.domain.xml.DataProcessing;
import com.example.demo.domain.xml.DataTemplate;
import com.example.demo.utils.ApplicationContextProvider;
import com.example.demo.utils.MyFileUtils;
import com.example.demo.utils.MyHttpUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
import java.util.List;
import java.util.concurrent.Future;

@Component
public class DataServiceTask {

    @Async
    public Future<String> getDataServiceResult(DataProcessing dataProcessing, int param , Task task) throws IOException, URISyntaxException {
        String baseUrl = "http://111.229.14.128:8898/invokeUrlDataPcs";
        String token = dataProcessing.getToken();
        String service = dataProcessing.getService();
        String data = null;
        DataTemplate input = dataProcessing.getInputData().getInputs().get(0);
        data = input.getDataContent().getValue();

        InputStream inputStream = MyFileUtils.getRemoteFileStream(data);

        String dataType = "other";
        if(inputStream!=null){
            byte[] buffer = new byte[1024];

            inputStream.read(buffer, 0, buffer.length);

            String a = MyFileUtils.getFileStreamSuffix(buffer);

            if(a.substring(0,8).equals("504B0304")){
                dataType = "zip";
            }
        }else{
            dataProcessing.setStatus(-1);
            return null;
        }

//        String url = baseUrl + "token=" + URLEncoder.encode(token) + "&pcsId=" + service + "&contDtId" + data + "&params=Processing";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type","application/x-www-form-urlencoded");

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("token", URLEncoder.encode(token));
        requestBody.add("pcsId",service);
        requestBody.add("url",data);
        HttpEntity<MultiValueMap> requestEntity = new HttpEntity<MultiValueMap>(requestBody, headers);

        String result = restTemplate.postForObject(baseUrl,requestEntity,String.class);
//        params.put("params","Processing");

        dataProcessing.setStatus(0);
        dataProcessing.setRemark("running");
        if(result.equals("offLine")){
            dataProcessing.setRemark("offLine");
        }else if(result.equals("error")||result.equals("parameters err")){
            dataProcessing.setStatus(-1);
        }else {
            dataProcessing.setStatus(1);
            List<DataTemplate> outputs = dataProcessing.getOutputData().getOutputs();
            for(DataTemplate output:outputs){
//                String resultUrl = "" + result;
                output.getDataContent().setValue(result);
            }
        }
        TaskDao taskDao = ApplicationContextProvider.getBean(TaskDao.class);
        taskDao.save(task);

        return new AsyncResult<>(result);
    }
}
