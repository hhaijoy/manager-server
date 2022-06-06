package com.example.demo.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.dao.TaskDao;
import com.example.demo.domain.Scheduler.Task;
import com.example.demo.domain.support.TemplateInfo;
import com.example.demo.domain.xml.*;
import com.example.demo.dto.computableModel.ExDataDTO;
import com.example.demo.dto.computableModel.OutputDataDTO;
import com.example.demo.pojo.IterationGroup;
import com.example.demo.sdk.Data;
import com.example.demo.thread.DataServiceTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dom4j.DocumentException;
import org.omg.CORBA.StringHolder;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;

import com.example.demo.dao.TaskDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.spel.ast.Operator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import sun.security.x509.AttributeNameEnumeration;

import javax.validation.constraints.Null;

import static com.example.demo.utils.MyFileUtils.getValueFromFile;

/**
 * created by Zihuan
 * 多模型集成运行
 * 2020.8.21
 */
public class TaskLoop {
    @Value("111.229.14.128:8898")
    private String dataServerIp = "111.229.14.128:8898";

    public static String MANAGERSERVER = "127.0.0.1:8084/GeoModeling";
    public static String DATACONVERTSERVER = "127.0.0.1:8084/GeoModeling";
    private ConcurrentHashMap<String, ShareData> tempOutput;
    private ConcurrentHashMap<String, ShareData> sharedOutput;//存储结构为k:output id,v:输出对应的data
    private ConcurrentHashMap<String, Boolean> conditions;
    private ConcurrentHashMap<String, Integer> iterationCount;//记录一个task中迭代模型运行次数
    private List<List<ModelAction>> iterationGroupList;//记录一个task中的需要迭代的模型，关联的放一个group里

    private ConcurrentHashMap<String,ConcurrentHashMap<String, ShareData>> iteSharedOutput;//k:循环的id，v:循环对应的共享数据池

    private String userName;

    private TaskDao taskDao = ApplicationContextProvider.getBean(TaskDao.class);

    public interface Operation {
        boolean calculate(float a, float b);
    }

//    Operation over = (a, b) -> a > b;
//    Operation oq = (a, b) -> a >= b;
//    Operation blow = (a, b) -> a < b;
//    Operation bq = (a, b) -> a <= b;
//    Operation eq = (a, b) -> a == b;
//    Operation neq = (a, b) -> a != b;

    private Map<String,Operation> opMap = new HashMap<String, Operation>() {
        {
            put(">", (a, b) -> a > b);
            put(">=", (a, b) -> a >= b);
            put("<", (a, b) -> a < b);
            put("<=", (a, b) -> a <= b);
            put("=", (a, b) -> a == b);
            put("!=", (a, b) -> a != b);
        }
    };


    public TaskLoop(String userName){

        this.userName = userName;
        this.sharedOutput = new ConcurrentHashMap<>();
        this.tempOutput = new ConcurrentHashMap<>();
        this.iterationCount = new ConcurrentHashMap<>();
        this.iterationGroupList = new ArrayList<>();
        this.conditions = new ConcurrentHashMap<>();
        this.iteSharedOutput = new ConcurrentHashMap<>();
    }

    public void initTaskRun(Task task){
        task.setStatus(0);

        initIterations(task);
        iterationInit(task);
        initCondition(task);
        initDataFlow(task);
        taskDao.save(task);
    }

    /**
     * 初始化task中的循环，对每个循环的元素进行设置
     * @param task
     * @return 1设置成功，0task中无循环，-1循环检验错误
     */
    public int initIterations(Task task){
        //todo 在前面判断循环是否出错
        List<Iteration> iterationList = task.getIterations();
        if(iterationList==null||iterationList.size()==0){
            return 0;//无循环
        }else {
            for(Iteration iteration:iterationList){
                List<String> contents= new ArrayList<>();

                String iterationId = iteration.getId();
                if(iteration.getCondition() == null||iteration.getElements() == null
                        || iteration.getElements().size() == 0){
                    return -1;
                }

                String conditionId = iteration.getCondition().getIndex();

                ControlCondition controlCondition = getTargetCondition(task.getControlConditions(),conditionId);
                controlCondition.setIteration(iterationId);
                String trueAction = controlCondition.getTrueAction();
                String falseAction = controlCondition.getFalseAction();

                if(controlCondition==null){
                    return -1;
                }

                contents.add(conditionId);//统计循环中的元素

                for(IterationElement element:iteration.getElements()){
                    String elementId = element.getIndex();

                    if(elementId.equals(trueAction)){
                        iteration.setControlCase(true);
                    }else if(elementId.equals(falseAction)){
                        iteration.setControlCase(false);
                    }

                    String type = element.getType();
                    if(type.equals("condition")){
                        ControlCondition controlCondition1 = getTargetCondition(task.getControlConditions(),elementId);
                        if(controlCondition1 == null){
                            return -1;
                        }else{
                            controlCondition1.setIteration(iterationId);
                        }
                    }
                    if(type.equals("modelAction")){
                        List<ModelAction> modelActions = getTargetModelById(task.getModelActions(),elementId);
                        if(modelActions!=null&&modelActions.size()==0){
                            return -1;
                        }else{
                            for(ModelAction modelAction:modelActions){
                                modelAction.setIteration(iterationId);
                            }
                        }
                    }
                    if(type.equals("dataProcessing")){
                        List<DataProcessing> dataProcessings = getTargetProcessingById(task.getDataProcessings(),elementId);
                        if(dataProcessings!=null&&dataProcessings.size()==0){
                            return -1;
                        }else{
                            for(DataProcessing dataProcessing:dataProcessings){
                                dataProcessing.setIteration(iterationId);
                            }
                        }
                    }
                    contents.add(elementId);//统计循环中的元素

                }
                iteration.setContents(contents);//统计循环中的元素

            }

            taskDao.save(task);


            return 1;
        }
    }

    //更新循环
    public void updateIteration(Iteration iteration,Task task){
        List<IterationElement> elementList = iteration.getElements();

        int iteRound = iteration.getRound();

        int count = 0;
        int flag = 1;

        for(IterationElement element:elementList){
            String elementId = element.getIndex();

            int eleRound = 0;


            String type = element.getType();
            if(type.equals("condition")){
                ControlCondition controlCondition1 = getTargetCondition(task.getControlConditions(),elementId);
                eleRound = controlCondition1.getRound();
                if(eleRound>iteRound){
                    count++;
                }

                if(eleRound>0){
                    flag = 1;
                }
            }
            if(type.equals("modelAction")){
                List<ModelAction> modelActions = getTargetModelById(task.getModelActions(),elementId);
                for(ModelAction modelAction:modelActions){
                    eleRound = modelAction.getRound();
                    if(eleRound>iteRound){
                        count++;
                    }
                    if(eleRound>0){
                        flag = 1;
                    }
                }
            }
            if(type.equals("dataProcessing")){
                List<DataProcessing> dataProcessings = getTargetProcessingById(task.getDataProcessings(),elementId);
                for(DataProcessing dataProcessing:dataProcessings){
                    eleRound = dataProcessing.getRound();
                    if(eleRound>iteRound){
                        count++;
                    }
                    if(eleRound>0){
                        flag = 1;
                    }
                }
            }

        }

        if(count==iteration.getElements().size()){
            iteration.setRound(iteRound+1);
        }

        ControlCondition controlCondition = getTargetCondition(task.getControlConditions(),iteration.getCondition().getIndex());
        if(iteration.getType().equals("while")){//当型循环
            if(controlCondition.getJudgeResult()==iteration.getControlCase()){//条件满足，当型开始执行
                iteration.setStatus(1);

            }else{//跳出循环
                exportIteOutput(iteration,task);
                iteration.setStatus(2);
            }
        }

        if(iteration.getType().equals("till")){//直到型循环
            if(flag==1&&iteration.getStatus()==0){//直到型循环只要有一个元素开始运行就认为开始
                iteration.setStatus(1);
            }
            if(controlCondition.getJudgeResult()!=iteration.getControlCase()){//条件不满足，直到型跳出
                exportIteOutput(iteration,task);
                iteration.setStatus(2);

            }
            //直到型开始与条件判断无关，元素第一轮直接执行。
        }

        taskDao.save(task);
    }

    public void addToIteDataPool(Action completeAction,Task task){
        String iterationId = completeAction.getIteration();
        Iteration iteration = getTargetIteration(task.getIterations(),iterationId);

        List<IterationElement> bodyElements = iteration.getElements();

        List<DataTemplate> outputData = completeAction.getOutputData().getOutputs();
        String actionId = completeAction.getId();

        ConcurrentHashMap<String, ShareData> iteShareData = iteSharedOutput.get(iterationId);
        if(iteShareData==null){
            iteShareData = new ConcurrentHashMap<>();
        }
        for(int i=0;i<outputData.size();i++){
            String dataId = outputData.get(i).getDataId();
            String url = outputData.get(i).getDataContent().getValue();
            if(iteShareData != null){
                if(iteShareData.containsKey(outputData.get(i).getDataId())){
                    List<String> datas = iteShareData.get(dataId).getValues();
                    if (datas.contains(url)){
                        break;
                    }else {
                        datas.add(url);
                    }
                }else{
                    List<String> urls = new ArrayList<>();
                    if(url.indexOf("[")!=-1){
                        String[] urlStrs = url.substring(1,url.length()).split(",");

//                        urls = Arrays.asList(url.substring(1,url.length()).split(","));
                        for (String s : urlStrs) {
                            String str= s.replace("\"", "");
                            urls.add(str);
                        }
                    }else{
                        urls.add(url);
                    }
                    ShareData shareData = new ShareData(actionId, dataId,urls,"suc",outputData.get(i).getDataContent().getType());
                    iteShareData.put(outputData.get(i).getDataId(),shareData);
                }
            }

//            else{
//                ShareData shareData = new ShareData(actionId, dataId,outputData.get(i).getDataContent().getValue(),outputData.get(i).getDataContent().getType());
//                sharedOutput.put(outputData.get(i).getDataId(),shareData);
//            }

        }

        iteSharedOutput.put(iterationId,iteShareData);

    }

    //根据datalink把需要的数据拷贝到公共数据池中
    public void exportIteOutput(Iteration iteration,Task task){
        ConcurrentHashMap<String, ShareData> iteShareData = iteSharedOutput.get(iteration.getId());

        if(iteShareData == null) return;

        Map<String,List<DataLink>> reverseDataLink = task.getReDataLinks();

        List<String> contents = iteration.getContents();

        for(Map.Entry<String, ShareData> ele: iteShareData.entrySet()) {
            //共享数据的索引
            String outputId = ele.getKey();
            //共享数据的内容
            ShareData outputData = ele.getValue();

            List<DataLink> dataLinks = reverseDataLink.get("outputId");//找到从这个输出出发的所有datalink

            for(DataLink dataLink:dataLinks){
                String toAction = dataLink.getTarget();//这个link的目标输入

                List<String> iteOutputs = outputData.getValues();

                if(!contents.contains(toAction)){//如果这个目标不在循环中，说明这个数据要放到公共池
                    if(sharedOutput.containsKey(outputId)){//公共数据池存在该输出，去重添加
                        List<String> datas = sharedOutput.get(outputId).getValues();

                        datas.removeAll(iteOutputs);
                        datas.addAll(iteOutputs);

                    }else{
                        //公共数据池不存在该输出，则全部拷贝
                        sharedOutput.put(outputId,outputData);
                    }
                }
            }




        }

    }

    //判断循环许可
    public int checkIteration(FlowElement flowElement, Task task) {
        if(flowElement.getIteration()==null)
            return 1;//非循环元素，直接许可运行

        String id = flowElement.getIteration();

        Iteration iteration = getTargetIteration(task.getIterations(), id);

        if (iteration.getStatus() == 0 && iteration.getType().equals("while")) {
            return 0;//循环未开始,当型不可执行
        } else if (iteration.getStatus() !=2 ) {//while型状态为1，或til型状态不为2，til启动无需判断，直接启动
            int eleRound = flowElement.getRound();
            int iteRound = iteration.getRound();

            if (eleRound > iteRound) {
                return 0;//本action在本轮循环执行已结束，等待
            } else {
                return 1;//可以开始下一轮
            }
        } else {
            return 2;//循环结束或者跳过，元素不需要在执行
        }


    }

    public String isDataFromIteration(String inputId,Iteration iteration,Task task){

        Map<String,List<DataLink>> dataLinkMap = task.getDataLinks();

        List<DataLink> dataLinks = dataLinkMap.get(inputId);

        List<String> contents = iteration.getContents();

        for(DataLink dataLink:dataLinks){
            String fromAction = dataLink.getSource();//这个link的出发地

            if(contents.contains(fromAction)){
                return dataLink.getFrom();
            }
        }

        return null;


    }

    public String getDataFrom(String inputId,Task task) {
        Map<String,List<DataLink>> dataLinkMap = task.getDataLinks();

        List<DataLink> dataLinks = dataLinkMap.get(inputId);

        //非循环的元素，数据流只存在一个
        return dataLinks.get(0).getFrom();
    }

    public Map<String,String> getDataFrom(String inputId,String iterationId,Task task){

        Iteration iteration = getTargetIteration(task.getIterations(),iterationId);

        Map<String,List<DataLink>> dataLinkMap = task.getDataLinks();

        List<DataLink> dataLinks = dataLinkMap.get(inputId);

        if(dataLinks==null){
            return null;
        }

        List<String> contents = iteration.getContents();

        Map<String,String> result = new HashMap<>();
        for(DataLink dataLink:dataLinks){
            String fromAction = dataLink.getSource();//这个link的出发地元素

            if(contents.contains(fromAction)){
                result.put("ite",dataLink.getFrom());
            }else{
                result.put("out",dataLink.getFrom());
            }
        }

//        if()

        return result;


    }

    //第一次运行，如果存在两个输出连接到一个输入，必然一个外部一个内部
    //检查循环内部元素的数据准备情况
    public Boolean checkItePrepared(Action action,Task task){
        String iterationId = action.getIteration();
        Iteration iteration = getTargetIteration(task.getIterations(),iterationId);

        int round = action.getRound();

        List<DataTemplate> inputsList = action.getInputData().getInputs();
        for (DataTemplate template : inputsList) {
            String dataId = template.getDataId();

            if(round > 0){//循环执行一轮产生中间数据，mixed和multiLink中间数据连接要从循环数据池取数据
                if(template.getDataContent().getType().equals("link")&&template.getPrepared()==false){//单link的数据产生后不会变

//                Map<String,String> dataFromMap = getDataFrom(dataId,iteration,task);
                    String value = template.getDataContent().getLink();
                    if(sharedOutput == null||!sharedOutput.containsKey(value)){
                        template.setPrepared(false);
                        return false;
                    }else{
                        if(sharedOutput.get(value).getValues().get(0).equals("error")){//说明上游数据错误
                            action.setStatus(2);
                            return false;
                        }
                        try{
                            linkDataFlow(template, action, task);
                        }catch (IOException e){

                        }
                    }
                }else if(template.getDataContent().getType().equals("mixed")||template.getDataContent().getType().equals("multiLink")||template.getDataContent().getType().equals("iteLink")){
                    String value = template.getDataContent().getIteLink();
                    Map<String,ShareData> dataPool = iteSharedOutput.get(iterationId);
                    if(dataPool == null||!dataPool.containsKey(value)){
                        template.setPrepared(false);
                        return false;
                    }else{
                        if(dataPool.get(value).getValues().get(0).equals("error")){//说明上游数据错误
                            action.setStatus(2);
                            return false;
                        }
                        try{
                            linkDataFlow(template, action, task);
                        }catch (IOException e){

                        }
                    }

                }else{
                    if (template.getDataContent().getValue()==null||template.getDataContent().getValue().equals("")||template.getDataContent().getValue().equals("error")){
                        template.setPrepared(false);
                        action.setStatus(2);//说明缺少数据，且无法配置
                        return false;
                    }else{
                        template.setPrepared(true);
                    }
                }
            }else if(round == 0){//循环未开始，所以带link和multiLink的都从公共数据池中取数据
                if(template.getDataContent().getType().equals("link")||template.getDataContent().getType().equals("multiLink")) {

                    String value = template.getDataContent().getOutLink();

                    if(sharedOutput == null||!sharedOutput.containsKey(value)){
                        template.setPrepared(false);
                        return false;
                    }else{
                        if(sharedOutput.get(value).getValues().get(0).equals("error")){//说明上游数据错误
                            action.setStatus(2);
                            return false;
                        }
                        try{
                            linkDataFlow(template, action, task);
                        }catch (IOException e){

                        }
                    }
                }else if(template.getDataContent().getType().equals("iteLink")){
                    String value = template.getDataContent().getIteLink();
                    Map<String,ShareData> dataPool = iteSharedOutput.get(iterationId);
                    if(dataPool == null||!dataPool.containsKey(value)){
                        template.setPrepared(false);
                        return false;
                    }else{
                        if(dataPool.get(value).getValues().get(0).equals("error")){//说明上游数据错误
                            action.setStatus(2);
                            return false;
                        }
                        try{
                            linkDataFlow(template, action, task);
                        }catch (IOException e){

                        }
                    }
                }
                else{//mixed第一次运行的数据由用户配置
                    if (template.getDataContent().getValue()==null||template.getDataContent().getValue().equals("")||template.getDataContent().getValue().equals("error")){
                        template.setPrepared(false);
                        action.setStatus(2);//说明缺少数据，且无法配置
                        return false;
                    }else{
                        template.setPrepared(true);
                    }
                }

            }

        }
        return true;


    }

    //连接循环中的数据流，本循环中的模型从本循环的数据池中获取数据
    public void linkIteDataFlow(DataTemplate template,Action action,Task task) throws IOException {
        String md5 = "";
        if(action instanceof ModelAction){
            action = (ModelAction)action;
            md5 = ((ModelAction) action).getMd5();
            //String type
        }else if(action instanceof DataProcessing){

        }

        String iterationId = action.getIteration();

        ConcurrentHashMap<String, ShareData> iteShareData = iteSharedOutput.get(iterationId);

        String value = template.getDataContent().getLink();//input所需要的output的id
        List<String> urls = iteShareData.get(value).getValues();
//        if(!checkConversion(task.getDataLink(),template.getDataId()).equals("0")){//转换数据
//            urlStr = convertData(urlStr,checkConversion(task.getDataLink(),template.getDataId()));
//        }

        String type = iteShareData.get(value).getType();
        if (urls.size()>0) {//如果这是一个多输出

            List<ModelAction> targetModelActionList = getTargetModel(task.getModelActions(),md5);
            if(targetModelActionList.size()<urls.size()){//多输出匹配的下游model任务数量不够要补上
                int num = urls.size() - targetModelActionList.size();
                for (int j = 0; j < num; j++){
                    ObjectMapper objectMapper = new ObjectMapper();
                    ModelAction newModelAction = objectMapper.readValue(objectMapper.writeValueAsString(action), ModelAction.class);

                    List<ModelAction> modelActions = task.getModelActions();
                    modelActions.add(newModelAction);
                    task.setModelActions(modelActions);
                }
            }

            targetModelActionList = getTargetModel(task.getModelActions(),md5);//重新获取要加的model列


            for (int i = 0; i < urls.size(); i++) {
                addOutputToInput(targetModelActionList.get(i),value,urls.get(i),type);//type是一致的

            }
        }else{
            String url = urls.get(0);
            template.getDataContent().setValue(url);
            template.getDataContent().setType(iteShareData.get(value).getType());
            template.setPrepared(true);
        }

        taskDao.save(task);
    }

    public Boolean inIteration(String id,Iteration iteration){

        List<String> iteContents = iteration.getContents();
        if(iteContents.contains(id))
            return true;
        else
            return false;
    }

    /**
     * 根据task中的link表设置每个input的link属性
     * @param task
     */
    public void initDataFlow(Task task){
        Map<String,List<DataLink>> dataFlowMap = task.getDataLinks();
        if(dataFlowMap!=null){
            List<ModelAction> modelActions = task.getModelActions();
            List<ModelAction> i_ModelActions = task.getI_modelActions();
            List<ModelAction> modelActionList = new ArrayList<>();

            modelActionList.addAll(modelActions);
            if(i_ModelActions!=null){
                modelActionList. addAll(i_ModelActions);//两个list都要加进去
            }

            for(ModelAction modelAction:modelActionList){

                String iterationId = modelAction.getIteration();
                Iteration iteration = new Iteration();
                if(iterationId!=null){
                    iteration = getTargetIteration(task.getIterations(),iterationId);
                }


                for(DataTemplate dataTemplate:modelAction.getInputData().getInputs()){
                    String dataId = dataTemplate.getDataId();
                    DataContent dataContent = dataTemplate.getDataContent();

                    if(dataTemplate.getDataContent().getType().equals("url")){
                        continue;
                    }

                    Map<String,String> dataFromMap = new HashMap<>();

                    if(modelAction.getIteration()==null){
                        if(dataTemplate.getDataContent().getType().equals("link")){
                            String from = getDataFrom(dataId,task);
                            dataTemplate.getDataContent().setLink(from);
                        }
                    }else {
                        dataFromMap = getDataFrom(dataId, modelAction.getIteration(), task);

                        if (dataTemplate.getDataContent().getType().equals("link") && dataFromMap.size() == 1) {

                            List<DataLink> dataLinks = dataFlowMap.get(dataTemplate.getDataId());
//                        if(dataLinks.size()>1){
//                            dataTemplate.getDataContent().setType("mutiLink");
//
//
//
//                        }else{
                            DataLink dataLink = dataLinks.get(0);
                            String sourceAction = dataLink.getSource();
                            if (iterationId != null && iteration.getContents().contains(sourceAction)) {
                                dataTemplate.getDataContent().setIteLink(dataLinks.get(0).getFrom());
                                dataTemplate.getDataContent().setType("iteLink");
                            } else {
                                dataTemplate.getDataContent().setOutLink(dataLinks.get(0).getFrom());
                            }

                            dataTemplate.getDataContent().setLink(dataLinks.get(0).getFrom());
//                        }

                        } else if (dataFromMap.size() > 1) {

                            dataContent.setIteLink(dataFromMap.get("ite"));

                            dataContent.setType("multiLink");

                            dataContent.setOutLink(dataFromMap.get("out"));

                        } else if (dataTemplate.getDataContent().getType().equals("mixed")) {
                            List<DataLink> dataLinks = dataFlowMap.get(dataTemplate.getDataId());

                            dataTemplate.getDataContent().setIteLink(dataLinks.get(0).getFrom());
                        }
                    }
                }
            }
        }

        taskDao.save(task);

    }

    /**
     *初始化条件判断
     * @param task
     */
    public void initCondition(Task task){
        List<ControlCondition> controlConditions = task.getControlConditions();

        if(controlConditions!=null){
            for(ControlCondition controlCondition:controlConditions){
                List<ModelAction> tModelActions = getTargetModelById(task.getModelActions(),controlCondition.getTrueAction());//获得条件判断为真时连接的modelAction
                ModelAction tModelAction = tModelActions.get(0);
                tModelAction.setCondition(controlCondition.getId());

                List<ModelAction> fModelActions = getTargetModelById(task.getModelActions(),controlCondition.getFalseAction());
                ModelAction fModelAction = fModelActions.get(0);
                fModelAction.setCondition(controlCondition.getId());

            }
        }

        taskDao.save(task);

    }


    /**
     * 初始化迭代计数器
     * @param task
     */
    public void iterationInit(Task task){

        if(task.getI_modelActions()!=null){
            List<ModelAction> i_modelActions = MyGeneralHandleUtils.deepCopy(task.getI_modelActions()) ;
            Iterator<ModelAction> modelActionIterator = i_modelActions.iterator();
            while(modelActionIterator.hasNext()){
                ModelAction modelAction = modelActionIterator.next();
                iterationCount.put(modelAction.getMd5(),0);//计数器置为零

                //生成迭代模型组列表
                List<ModelAction> relatedModels = new ArrayList<>();
                relatedModels.add(modelAction);
                for(int i=0;i<relatedModels.size();i++){//贪心把一个group内的model找净
                    ModelAction srcModelAction = relatedModels.get(i);
                    for(DataTemplate inputData:srcModelAction.getInputData().getInputs()){
                        if(inputData.getLink()!=null){
                            ModelAction relatedModelAction = findDataLinkedModel(inputData.getLink(),i_modelActions);
                            if(relatedModelAction!=null&&relatedModelAction!=modelAction){//迭代可能link自己本身，则不计入
                                relatedModels.add(relatedModelAction);
                                modelActionIterator.remove();
                            }

                        }
                    }
                }
                iterationGroupList.add(relatedModels);

            }
        }

    }

    public ConcurrentHashMap<String, Integer> getIterationInfo(Task task){
        return this.iterationCount;
    }

    public void iteFirstRun(){

    }

    /**
     * 把已经准备好的模型任务推入taskserver
     * @param waitingModels 待处理的模型
     * @param task
     * @return
     */
    public int query(List<ModelAction> waitingModels,List<DataProcessing> waitingProcessings , Task task) throws IOException, URISyntaxException, DocumentException {
        int result = 0;

        for(int i=0;i<waitingModels.size();i++){
            ModelAction modelAction = waitingModels.get(i);
            if(checkCondition(task,modelAction)==1){
                if(modelAction.getIteration()!=null){
                    if(checkIteration(modelAction,task)==1 && checkItePrepared(modelAction,task)){//分别检查循环控制和数据准备情况
                        runModel(modelAction);
                    }
                }else if(modelAction.getIterationNum()==1 && checkData(modelAction,task)){//如果是个单运行模型
                    runModel(modelAction);
                }else if(modelAction.getIterationNum()>1&&checkSingleItePrepared(modelAction,task)){//如果是个迭代模型
                    runModel(modelAction);
                }
            }

        }

        if(waitingProcessings!=null){
            DataServiceTask dataServiceTask = new DataServiceTask();
            List<Future> futures = new ArrayList<>();
            for(int i=0;i<waitingProcessings.size();i++){
                DataProcessing dataProcessing = waitingProcessings.get(i);
//                if(dataProcessing.getType().equals("modelService") ){//如果是个modelService
//                    if(checkData(dataProcessing,task)){
//                        runModel(dataProcessing);
//                    }
//                }else if(dataProcessing.getType().equals("dataService")){
//                    if(checkDataServicePrepared(dataProcessing,task)){
//                        runProcessing(dataProcessing);
//                    }
//                }
                if(checkCondition(task,dataProcessing)==1){
                    if(dataProcessing.getIteration()!=null){
                        if(checkIteration(dataProcessing,task)==1 && checkItePrepared(dataProcessing,task)){//分别检查循环控制和数据准备情况
                            runProcessing(dataProcessing);
                        }
                    }else if(checkDataServicePrepared(dataProcessing,task)){
                        runProcessing(dataProcessing);
                    }
//                }
                }
            }
        }


        if(task.getControlConditions()!=null){
            for(ControlCondition controlCondition:task.getControlConditions()){//判断条件
                if(controlCondition.getIteration()==null&&controlCondition.getStatus()!=1){//非循环条件判断只要判断一次
                    judgeCondition(controlCondition,task);
                }
                if(controlCondition.getIteration()!=null){
                    String iterationId = controlCondition.getIteration();
                    Iteration iteration = getTargetIteration(task.getIterations(),iterationId);

                    String iterationConditionId = iteration.getCondition().getIndex();

                    if(iterationConditionId.equals(iterationId)
                            &&iteration.getType().equals("while")
                            &&controlCondition.getRound()==0){//如果是一个当型循环的决定条件，且未判断过则尝试判断
                        judgeCondition(controlCondition,task);

                    }else if(checkIteration(controlCondition,task)==1){
                        judgeCondition(controlCondition,task);
                    }

                }

            }
        }

        return result;
    }


    /**
     * 运行模型
     * @param action
     * @throws IOException
     */
    private void runModel(Action action) throws IOException {
        String url = "http://" + MANAGERSERVER + "/computableModel/submitTask";
        JSONObject params = new JSONObject();

        String pid = null;
        if(action instanceof ModelAction){
            ModelAction modelAction = (ModelAction) action;
            pid = modelAction.getMd5();
        }else{
            DataProcessing modelAction = (DataProcessing) action;
            pid = modelAction.getService();
        }

        List<ExDataDTO> inputs = TemplateToExData(action.getInputData().getInputs());
        // TaskServiceDTO和TaskSubmitDTO两个中的outputs属性不一样，这里需要的是后者
        List<OutputDataDTO> outputs = new ArrayList<>();
        for (int i = 0; i < action.getOutputData().getOutputs().size(); i++) {
            DataTemplate out = action.getOutputData().getOutputs().get(i);
            OutputDataDTO output = new OutputDataDTO();
            output.setEvent(out.getEvent());
            output.setStatename(out.getState());
            output.setTemplate(new TemplateInfo("",""));
            outputs.add(output);
        }

        params.put("pid",pid);
        params.put("userName",userName);
        params.put("inputs",inputs);
        params.put("outputs",outputs);

        String resJson = MyHttpUtils.POSTWithJSON(url, "UTF-8",null,params);
        System.out.println(resJson);
        JSONObject jResponse = JSONObject.parseObject(resJson);
        System.out.println("error place");
        if(jResponse.getInteger("code") == -1){
            //说明找不到可用的地理模型服务
            action.setStatus(-1);
        }else{
            //把返回信息填入该模型，进行标识
            JSONObject modelInfo = (JSONObject) jResponse.get("data");
            action.setTaskId(modelInfo.get("tid").toString());
            action.setPort((Integer) modelInfo.get("port"));
            action.setTaskIp(modelInfo.get("ip").toString());
        }
    }

    private void runProcessing(DataProcessing dataProcessing) throws IOException {
        String baseUrl = "http://"+dataServerIp;

        String token = dataProcessing.getToken();
        String service = dataProcessing.getService();

        String data = null;
        List<DataTemplate> inputs = dataProcessing.getInputData().getInputs();
        List<DataTemplate> outputs = dataProcessing.getOutputData().getOutputs();
        List<ActionParam> params = new ArrayList<>();
        if(dataProcessing.getInputData().getParams()!=null){
            params = dataProcessing.getInputData().getParams();
        }

        JSONObject urls = new JSONObject();
        for(int i=0;i<inputs.size();i++){
            data = inputs.get(i).getDataContent().getValue();
            urls.put(inputs.get(i).getEvent(),data);
        }
        JSONObject outputsConfig = new JSONObject();
        for(int i=0;i<outputs.size();i++){
            if(outputs.get(i).getDataContent()!=null
                    &&outputs.get(i).getDataContent().getType()!=null
                    &&outputs.get(i).getDataContent().getType().equals("insituData"))
            {
                outputsConfig.put(outputs.get(i).getEvent(),false);
            }else if(outputs.get(i).getDataContent().getType()==null){
                //如果没有填写，默认设置为url
                outputs.get(i).getDataContent().setType("url");
            }
        }

        JSONObject paramsArr = new JSONObject();
        for(int i=0;i<params.size();i++){
            String value = params.get(i).getValue();
            paramsArr.put(params.get(i).getEvent(),value);
        }

        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(6000);// 设置超时
        requestFactory.setReadTimeout(6000);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("token", URLEncoder.encode(token));
        requestBody.add("serviceId",service);
        requestBody.add("inputArr",urls);
        if(!paramsArr.isEmpty()){
            requestBody.add("paramsArr",paramsArr);
        }
        requestBody.add("outputArr",outputsConfig);
        HttpEntity<MultiValueMap> requestEntity = new HttpEntity<MultiValueMap>(requestBody, headers);
        String url = baseUrl + "/invokeLocally";
        String result = null;

        Map<String,String> header = new HashMap<>();
        header.put("Content-Type", "application/json");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", URLEncoder.encode(token));
        jsonObject.put("serviceId", service);
        jsonObject.put("inputArr",urls);
        if(!paramsArr.isEmpty()){
            jsonObject.put("paramsArr",paramsArr);
        }
        jsonObject.put("outputArr",outputsConfig);
        try{
            result = MyHttpUtils.POSTWithJSON(url,"UTF-8",header,jsonObject);
        }catch (Exception e){
            dataProcessing.setStatus(0);
            dataProcessing.setRemark("running");
            dataProcessing.setRunTimes(dataProcessing.getRunTimes()+1);
            if(dataProcessing.getRunTimes()==5){//超时、不在线超过5次，视为失败
                dataProcessing.setStatus(2);
            }
            dataProcessing.setStatus(-1);

            return;
        }
//        params.put("params","Processing");
        JSONObject j_result =  JSONObject.parseObject(result);
        int code = j_result.getInteger("code");

        dataProcessing.setStatus(0);
        dataProcessing.setRemark("running");
        dataProcessing.setRunTimes(dataProcessing.getRunTimes()+1);

        if(code==-1){
            dataProcessing.setRemark("offLine");
            if(dataProcessing.getRunTimes()==5){
                dataProcessing.setStatus(-1);
            }
        }else if(code==0){
            JSONObject dataRespose = j_result.getJSONObject("data");

            String status = dataRespose.getString("status");
            String taskId = dataRespose.getString("recordId");
            dataProcessing.setTaskId(taskId);

            if(status.equals("success")){
                dataProcessing.setRemark("completed");
                dataProcessing.setStatus(1);
                JSONObject dataResult = new JSONObject();
                String outputArrString = dataRespose.getString("outputArrString");
                String downloadUrlString = dataRespose.getString("downloadUrlString");
                String outputIdString = dataRespose.getString("outputIdString");
                JSONObject j_outputArr = JSONObject.parseObject(outputArrString);
                JSONObject j_downloadUrl = JSONObject.parseObject(downloadUrlString);
                JSONObject j_outputId = JSONObject.parseObject(outputIdString);


                List<DataTemplate> runOutputs = dataProcessing.getOutputData().getOutputs();

                j_outputArr.forEach((key,value)->{
                    for(DataTemplate output:outputs){
                        if(key.equals(output.getEvent())){
                            if((Boolean) value){
                                output.getDataContent().setValue(j_downloadUrl.getString(key));
                            }else{
                                output.getDataContent().setValue(j_outputId.getString(key));
                            }
//                            output.getDataContent().setValue((String) value);

                        }
                    }
                });

            }



        }
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

    //检查集成任务中的所有任务状态
    public Map<String,Object> checkActions(Task task) throws IOException, URISyntaxException {
        Map<String,Object> result = new HashMap<>();

        List<DataProcessing> dataProcessingList = task.getDataProcessings();
        result.put("processing",checkProcessings(task));


        List<ModelAction> modelActionList = task.getModelActions();
        result.put("model",checkModels(task));




        return result;
    }

    /**
     * 检查数据转换，与model同理
     * @param task
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public Map<String,List<DataProcessing>> checkProcessings(Task task) throws IOException, URISyntaxException {
        Map<String,List<DataProcessing>> result = new HashMap<>();
        List<DataProcessing> dataProcessingList = task.getDataProcessings();
        List<DataProcessing> waitingProcessing = new ArrayList<>();
        List<DataProcessing> completedProcessing = new ArrayList<>();
        List<DataProcessing> runningProcessing = new ArrayList<>();
        List<DataProcessing> failedProcessing = new ArrayList<>();
        List<DataProcessing> ignoreList = new ArrayList<>();

        if(task.getDataProcessings()!=null){
            DataProcessing dataProcessing = new DataProcessing();
            for(int i=0;i<dataProcessingList.size();i++){
                dataProcessing = dataProcessingList.get(i);
                if(dataProcessing.getType().equals("modelService")){
                    if(dataProcessing.getStatus()==1){//已经运行好的模型任务
                        completedProcessing.add(dataProcessing);
                    }else if(dataProcessing.getStatus()==0){
                        if(dataProcessing.getTaskId()!=null){//有taskid属性，模型任务已经推入taskserver，则去检查状态
                            JSONObject taskResultResponse = checkAction(dataProcessing);

                            if(taskResultResponse.getString("result").equals("suc")){
                                JSONObject jData = taskResultResponse.getJSONObject("data");
                                if(jData == null){
                                    throw new IOException("task Server Error");
                                }else{
                                    String t_status = jData.getString("t_status");
                                    int ProcessingStatus = convertStatus(t_status);
                                    //对状态进行判断，运行成功和失败
                                    if (ProcessingStatus == 1) {

                                        dataProcessing.setStatus(1);
                                        JSONArray jOutputs = jData.getJSONArray("t_outputs");
                                        updateModelOutputByTask(jOutputs, dataProcessing);
                                        addToSharedData(dataProcessing, task);
                                        completedProcessing.add(dataProcessing);


                                    } else if(ProcessingStatus == -1||ProcessingStatus == 2){
                                        dataProcessing.setStatus(2);
                                        failedProcessing.add(dataProcessing);
                                    }else if(ProcessingStatus == 0){
                                        runningProcessing.add(dataProcessing);
                                    }
                                }
                            }else{
                                // 返回result为err，说明taskServer可能出问题了，因为查询不到记录,往外面抛出错误
                                throw new IOException("task Server Error");
                            }
                        }else{
                            waitingProcessing.add(dataProcessing);
                        }
                    }
                }else if(dataProcessing.getType().equals("dataService")){
                    if(checkCondition(task,dataProcessing)==-1){//如果是个条件判断为false的任务，则不会去运行
                        ignoreList.add(dataProcessing);
                    }else if(dataProcessing.getStatus()==1){
                        addToSharedData(dataProcessing,task);
                        completedProcessing.add(dataProcessing);
                    }else if(dataProcessing.getStatus()==0){
                        if(dataProcessing.getTaskId()!=null){//有taskid属性，模型任务已经推入taskserver，则去检查状态
                            JSONObject taskResultResponse = checkAction(dataProcessing);

                            if(taskResultResponse.getInteger("code")==0){
                                JSONObject jData = taskResultResponse.getJSONObject("data");
                                if(jData == null){
                                    throw new IOException("task Server Error");
                                }else{
                                    String t_status = jData.getString("status");
                                    int ProcessingStatus = convertStatus(t_status);
                                    //对状态进行判断，运行成功和失败
                                    if (ProcessingStatus == 1) {
                                        dataProcessing.setStatus(1);
                                        String outputArrString = jData.getString("outputArrString");
                                        String downloadUrlString = jData.getString("downloadUrlString");
                                        String outputIdString = jData.getString("outputIdString");
                                        JSONObject j_outputArr = JSONObject.parseObject(outputArrString);
                                        JSONObject j_downloadUrl = JSONObject.parseObject(downloadUrlString);
                                        JSONObject j_outputId = JSONObject.parseObject(outputIdString);

                                        List<DataTemplate> runOutputs = dataProcessing.getOutputData().getOutputs();

                                        j_outputArr.forEach((key,value)->{
                                            for(DataTemplate output:runOutputs){
                                                if(key.equals(output.getEvent())){
                                                    if((Boolean) value){
                                                        output.getDataContent().setValue(j_downloadUrl.getString(key));
                                                    }else{
                                                        output.getDataContent().setValue(j_outputId.getString(key));
                                                    }
//                            output.getDataContent().setValue((String) value);

                                                }
                                            }
                                        });
                                        addToSharedData(dataProcessing, task);
                                        completedProcessing.add(dataProcessing);

                                    } else if(ProcessingStatus == -1||ProcessingStatus == 2){
                                        dataProcessing.setStatus(2);
                                        failedProcessing.add(dataProcessing);
                                    }else if(ProcessingStatus == 0){
                                        runningProcessing.add(dataProcessing);
                                    }
                                }
                            }else{
                                //返回result为err，说明taskServer可能出问题了，因为查询不到记录,往外面抛出错误
                                dataProcessing.setStatus(2);
                                failedProcessing.add(dataProcessing);
                                throw new IOException("task Server Error");
                            }
                        }else{
                            waitingProcessing.add(dataProcessing);
                        }
                    }else if(dataProcessing.getStatus()==-1){
                        updateFailedAction(dataProcessing);
                        addToSharedData(dataProcessing,task);
                        failedProcessing.add(dataProcessing);
                    }else if(dataProcessing.getStatus()==2){
                        failedProcessing.add(dataProcessing);
                    }
                }

            }

            taskDao.save(task);
        }


        result.put("waiting",waitingProcessing);
        result.put("running",runningProcessing);
        result.put("completed",completedProcessing);
        result.put("failed",failedProcessing);

        return result;
    }


    /**
     * 检查task中的单模型状态，分类
     * @param task
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public Map<String,List<ModelAction>> checkModels(Task task) throws IOException, URISyntaxException {
        Map<String,List<ModelAction>> result = new HashMap<>();
        List<ModelAction> waitingModel = new ArrayList<>();
        List<ModelAction> completedModel = new ArrayList<>();
        List<ModelAction> runningModel = new ArrayList<>();
        List<ModelAction> failedModel = new ArrayList<>();
        List<ModelAction> ignoredModel = new ArrayList<>();

        List<ModelAction> models = task.getModelActions();
        List<ModelAction> iModels = task.getI_modelActions();
        List<ModelAction> modelActionList = new ArrayList<>();

        if(models!=null){
            modelActionList.addAll(models);
        }
        if(iModels!=null){
            modelActionList.addAll(iModels);//两个list都要加进去
        }

        ModelAction modelAction = new ModelAction();
        for(int i=0;modelActionList!=null&&modelActionList.size()>0&&i<modelActionList.size();i++){
            modelAction = modelActionList.get(i);
            if(checkCondition(task,modelAction)==-1){//如果是个条件判断为false的模型任务，则不会去运行
                ignoredModel.add(modelAction);
            }else if(modelAction.getIteration()!=null&&checkIteration(modelAction,task)==2){
                ignoredModel.add(modelAction);
            } else if(modelAction.getStatus()==1){//已经运行好的模型任务
                completedModel.add(modelAction);
            }else if(modelAction.getStatus()==0){
                if(modelAction.getTaskId()!=null){//有taskid属性，模型任务已经推入taskserver，则去检查状态

                    JSONObject taskResultResponse = checkAction(modelAction);

                    if(taskResultResponse.getString("result").equals("suc")){
                        JSONObject jData = taskResultResponse.getJSONObject("data");
                        if(jData == null){
                            throw new IOException("task Server Error");
                        }else{
                            String t_status = jData.getString("t_status");
                            int modelStatus = convertStatus(t_status);
                            //对状态进行判断，运行成功和失败
                            if (modelStatus == 1){
                                if(modelAction.getIterationNum()>1){//如果是个迭代模型，在迭代完成之前一次运行完状态回滚状态准备下一次运行
                                    Integer newCount = iterationCount.get(modelAction.getMd5())+1;//当前迭代次数更新
                                    if(newCount < modelAction.getIterationNum()){//迭代尚未结束
                                        iterationCount.put(modelAction.getMd5(),newCount);
                                        addToTempData(modelAction);
                                        waitingModel.add(modelAction);
                                        modelAction.setStatus(0);
                                        modelAction.setTaskId(null);
                                    }else{
                                        iterationCount.put(modelAction.getMd5(),modelAction.getIterationNum());
                                        modelAction.setStatus(1);
                                        JSONArray jOutputs = jData.getJSONArray("t_outputs");
                                        updateModelOutputByTask(jOutputs,modelAction);
                                        addToSharedData(modelAction,task);
                                        completedModel.add(modelAction);
                                    }

                                    task.setIterationCount(iterationCount);

                                } else{
                                    if(modelAction.getIteration()!=null){
//                                        String iterationId = modelAction.getIteration();
//                                        Iteration iteration = getTargetIteration(task.getIterations(),iterationId);
                                        int itePermission = checkIteration(modelAction,task);
                                        if(itePermission==2){
                                            completedModel.add(modelAction);
                                        }else if(itePermission==0){
                                            waitingModel.add(modelAction);
                                        }else{

                                        }

                                    }else{
                                        modelAction.setStatus(1);
                                        JSONArray jOutputs = jData.getJSONArray("t_outputs");
                                        updateModelOutputByTask(jOutputs,modelAction);
                                        addToSharedData(modelAction,task);
                                        completedModel.add(modelAction);
                                    }

                                }


                            }else if(modelStatus == -1||modelStatus == 2){
                                modelAction.setStatus(2);

                                //进行运行失败处理
                                updateFailedAction(modelAction);
                                if(modelAction.getIterationNum()>1&&iterationCount.get(modelAction.getMd5())< modelAction.getIterationNum())//迭代模型且
                                {
                                    addToTempData(modelAction);//迭代和非迭代模型共享池不一样
                                }else{
                                    addToSharedData(modelAction,task);
                                }
                                failedModel.add(modelAction);
                            }else if(modelStatus == 0){
                                runningModel.add(modelAction);
                            }
                        }
                    }else{
                        // 返回result为err，说明taskServer可能出问题了，因为查询不到记录,往外面抛出错误
                        updateFailedAction(modelAction);
                        throw new IOException("task Server Error");
                    }
                }else{
                    waitingModel.add(modelAction);
                }
            }else if(modelAction.getStatus()==2){
                failedModel.add(modelAction);
            }
        }

        taskDao.save(task);

        result.put("waiting",waitingModel);
        result.put("running",runningModel);
        result.put("completed",completedModel);
        result.put("failed",failedModel);

        return result;
    }


    //请求容器检查任务状态
    public JSONObject checkAction(Action action) throws IOException, URISyntaxException {
        String taskId = action.getTaskId();
        if(action instanceof ModelAction){
            String taskIp = action.getTaskIp();
            int port = action.getPort();
            String taskQueryUrl = "http://" + taskIp + ":" + port + "/task/" + taskId;

            String taskResult = MyHttpUtils.GET(taskQueryUrl, "UTF-8",null);
            return JSONObject.parseObject(taskResult);
        }else{
            String baseUrl = "http://111.229.14.128:8898/record";
            DataProcessing dataProcessing = (DataProcessing) action;
            String token = dataProcessing.getToken();
            String recordId = dataProcessing.getTaskId();
            String url = baseUrl + "?recordId=" + recordId + "&token=" + URLEncoder.encode(token);
            try {
                String taskResult = MyHttpUtils.GET(url, "UTF-8",null);
                if(JSONObject.parseObject(taskResult).getInteger("code")==-1){
                    System.out.println(JSONObject.parseObject(taskResult));
                }
                return JSONObject.parseObject(taskResult);
            }catch (Exception e){
                JSONObject errObject = new JSONObject();
                errObject.put("code",-1);
                return errObject;
            }

        }

    }

    public void judgeCondition(ControlCondition controlCondition,Task task){
        String valueId = "";
        ConcurrentHashMap<String, ShareData> dataPool = new ConcurrentHashMap<>();

        if(controlCondition.getIteration()==null){
            valueId = getDataFrom(controlCondition.getId(),task);
            dataPool = sharedOutput;
        }else{
            String iterationId = controlCondition.getIteration();

            Iteration iteration = getTargetIteration(task.getIterations(),iterationId);

            //获取该条件判断的输入，可能是一个也可能是两个
            Map<String,String> valueIdMap = getDataFrom(controlCondition.getId(),iterationId,task);
            if(valueIdMap.size()==1){
                String key = "" ;
                for(String key1:valueIdMap.keySet()) {//只要取第一个值
                    valueId = valueIdMap.get(key1);
                    key = key1;
                    break;
                }
                if(key.equals("ite")){
                    dataPool = iteSharedOutput.get(iterationId);
                }else {
                    dataPool = sharedOutput;
                }

            }else{//如果存在两个连接
                int round = controlCondition.getRound();

                if(round == 0){//第一轮使用外部数据
                    valueId = valueIdMap.get("out");
                    dataPool = sharedOutput;
                }else{
                    valueId = valueIdMap.get("ite");
                    dataPool = iteSharedOutput.get(iterationId);
                }
            }


        }

        if(dataPool==null){
            return;
        }
        if(dataPool.contains(valueId)){
            List<String> urls = dataPool.get(valueId).getValues();

            if(urls.size()==0)
                return;

            String url = urls.get(0);
            String value = MyFileUtils.getValueFromFile(url);
            if(controlCondition.getFormat().toLowerCase()=="number"){
                float targetV = Float.parseFloat(value) ;
                boolean curResult = true;
                boolean formerResult = true;
                for(ConditionCase conditionCase:controlCondition.getConditionCases()){
                    //先计算当前case的结果
                    Float standard = Float.parseFloat(conditionCase.getStandard());
                    curResult = opMap.get(conditionCase.getOpertator()).calculate(targetV,standard);
                    String relation = "and";

                    if(conditions.size() == 0||!conditions.containsKey(controlCondition.getId())){//第一个case的判断
                        relation = conditionCase.getRelation();//与第二个条件的关系
                        conditions.put(controlCondition.getId(),curResult);
                        formerResult = curResult;
                    }else{
//                        formerResult = conditions.get(controlCondition.getId());
                        if(relation.equals("and")){
                            conditions.put(controlCondition.getId(),formerResult&&curResult);

                            formerResult = formerResult&&curResult;
                        }else{
                            conditions.put(controlCondition.getId(),formerResult||curResult);

                            formerResult = formerResult&&curResult;
                        }
                        relation = conditionCase.getRelation();//更新与下一个条件的关系

                    }

                }
                controlCondition.setJudgeResult(formerResult);

            }else if(controlCondition.getFormat().toLowerCase()=="string"){
                boolean curResult = true;
                boolean formerResult = true;
                for(ConditionCase conditionCase:controlCondition.getConditionCases()){
                    String standard = conditionCase.getStandard();
                    if(value.equals(standard)){
                        curResult = true;
                    }else{
                        curResult = false;
                    }
                    String relation = "and";
                    if(conditions.size() == 0||!conditions.containsKey(controlCondition.getId())){
                        relation = conditionCase.getRelation();//与第二个条件的关系
                        conditions.put(controlCondition.getId(),curResult);
                    }else{
                        formerResult = conditions.get(controlCondition.getId());
                        if(conditionCase.getRelation().equals("and")){
                            conditions.put(controlCondition.getId(),formerResult&&curResult);
                            formerResult = formerResult&&curResult;
                        }else{
                            conditions.put(controlCondition.getId(),formerResult||curResult);
                            formerResult = formerResult||curResult;
                        }
                        relation = conditionCase.getRelation();//更新与下一个条件的关系
                    }
                }

                controlCondition.setJudgeResult(formerResult);
            }

            controlCondition.setStatus(1);//标识是否已经完整判断过
            taskDao.save(task);
        }

    }

    /**
     * 寻找包含这个data的模型
     * @param dataId
     * @param modelActionList
     * @return
     */
    public ModelAction findDataLinkedModel(String dataId, List<ModelAction> modelActionList){
        for(ModelAction modelAction:modelActionList){
            for(DataTemplate inputData:modelAction.getInputData().getInputs()){
                if(inputData.getDataId().equals(dataId)){
                    return modelAction;
                }
            }
            for(DataTemplate outputData:modelAction.getOutputData().getOutputs()){
                if(outputData.getDataId().equals(dataId)){
                    return modelAction;
                }
            }
        }
        return null;
    }

//    /**
//     * 检查task中的迭代模型状态
//     * @param task
//     * @return
//     * @throws IOException
//     * @throws URISyntaxException
//     */
//    public Map<String,List<Model>> checkIteModels(Task task) throws IOException, URISyntaxException {
//        Map<String,List<Model>> result = new HashMap<>();
//        List<Model> waitingModel = new ArrayList<>();
//        List<Model> completedModel = new ArrayList<>();
//        List<Model> runningModel = new ArrayList<>();
//        List<Model> failedModel = new ArrayList<>();
//
//        List<Model> modelList = task.getI_models();
//
//        Model model = new Model();
//        for(int i=0;i<modelList.size();i++){
//            model = modelList.get(i);
//            if(model.getStatus()==1){//已经运行好的模型任务
//                completedModel.add(model);
//            }else if(model.getStatus()==0){
//                if(model.getTaskId()!=null){//有taskid属性，模型任务已经推入taskserver，则去检查状态
//                    String taskId = model.getTaskId();
//                    String taskIp = model.getTaskIp();
//                    int port = model.getPort();
//                    String taskQueryUrl = "http://" + taskIp + ":" + port + "/task/" + taskId;
//
//                    String taskResult = MyHttpUtils.GET(taskQueryUrl, "UTF-8",null);
//                    JSONObject taskResultResponse = JSONObject.parseObject(taskResult);
//
//                    if(taskResultResponse.getString("result").equals("suc")){
//                        JSONObject jData = taskResultResponse.getJSONObject("data");
//                        if(jData == null){
//                            throw new IOException("task Server Error");
//                        }else{
//                            String t_status = jData.getString("t_status");
//                            int modelStatus = convertStatus(t_status);
//                            //对状态进行判断，运行成功和失败
//                            if (modelStatus == 1){
//                                model.setStatus(1);
//                                JSONArray jOutputs = jData.getJSONArray("t_outputs");
//                                updateModelOutputByTask(jOutputs,model);
//
//
//
//                            }else if(modelStatus == -1||modelStatus == 2){
//                                model.setStatus(2);
//                                failedModel.add(model);
//                            }else if(modelStatus == 0){
//                                runningModel.add(model);
//                            }
//                        }
//                    }else{
//                        // 返回result为err，说明taskServer可能出问题了，因为查询不到记录,往外面抛出错误
//                        throw new IOException("task Server Error");
//                    }
//                }else{
//                    waitingModel.add(model);
//                }
//            }
//        }
//
//        taskDao.save(task);
//
//        result.put("waiting",waitingModel);
//        result.put("running",runningModel);
//        result.put("completed",completedModel);
//        result.put("failed",failedModel);
//
//        return result;
//    }

    // 将从TaskServer得到的模型运行记录(输出结果信息)更新到原文档中
    private void updateModelOutputByTask(JSONArray jOutputs,Action modelAction){
        List<DataTemplate> outputs = modelAction.getOutputData().getOutputs();
        for (int i = 0; i < jOutputs.size(); i++){
            JSONObject temp = jOutputs.getJSONObject(i);
            String state = temp.getString("StateName");
            String event = temp.getString("Event");
            //根据state和event去outputs里面查找对应的并且更新相对应的值
            for (DataTemplate dataTemplate : outputs) {
                if(dataTemplate.getState().equals(state) && dataTemplate.getEvent().equals(event)){
                    dataTemplate.getDataContent().setValue(temp.getString("Url"));
                    dataTemplate.getDataContent().setType("Url");
                    dataTemplate.getDataContent().setFileName(temp.getString("Tag"));
                    dataTemplate.getDataContent().setSuffix(temp.getString("Suffix"));
                    dataTemplate.setPrepared(true);
                }
            }
        }
    }

    //对运行失败的action的输出进行处理
    private void updateFailedAction(Action action){
        List<DataTemplate> outputs = action.getOutputData().getOutputs();
        for (DataTemplate dataTemplate : outputs) {
            dataTemplate.getDataContent().setValue("error");
            dataTemplate.getDataContent().setType("Url");
            dataTemplate.getDataContent().setFileName("error");
            dataTemplate.getDataContent().setSuffix("");
            dataTemplate.setPrepared(true);
        }
    }

    public Boolean finalCheck(Task task) throws IOException, URISyntaxException {
        Map<String,List<ModelAction>> modelActionList = (Map<String,List<ModelAction>>) checkActions(task).get("model");
        Map<String,List<DataProcessing>> DataProcessingList = (Map<String,List<DataProcessing>>) checkActions(task).get("processing");

        List<ModelAction> waitingModelAction = modelActionList.get("waiting");
        List<ModelAction> failedModelAction = modelActionList.get("failed");
        List<ModelAction> runningModelAction = modelActionList.get("running");
        List<DataProcessing> waitingProcessings = DataProcessingList.get("waiting");
        List<DataProcessing> failedProcessings = DataProcessingList.get("failed");
        List<DataProcessing> runningProcessings = DataProcessingList.get("running");

        if((task.getModelActions()!=null&&failedModelAction.size()==task.getModelActions().size())){//task的model全部失败，则整个task失败
            task.setStatus(-1);
            task.setFinish(new Date());
            taskDao.save(task);
            return true;
        } else if(waitingModelAction.size()==0&&runningModelAction.size()==0&&waitingProcessings.size()==0&&runningProcessings.size()==0){
            task.setStatus(1);
            task.setFinish(new Date());
            taskDao.save(task);
            return true;
        }
        return false;
    }

    private void addToSharedData(Action completeAction, Task task ){//把结果加入共享文件组，已加过的模型不再加
        List<DataTemplate> outputData = completeAction.getOutputData().getOutputs();
        String actionId = completeAction.getId();
        if(sharedOutput==null){
            sharedOutput = new ConcurrentHashMap<>();
        }
        for(int i=0;i<outputData.size();i++){
            String dataId = outputData.get(i).getDataId();
            String url = outputData.get(i).getDataContent().getValue();
            if(sharedOutput != null){
                if(sharedOutput.containsKey(outputData.get(i).getDataId())){
                    List<String> datas = sharedOutput.get(dataId).getValues();
                    if (datas.contains(url)){
                        break;
                    }else {
                        datas.add(url);
                    }
                }else{
                    List<String> urls = new ArrayList<>();
                    if(url.indexOf("[")!=-1){
                        String[] urlStrs = url.substring(1,url.length()).split(",");

//                        urls = Arrays.asList(url.substring(1,url.length()).split(","));
                        for (String s : urlStrs) {
                            String str= s.replace("\"", "");
                            urls.add(str);
                        }
                    }else{
                        urls.add(url);
                    }
                    ShareData shareData = new ShareData(actionId, dataId,urls,"suc",outputData.get(i).getDataContent().getType());
                    sharedOutput.put(outputData.get(i).getDataId(),shareData);
                }
            }
//            else{
//                ShareData shareData = new ShareData(actionId, dataId,outputData.get(i).getDataContent().getValue(),outputData.get(i).getDataContent().getType());
//                sharedOutput.put(outputData.get(i).getDataId(),shareData);
//            }

        }

    }

    private void addToTempData(ModelAction modelAction){
        List<DataTemplate> outputData = modelAction.getOutputData().getOutputs();
        String actionId = modelAction.getId();
        if(tempOutput==null){
            tempOutput = new ConcurrentHashMap<>();
        }
        for(int i=0;i<outputData.size();i++){
            String dataId = outputData.get(i).getDataId();
            if(tempOutput != null){//添加临时文件池，上次迭代的结果要覆盖
                List<String> urls = new ArrayList<>();
                urls.set(0,outputData.get(i).getDataContent().getValue());
                ShareData shareData = new ShareData(actionId,dataId,urls,"suc",outputData.get(i).getDataContent().getType());
                tempOutput.put(outputData.get(i).getDataId(),shareData);
            }
        }
    }

    private int convertStatus(String taskStatus){
        int status;
        if(taskStatus.equals("Inited") || taskStatus.equals("Started") || taskStatus.equals("run")){
            //任务处于开始状态
            status = 0;
        }else if(taskStatus.equals("Finished")||taskStatus.equals("success")){
            status = 1;
        }else {
            status = -1;
        }
        return status;
    }

    private boolean checkData(Action modelAction,Task task){//检查数据齐全，并把数据加到对应的input
        List<DataTemplate> inputsList = modelAction.getInputData().getInputs();
        for (DataTemplate template : inputsList) {
            if(template.getDataContent().getType().equals("link")||template.getDataContent().getType().equals("mixed")){
                String value = template.getDataContent().getLink();
                if(sharedOutput == null||!sharedOutput.containsKey(value)){
                    template.setPrepared(false);
                    return false;
                }else{
                    if(sharedOutput.get(value).getValues().get(0).equals("error")){//说明上游数据错误
                        modelAction.setStatus(2);
                        return false;
                    }
                    try{
                        linkDataFlow(template, modelAction, task);
                    }catch (IOException e){

                    }
                }
            }
            else{
                if (template.getDataContent().getValue()==null||template.getDataContent().getValue().equals("")||template.getDataContent().getValue().equals("error")){
                    template.setPrepared(false);
                    modelAction.setStatus(2);//说明缺少数据，且无法配置
                    return false;
                }else{
                    template.setPrepared(true);
                }
            }
        }
        return true;
    }

    private boolean checkDataServicePrepared(DataProcessing dataProcessing,Task task) throws IOException {
        List<DataTemplate> inputsList = dataProcessing.getInputData().getInputs();
        for (DataTemplate template : inputsList) {
            if(template.getDataContent().getType().equals("link")||template.getDataContent().getType().equals("mixed")){
                String value = template.getDataContent().getLink();
                if(sharedOutput == null||!sharedOutput.containsKey(value)){
                    template.setPrepared(false);
                    return false;
                }else{
                    if(sharedOutput.get(value).getValues().get(0).equals("error")){//说明上游数据错误
                        dataProcessing.setStatus(2);
                        return false;
                    }
                    linkDataFlow(template,dataProcessing,task);
                }
            }
            else{
                if (template.getDataContent().getValue()==null||template.getDataContent().getValue().equals("")||template.getDataContent().getValue().equals("error")){
                    template.setPrepared(false);
                    dataProcessing.setStatus(2);//说明缺少数据，且无法配置
                    return false;
                }else{
                    template.setPrepared(true);
                }
            }
        }
        return true;
    }

    //单循环模型的判定
    private boolean checkSingleItePrepared(ModelAction modelAction,Task task) throws IOException {
        List<ModelAction> targetModelActionList = new ArrayList();
        for(int i = 0;i<iterationGroupList.size();i++){//找到该模型所在的iteration group
            if(iterationGroupList.get(i).contains(modelAction)){
                targetModelActionList = iterationGroupList.get(i);
                break;
            }
        }
        Integer currentStep = iterationCount.get(modelAction.getMd5());
        for(ModelAction ele:targetModelActionList){//判断迭代模型是否同步
            if(iterationCount.get(ele.getMd5())<currentStep){
                return false;
            }
        }

        List<DataTemplate> inputsList = modelAction.getInputData().getInputs();
        for (DataTemplate template : inputsList) {
            if(template.getDataContent().getType().equals("link")){
                String value = template.getDataContent().getLink();
                //TODO
                if(tempOutput.containsKey(value)){
                    linkIteActionDataflow(template, modelAction, task);
                }else if(sharedOutput.containsKey(value)){
                    linkDataFlow(template, modelAction, task);
                }else{
                    template.setPrepared(false);
                    return false;
                }
            }
            else{
                if (template.getDataContent().getValue()==null||template.getDataContent().getValue().equals("")){
                    template.setPrepared(false);
                    modelAction.setStatus(2);//说明缺少数据，且无法配置
                    return false;
                }else{
                    template.setPrepared(true);
                }
            }
        }

        return true;
    }

    public int checkCondition(Task task,Action action) throws IOException {
        String conditionId = action.getCondition();
        if(conditionId==null){
            return 1;//没有条件约束，则直接运行
        }

        ControlCondition controlCondition = getTargetCondition(task.getControlConditions(),conditionId);

        Boolean result = null;
        if(controlCondition!=null){
            result = conditions.get(conditionId);
            if(result==null) return 0;//条件判断结果未判断，不运行

            if((result == true&&controlCondition.getTrueAction().equals(action.getId()))
                    || (result == false&&controlCondition.getFalseAction().equals(action.getId()))){
                return 1;//条件判断符合
            }

            else {
                return -1;//条件判断不符合
            }
        }

        throw new IOException("condition not exist");


    }

    public String uploadDataProcessingOutput(String token,String dataId){
        String url = "http://"+dataServerIp +"/uploadData";
        JSONObject params = new JSONObject();
        params.put("token",token);
        params.put("dataId",dataId);

        RestTemplate restTemplate = new RestTemplate();
        JSONObject j_result = restTemplate.postForObject(url,params,JSONObject.class);

        int code = j_result.getInteger("code");
        String downloadUrl = null;
        if(code==-1){
            downloadUrl = "error";
        }else {
            downloadUrl = j_result.getJSONObject("message").getString("downloadUrl");
        }

        return downloadUrl;
    }

    /**
     * 将output填入需要的input中，如果是多输出还要新建model，数据转换则调用
     * @param template
     * @param modelAction
     * @param task
     * @throws IOException
     */
    public void linkDataFlow(DataTemplate template, Action modelAction, Task task) throws IOException {
        String md5 = "";
        if(modelAction instanceof ModelAction){
            modelAction = (ModelAction)modelAction;
            md5 = ((ModelAction) modelAction).getMd5();
            //String type
        }else if(modelAction instanceof DataProcessing){

        }

        String value = template.getDataContent().getLink();//input所需要的output的id
        List<String> urls = sharedOutput.get(value).getValues();
//        if(!checkConversion(task.getDataLink(),template.getDataId()).equals("0")){//转换数据
//            urlStr = convertData(urlStr,checkConversion(task.getDataLink(),template.getDataId()));
//        }

        String type = sharedOutput.get(value).getType();
        if (urls.size()>1) {//如果这是一个多输出

            List<ModelAction> targetModelActionList = getTargetModel(task.getModelActions(),md5);
            if(targetModelActionList.size()<urls.size()){//多输出匹配的下游model任务数量不够要补上
                int num = urls.size() - targetModelActionList.size();
                for (int j = 0; j < num; j++){
                    ObjectMapper objectMapper = new ObjectMapper();
                    ModelAction newModelAction = objectMapper.readValue(objectMapper.writeValueAsString(modelAction), ModelAction.class);

                    List<ModelAction> modelActions = task.getModelActions();
                    modelActions.add(newModelAction);
                    task.setModelActions(modelActions);
                }
            }

            targetModelActionList = getTargetModel(task.getModelActions(),md5);//重新获取要加的model列


            for (int i = 0; i < urls.size(); i++) {
                addOutputToInput(targetModelActionList.get(i),value,urls.get(i),type);//type是一致的

            }
        }else{
            String url = urls.get(0);
            template.getDataContent().setValue(url);
//            template.getDataContent().setType(sharedOutput.get(value).getType());
            template.setPrepared(true);
        }

        taskDao.save(task);

    }

    //todo 添加多输出管理
    public void linkIteActionDataflow(DataTemplate template, ModelAction modelAction, Task task){
        String value = template.getLink();
        String urlStr = tempOutput.get(value).getValues().get(0);
        String type = tempOutput.get(value).getType();

        template.getDataContent().setValue(urlStr);
        template.getDataContent().setType(tempOutput.get(value).getType());
        template.setPrepared(true);
    }

    //todo
    /**
     * 检查是否要转换数据
     * @param dataFlowMap
     * @param inputId
     * @return
     * @throws IOException
     */
    public String checkConversion(Map<String,DataLink> dataFlowMap,String inputId) throws IOException {
        DataLink dataLink = dataFlowMap.get(inputId);
//        if(dataLink.getTool()!=null){
//            return dataLink.getTool();
//        }
        return "no";
    }

    //todo
    /**
     *转换数据，返回转换后的数据
     * @param fromData
     */
    public String convertData(String fromData,String tool) throws IOException {
        String resultData = null;
        String url = "http://"+DATACONVERTSERVER+"/convert";
        JSONObject params = new JSONObject();
        params.put("input",fromData);
        params.put("tool",tool);

        System.out.println("使用工具:"+tool+"转换:"+fromData+"成功");
//        String resJson = MyHttpUtils.POSTWithJSON(url,"UTF-8",null,params);
//        JSONObject jResponse = JSONObject.parseObject(resJson);
//        if(jResponse.getInteger("code") == -1){
//            //说明找不到可用的地理模型服务
//            resultData = fromData;
//        }else{
//            //把返回信息填入该模型，进行标识
//        }

        return resultData;
    }

    /**
     把output加入到需要该文件的input中去
     * @param modelAction 对应的模型任务
     * @param dataValue input所link的output的id
     * @param url link的output的url,即文件
     * @param type data类型
     */
    public void addOutputToInput(ModelAction modelAction, String dataValue, String url,String type){
        List<DataTemplate> inputsList = modelAction.getInputData().getInputs();
        for(DataTemplate template : inputsList){
            if(template.getLink()!=null&&template.getLink().equals(dataValue)){
                template.getDataContent().setValue(url);
                template.getDataContent().setType(type);
            }
        }
    }

    /**
     * List中寻找对应的Action
     * @param srcActionList
     * @param id
     * @return
     */
    public Action getTargetAction(List<Action> srcActionList,String id){

        for(Action action : srcActionList){
            if(action.getId().equals(id)){
                return action;
            }
        }

        return null;
    }

    /**
     * List中寻找对应的FlowElement
     * @param flowElementList
     * @param id
     * @return
     */
    public FlowElement getTargetElement(List<FlowElement> flowElementList,String id){

        for(FlowElement flowElement : flowElementList){
            if(flowElement.getId().equals(id)){
                return flowElement;
            }
        }

        return null;
    }

    /**
     * List中寻找对应model的modelAction
     * @param srcModelActionList
     * @param targetPid 模型md5
     * @return
     */
    public  List<ModelAction> getTargetModel(List<ModelAction> srcModelActionList,String targetPid){
        List<ModelAction> modelActionList = new ArrayList<>();

        for(ModelAction modelAction : srcModelActionList){
            if(modelAction.getMd5().equals(targetPid)){
                modelActionList.add(modelAction);
            }
        }

        return modelActionList;
    }

    /**
     * List中寻找对应model的modelAction
     * @param srcModelActionList
     * @param actionId
     * @return
     */
    public  List<ModelAction> getTargetModelById(List<ModelAction> srcModelActionList,String actionId){
        List<ModelAction> modelActionList = new ArrayList<>();

        for(ModelAction modelAction : srcModelActionList){
            if(modelAction.getId().equals(actionId)){
                modelActionList.add(modelAction);
            }
        }

        return modelActionList;
    }

    /**
     * 寻找对应的processing
     * @param srcProcessingList
     * @param targetPid
     * @return
     */
    public  List<DataProcessing> getTargetProcessing(List<DataProcessing> srcProcessingList,String targetPid){
        List<DataProcessing> dataProcessingList = new ArrayList<>();
        if(srcProcessingList==null){
            return null;
        }
        for(DataProcessing dataProcessing : srcProcessingList){
            if(dataProcessing.getService().equals(targetPid)){
                dataProcessingList.add(dataProcessing);
            }
        }

        return dataProcessingList;
    }

    /**
     * 寻找对应的processing
     * @param srcProcessingList
     * @param actionId
     * @return
     */
    public  List<DataProcessing> getTargetProcessingById(List<DataProcessing> srcProcessingList,String actionId){
        List<DataProcessing> dataProcessingList = new ArrayList<>();
        if(srcProcessingList==null){
            return null;
        }

        for(DataProcessing dataProcessing : srcProcessingList){
            if(dataProcessing.getId().equals(actionId)){
                dataProcessingList.add(dataProcessing);
            }
        }

        return dataProcessingList;
    }

    public Iteration getTargetIteration(List<Iteration> iterations,String id){

        if(id == null)
            return null;
        for(Iteration iteration:iterations){
            if(iteration.getId().equals(id)){
                return iteration;
            }
        }

        return null;
    }

    //    public  getModelStatus
    public  ControlCondition getTargetCondition(List<ControlCondition> controlConditionList,String id){
        if(controlConditionList!=null){
            for(ControlCondition controlCondition : controlConditionList){
                if(controlCondition.getId().equals(id)){
                    return controlCondition;
                }
            }

        }
        return null;
    }
}
