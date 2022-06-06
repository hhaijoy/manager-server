package com.example.demo.thread;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.domain.support.TemplateInfo;
import com.example.demo.domain.xml.DataTemplate;
import com.example.demo.domain.xml.Model;
import com.example.demo.domain.xml.ModelAction;
import com.example.demo.domain.xml.ShareData;
import com.example.demo.dto.computableModel.ExDataDTO;
import com.example.demo.dto.computableModel.OutputDataDTO;
import com.example.demo.utils.MyHttpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * 针对于对应的Model节点的操作
 * @Author: wangming
 * @Date: 2019-11-15 22:34
 */
public class AdvanceHandler {

    public static String MANAGERSERVER = "127.0.0.1:8084/GeoModeling";
    private List<ModelAction> modelList;
    private String userName;
    /*
    * sharedMap主要用来存储该任务所用到的数据
    */
    private volatile ConcurrentHashMap<String, ShareData> sharedMap;

    public AdvanceHandler(List<ModelAction> modelList, ConcurrentHashMap<String, ShareData> sharedMap, String userName){
        this.modelList = modelList;
        this.sharedMap = sharedMap;
        this.userName = userName;
    }

    public Model runModel(int index) throws Exception {
        while (true){
            /*
            * TODO check error,
            * 目前是当发现有模型运行出错了的话，就直接返回该节点
            * 而实际过程中因为提交的任务有可能之间并无依赖关系，所以并不能因为一个模型的运行出错导致整个任务的终结
             */
//            if(checkError()){
//                return this.modelList.get(index);
//            }
//            //构建用来判断数据是否准备完好的变量
//            boolean status = false;
//            //分段加锁，在判断完数据准备完毕之后，开启模型调用流程
//            Model runModel = this.modelList.get(index);
//            synchronized (this.sharedMap){
//                List<DataTemplate> inputData = this.modelList.get(index).getInputData().getInputs();
//                //检查该模型需要运行的数据是否已经准备好
//                if(checkData(inputData)){
//                    status = true;
//                }else{
//                    this.sharedMap.wait();
//                }
//            }
//            //只有数据准备完毕之后才会进入到这里
//            if (status){
//                //提交模型运行给ManagerServer(这里测试就调用本地)
//                String pid = this.modelList.get(index).getPid();
//                List<ExDataDTO> inputs = TemplateToExData(this.modelList.get(index).getInputData().getInputs());
//                // TaskServiceDTO和TaskSubmitDTO两个中的outputs属性不一样，这里需要的是后者
//                List<OutputDataDTO> outputs = new ArrayList<>();
//                for (int i = 0; i < this.modelList.get(index).getOutputData().getOutputs().size(); i++) {
//                    DataTemplate out = this.modelList.get(index).getOutputData().getOutputs().get(i);
//                    OutputDataDTO output = new OutputDataDTO();
//                    output.setEvent(out.getEvent());
//                    output.setStatename(out.getState());
//                    output.setTemplate(new TemplateInfo(out.getDataContent().getType(),out.getDataContent().getValue()));
//                    outputs.add(output);
//                }
//                //拼凑请求URL
//                String url = "http://" + MANAGERSERVER + "/computableModel/submitTask";
//                JSONObject params = new JSONObject();
//                params.put("pid",pid);
//                params.put("userName",userName);
//                params.put("inputs",inputs);
//                params.put("outputs",outputs);
//
//                try{
//                    String resJson = MyHttpUtils.POSTWithJSON(url, "UTF-8",null,params);
//                    System.out.println(resJson);
//                    JSONObject jResponse = JSONObject.parseObject(resJson);
//                    System.out.println("error place");
//                    if(jResponse.getInteger("code") == -1){
//                        //说明找不到可用的地理模型服务
//                        synchronized (this.sharedMap){
//                            this.modelList.get(index).setStatus(-1);
//                            //唤醒其他线程
//                            this.sharedMap.notifyAll();
//                        }
//                        //退出该线程循环
//                        break;
//                    }else{
//                        //得到任务的Id后，进行轮询遍历，以得到模型的运行结果
//                        String taskId = jResponse.getJSONObject("data").getString("tid");
//                        String taskIp = jResponse.getJSONObject("data").getString("ip");
//                        int port = jResponse.getJSONObject("data").getInteger("port");
//                        //构建模型运行状态变量
//                        int taskStatus;
//                        while(true){
//                            //直接向Task Server 发起http请求，获取模型运行结果
//                            String taskQueryUrl = "http://" + taskIp + ":" + port + "/task/" + taskId;
//                            String taskResult = MyHttpUtils.GET(taskQueryUrl, "UTF-8",null);
//                            JSONObject taskResultResponse = JSONObject.parseObject(taskResult);
//                            if(taskResultResponse.getString("result").equals("suc")){
//                                JSONObject jData = taskResultResponse.getJSONObject("data");
//                                if(jData == null){
//                                    throw new IOException("task Server Error");
//                                }else{
//                                    String t_status = jData.getString("t_status");
//                                    taskStatus = convertStatus(t_status);
//                                    //对状态进行判断，运行成功和失败
//                                    if (taskStatus == 0){
//                                        //表明模型还处于等待运行或者已经运行状态过程中，那么让该线程睡眠一会儿
//                                        Thread.sleep(2000);
//                                        continue;
//                                    }else{
//                                        //失败或成功的状态跳出while循环，在break之前更新一下model里面output信息
//                                        JSONArray jOutputs = jData.getJSONArray("t_outputs");
//                                        //更新状态
//                                        updateModelOutputByTask(jOutputs,index);
//                                        break;
//                                    }
//                                }
//                            }else{
//                                // 返回result为err，说明taskServer可能出问题了，因为查询不到记录,往外面抛出错误
//                                throw new IOException("task Server Error");
//                            }
//                        }
//                        //模型运行成功或者失败的情况
//                        synchronized (this.sharedMap){
//                            this.modelList.get(index).setStatus(taskStatus);
//                            //更新数据的concurrentHashMap
//                            updateOutputToGlobalMap(index);
//                            //唤醒其他等待的线程
//                            this.sharedMap.notifyAll();
//                            break;
//                        }
//                    }
//                }catch (IOException e){
//                    e.printStackTrace();
//                    synchronized (this.sharedMap){
//                        this.modelList.get(index).setStatus(-1);
//                        this.sharedMap.notifyAll();
//                    }
//                    break;
//                } catch (URISyntaxException e) {
//                    e.printStackTrace();
//                    synchronized (this.sharedMap){
//                        this.modelList.get(index).setStatus(-1);
//                        this.sharedMap.notifyAll();
//                    }
//                    break;
//                }
//            }
        }
    }

    /**
     * 初步实现策略：
     * 只要集成流程有一个服务运行出错了，那么就会导致整个集成流程不再运行，保留现场痕迹
     * 后面再进行优化，因为可能任务之间并不存在依赖性
     * @param
     * @return boolean
     * @author wangming
     * @date 2019/11/19 21:29
     */
    public boolean checkError(){
        for (ModelAction model : this.modelList){
            if(model.getStatus() == -1){
                return true;
            }
        }
        return false;
    }

    //更新数据到concurrentHashMap中
    private void updateOutputToGlobalMap(int index){
//        List<DataTemplate> outputs = this.modelList.get(index).getOutputData().getOutputs();
//        for (DataTemplate output : outputs) {
//            if (!output.getDataContent().getValue().equals("")){
//                ShareData shareData = new ShareData();
//                shareData.setValue(output.getDataContent().getValue());
//                shareData.setType(output.getDataContent().getType());
//                this.sharedMap.put(output.getDataId(),shareData);
//            }
//        }
    }

    // 将从TaskServer得到的模型运行记录(输出结果信息)更新到原文档中
    private void updateModelOutputByTask(JSONArray jOutputs, int index){
        List<DataTemplate> outputs = this.modelList.get(index).getOutputData().getOutputs();
        for (int i = 0; i < jOutputs.size(); i++){
            JSONObject temp = jOutputs.getJSONObject(i);
            String state = temp.getString("StateName");
            String event = temp.getString("Event");
            //根据state和event去outputs里面查找对应的并且更新相对应的值
            for (DataTemplate dataTemplate : outputs) {
                if(dataTemplate.getState().equals(state) && dataTemplate.getEvent().equals(event)){
                    dataTemplate.getDataContent().setValue(temp.getString("Url"));
                    dataTemplate.setPrepared(true);
                }
            }
        }
    }

    private int convertStatus(String taskStatus){
        int status;
        if(taskStatus.equals("Inited") || taskStatus.equals("Started")){
            //任务处于开始状态
            status = 0;
        }else if(taskStatus.equals("Finished")){
            status = 1;
        }else {
            status = -1;
        }
        return status;
    }

    private List<ExDataDTO> TemplateToExData(List<DataTemplate> dataTemplates){
        List<ExDataDTO> exDataDTOList = new ArrayList<>();
        for (int i = 0; i < dataTemplates.size(); i++) {
            ExDataDTO exDataDTO = new ExDataDTO();
            DataTemplate dataTemplate = dataTemplates.get(i);
            exDataDTO.setStatename(dataTemplate.getState());
            exDataDTO.setEvent(dataTemplate.getEvent());
            exDataDTO.setUrl(dataTemplate.getDataContent().getValue());
            //默认以event name作为tag的标签
            exDataDTO.setTag(dataTemplate.getEvent());
            exDataDTOList.add(exDataDTO);
        }
        return exDataDTOList;
    }

    /**
     * 检查数据有没有准备好，只要有一个未准备好就直接返回false
     * @param inputsList
     * @return boolean
     * @author wangming
     * @date 2019/11/19 16:26
     */
    private boolean checkData(List<DataTemplate> inputsList){
        for (DataTemplate template : inputsList) {
            if(template.getDataContent().getType().equals("link")){
                String value = template.getDataContent().getValue();
                if(!sharedMap.containsKey(value)){
                    template.setPrepared(false);
                    return false;
                }else{
//                    String urlStr= sharedMap.get(value).getValue();
//                    if(urlStr.indexOf("[")!=-1){
//                        urlStr = urlStr.substring(1,urlStr.length()-1);
//                        String[] urls = urlStr.split(",");
////                        for(int i=0;i<urls.length;i++){
////
////                        }
//                        template.setMutiFile(urls);
//                    }
//                    else{
//                        template.getDataContent().setValue(sharedMap.get(value).getValue());
                        template.getDataContent().setType(sharedMap.get(value).getType());
                        template.setPrepared(true);
//                    }

                }
            }else{
                if (!sharedMap.containsKey(template.getDataId())){
                    template.setPrepared(false);
                    return false;
                }else{
                    template.setPrepared(true);
                }
            }
        }
        return true;
    }

}
