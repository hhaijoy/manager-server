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
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;

import com.example.demo.dao.TaskDao;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.spel.ast.Operator;
import sun.security.x509.AttributeNameEnumeration;

import javax.validation.constraints.Null;

import static com.example.demo.utils.MyFileUtils.getValueFromFile;

/**
 * created by Zihuan
 * 多模型集成运行
 * 2020.8.21
 */
public class TaskLoop {

    public static String MANAGERSERVER = "127.0.0.1:8084/GeoModeling";
    public static String DATACONVERTSERVER = "127.0.0.1:8084/GeoModeling";
    private ConcurrentHashMap<String, ShareData> tempOutput;
    private ConcurrentHashMap<String, ShareData> sharedOutput;//存储结构为k:modelAction id,v:output组成的shareData队列
    private ConcurrentHashMap<String, Boolean> conditions;//存储结构为k:modelAction id,v:output组成的shareData队列
    private ConcurrentHashMap<String, Integer> iterationCount;//记录一个task中迭代模型运行次数
    private List<List<ModelAction>> iterationGroupList;//记录一个task中的需要迭代的模型，关联的放一个group里
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
    }

    public void initTaskRun(Task task){
        initDataFlow(task);
        iterationInit(task);
        initCondition(task);
    }

    /**
     * 根据task中的link表设置每个input的link属性
     * @param task
     */
    public void initDataFlow(Task task){
        Map<String,DataLink> dataFlowMap = task.getDataLink();
        if(dataFlowMap!=null){
            List<ModelAction> modelActions = task.getModelActions();
            List<ModelAction> i_ModelActions = task.getI_modelActions();
            List<ModelAction> modelActionList = new ArrayList<>();

            modelActionList.addAll(modelActions);
            if(i_ModelActions!=null){
                modelActionList. addAll(i_ModelActions);//两个list都要加进去
            }

            for(ModelAction modelAction:modelActionList){
                for(DataTemplate dataTemplate:modelAction.getInputData().getInputs()){
                    if(dataTemplate.getDataContent().getType().equals("link")){
                        dataTemplate.setLink(dataFlowMap.get(dataTemplate.getDataId()).getFrom());
                    }
                }
            }
        }


    }

    /**
     *初始化条件判断
     * @param task
     */
    public void initCondition(Task task){
        List<ControlCondition> controlConditions = task.getControlConditions();

        if(controlConditions!=null){
            for(ControlCondition controlCondition:controlConditions){
                List<ModelAction> tModelActions = getTargetModel(task.getModelActions(),controlCondition.getTrueAction());//获得条件判断为真时连接的modelAction
                ModelAction tModelAction = tModelActions.get(1);
                tModelAction.setCondition(controlCondition.getId());

                List<ModelAction> fModelActions = getTargetModel(task.getModelActions(),controlCondition.getFalseAction());
                ModelAction fModelAction = fModelActions.get(1);
                fModelAction.setCondition(controlCondition.getId());

            }
        }

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
    public int query(List<ModelAction> waitingModels,List<DataProcessing> waitingProcessings , Task task) throws IOException, URISyntaxException {
        int result = 0;

        for(int i=0;i<waitingModels.size();i++){
            ModelAction modelAction = waitingModels.get(i);
            if(checkCondition(task,modelAction)==1){
                if(modelAction.getIterationNum()==1 && checkData(modelAction,task)){//如果是个单运行模型
                    runModel(modelAction);
                }else if(modelAction.getIterationNum()>1&&checkItePrepared(modelAction,task)){//如果是个迭代模型
                    runModel(modelAction);
                }
            }

        }

        if(waitingProcessings!=null){
            DataServiceTask dataServiceTask = new DataServiceTask();
            List<Future> futures = new ArrayList<>();
            for(int i=0;i<waitingProcessings.size();i++){
                DataProcessing dataProcessing = waitingProcessings.get(i);
                if(dataProcessing.getType().equals("modelService") ){//如果是个modelService
                    if(checkData(dataProcessing,task)){
                        runModel(dataProcessing);
                    }
                }else if(dataProcessing.getType().equals("dataService")){
                    if(checkDataServicePrepared(dataProcessing)){
                        futures.add(dataServiceTask.getDataServiceResult(dataProcessing,0,task));
                    }
                }
            }
        }


        if(task.getControlConditions()!=null){
            for(ControlCondition controlCondition:task.getControlConditions()){//判断条件
                if(controlCondition.getStatus()!=1){
                    judgeCondition(controlCondition);
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

    public Map<String,Object> checkActions(Task task) throws IOException, URISyntaxException {
        Map<String,Object> result = new HashMap<>();
        result.put("processing",checkProcessings(task));
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
                    if(dataProcessing.getStatus()==1){
                        addToSharedData(dataProcessing,task);
                        completedProcessing.add(dataProcessing);
                    }else if(dataProcessing.getStatus()==0){
                        if(dataProcessing.getRemark()!=null&&dataProcessing.getRemark().equals("running")){
                            runningProcessing.add(dataProcessing);
                        }else{
                            waitingProcessing.add(dataProcessing);
                        }
                    }else if(dataProcessing.getStatus()==-1){
                        failedProcessing.add(dataProcessing);
                    }
                }

            }

            taskDao.save(task);
        }


        result.put("waiting",waitingProcessing);
        result.put("running",completedProcessing);
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

        modelActionList.addAll(models);
        if(iModels!=null){
            modelActionList.addAll(iModels);//两个list都要加进去
        }

        ModelAction modelAction = new ModelAction();
        for(int i=0;i<modelActionList.size();i++){
            modelAction = modelActionList.get(i);
            if(checkCondition(task,modelAction)==-1){//如果是个条件判断为false的模型任务，则不会去运行
                ignoredModel.add(modelAction);
            }else if(modelAction.getStatus()==1){//已经运行好的模型任务
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
                                    modelAction.setStatus(1);
                                    JSONArray jOutputs = jData.getJSONArray("t_outputs");
                                    updateModelOutputByTask(jOutputs,modelAction);
                                    addToSharedData(modelAction,task);
                                    completedModel.add(modelAction);
                                }


                            }else if(modelStatus == -1||modelStatus == 2){
                                modelAction.setStatus(2);
                                failedModel.add(modelAction);
                            }else if(modelStatus == 0){
                                runningModel.add(modelAction);
                            }
                        }
                    }else{
                        // 返回result为err，说明taskServer可能出问题了，因为查询不到记录,往外面抛出错误
                        throw new IOException("task Server Error");
                    }
                }else{
                    waitingModel.add(modelAction);
                }
            }
        }

        taskDao.save(task);

        result.put("waiting",waitingModel);
        result.put("running",runningModel);
        result.put("completed",completedModel);
        result.put("failed",failedModel);

        return result;
    }

    public JSONObject checkAction(Action action) throws IOException, URISyntaxException {
        String taskId = action.getTaskId();
        String taskIp = action.getTaskIp();
        int port = action.getPort();
        String taskQueryUrl = "http://" + taskIp + ":" + port + "/task/" + taskId;

        String taskResult = MyHttpUtils.GET(taskQueryUrl, "UTF-8",null);
        return JSONObject.parseObject(taskResult);
    }

    public void judgeCondition(ControlCondition controlCondition){
        String valueId = controlCondition.getValue();
        if(sharedOutput.contains(valueId)){
            String url = sharedOutput.get(valueId).getValue();

            String value = MyFileUtils.getValueFromFile(url);
            if(controlCondition.getFormat().toLowerCase()=="number"){
                float targetV = Float.parseFloat(value) ;
                boolean result = true;
                for(ConditionCase conditionCase:controlCondition.getConditionCases()){
                    Float standard = Float.parseFloat(conditionCase.getStandard());
                    result = opMap.get(conditionCase.getOpertator()).calculate(targetV,standard);
                    String relation = "and";
                    if(conditions.size() == 0||!conditions.containsKey(controlCondition.getId())){//第一个case的判断
                        relation = conditionCase.getRelation();//与第二个条件的关系
                        conditions.put(controlCondition.getId(),result);
                    }else{
                        boolean formerResult = conditions.get(controlCondition.getId());
                        if(relation.equals("and")){
                            conditions.put(controlCondition.getId(),formerResult&&result);
                        }else{
                            conditions.put(controlCondition.getId(),formerResult||result);
                        }
                        relation = conditionCase.getRelation();//更新与下一个条件的关系
                    }

                }
            }else if(controlCondition.getFormat().toLowerCase()=="string"){
                boolean result = true;
                for(ConditionCase conditionCase:controlCondition.getConditionCases()){
                    String standard = conditionCase.getStandard();
                    if(value.equals(standard)){
                        result = true;
                    }else{
                        result = false;
                    }
                    String relation = "and";
                    if(conditions.size() == 0||!conditions.containsKey(controlCondition.getId())){
                        relation = conditionCase.getRelation();//与第二个条件的关系
                        conditions.put(controlCondition.getId(),result);
                    }else{
                        boolean formerResult = conditions.get(controlCondition.getId());
                        if(conditionCase.getRelation().equals("and")){
                            conditions.put(controlCondition.getId(),formerResult&&result);
                        }else{
                            conditions.put(controlCondition.getId(),formerResult||result);
                        }
                        relation = conditionCase.getRelation();//更新与下一个条件的关系
                    }
                }
            }

            controlCondition.setStatus(1);//标识是否已经完整判断过，已判断的不再判断
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

    public Boolean finalCheck(Task task) throws IOException, URISyntaxException {
        Map<String,List<ModelAction>> modelActionList = (Map<String,List<ModelAction>>) checkActions(task).get("model");
        Map<String,List<DataProcessing>> DataProcessingList = (Map<String,List<DataProcessing>>) checkActions(task).get("processing");

        List<ModelAction> waitingModelAction = modelActionList.get("waiting");
        List<ModelAction> failedModelAction = modelActionList.get("failed");
        List<ModelAction> runningModelAction = modelActionList.get("running");
        List<DataProcessing> waitingProcessings = DataProcessingList.get("waiting");
        List<DataProcessing> failedProcessings = DataProcessingList.get("failed");
        List<DataProcessing> runningProcessings = DataProcessingList.get("running");

        if(failedModelAction.size()==task.getModelActions().size()){//task的model全部失败，则整个task失败
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
        for(int i=0;i<outputData.size();i++){
            if(sharedOutput != null){
                if(sharedOutput.containsKey(outputData.get(i).getDataId())){
                    break;
                }else{
                    ShareData shareData = new ShareData(outputData.get(i).getDataContent().getValue(),outputData.get(i).getDataContent().getType());
                    sharedOutput.put(outputData.get(i).getDataId(),shareData);
                }
            }else{
                ShareData shareData = new ShareData(outputData.get(i).getDataContent().getValue(),outputData.get(i).getDataContent().getType());
                sharedOutput.put(outputData.get(i).getDataId(),shareData);
            }

        }

    }

    private void addToTempData(ModelAction modelAction){
        List<DataTemplate> outputData = modelAction.getOutputData().getOutputs();
        for(int i=0;i<outputData.size();i++){
            if(tempOutput != null){//添加临时文件池，上次迭代的结果要覆盖
                ShareData shareData = new ShareData(outputData.get(i).getDataContent().getValue(),outputData.get(i).getDataContent().getType());
                tempOutput.put(outputData.get(i).getDataId(),shareData);
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

    private boolean checkData(Action modelAction,Task task){//检查数据齐全，并把数据加到对应的input
        List<DataTemplate> inputsList = modelAction.getInputData().getInputs();
        for (DataTemplate template : inputsList) {
            if(template.getDataContent().getType().equals("link")){
                String value = template.getDataContent().getLink();
                if(sharedOutput == null||!sharedOutput.containsKey(value)){
                    template.setPrepared(false);
                    return false;
                }else{
                    try{
                        linkDataFlow(template, modelAction, task);
                    }catch (IOException e){

                    }
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

    private boolean checkDataServicePrepared(DataProcessing dataProcessing){
        List<DataTemplate> inputsList = dataProcessing.getInputData().getInputs();
        for (DataTemplate template : inputsList) {
            if(template.getDataContent().getType().equals("link")){
                String value = template.getDataContent().getLink();
                if(sharedOutput == null||!sharedOutput.containsKey(value)){
                    template.setPrepared(false);
                    return false;
                }else{
                    template.getDataContent().setValue(sharedOutput.get(value).getValue());
                    template.getDataContent().setType(sharedOutput.get(value).getType());
                    template.setPrepared(true);
                }
            }
            else{
                if (template.getDataContent().getValue()==null||template.getDataContent().getValue().equals("")){
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

    private boolean checkItePrepared(ModelAction modelAction,Task task) throws IOException {
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
                    linkIteDataflow(template, modelAction, task);
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

    public int checkCondition(Task task,Action action){
        String conditionId = action.getCondition();
        if(conditionId==null){
            return 1;//没有条件约束，则直接运行
        }

        ControlCondition controlCondition = getTargetCondition(task.getControlConditions(),conditionId);

        Boolean result = null;
        if(controlCondition!=null){
            result = conditions.get(conditionId);
            if((result == true&&controlCondition.getTrueAction().equals(action.getId()))
                    || (result == false&&controlCondition.getFalseAction().equals(action.getId()))){
                return 1;//条件判断符合
            }

            else {
                return -1;//条件判断不符合
            }
        }

        return 0;

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
        }else if(modelAction instanceof DataProcessing){

        }

        String value = template.getDataContent().getLink();//input所需要的output的id
        String urlStr = sharedOutput.get(value).getValue();
        if(!checkConversion(task.getDataLink(),template.getDataId()).equals("0")){//转换数据
//            urlStr = convertData(urlStr,checkConversion(task.getDataLink(),template.getDataId()));
        }

        String type = sharedOutput.get(value).getType();
        if (urlStr.indexOf("[") != -1) {//如果这是一个多输出
            urlStr = urlStr.substring(1, urlStr.length() - 1);
            String[] urls = urlStr.split(",");

            List<ModelAction> targetModelActionList = getTargetModel(task.getModelActions(),md5);
            if(targetModelActionList.size()<urls.length){//多输出匹配的下游model任务数量不够要补上
                int num = urls.length - targetModelActionList.size();
                for (int j = 0; j < num; j++){
                    ObjectMapper objectMapper = new ObjectMapper();
                    ModelAction newModelAction = objectMapper.readValue(objectMapper.writeValueAsString(modelAction), ModelAction.class);

                    List<ModelAction> modelActions = task.getModelActions();
                    modelActions.add(newModelAction);
                    task.setModelActions(modelActions);
                }
            }

            targetModelActionList = getTargetModel(task.getModelActions(),md5);//重新获取要加的model列

            for (int i = 0; i < urls.length; i++) {
                addOutputToInput(targetModelActionList.get(i),value,urls[i],type);//type是一致的

            }
        }else{
            template.getDataContent().setValue(sharedOutput.get(value).getValue());
            template.getDataContent().setType(sharedOutput.get(value).getType());
            template.setPrepared(true);
        }


    }

    public void linkIteDataflow(DataTemplate template, ModelAction modelAction, Task task){
        String value = template.getLink();
        String urlStr = tempOutput.get(value).getValue();
        String type = tempOutput.get(value).getType();
        template.getDataContent().setValue(tempOutput.get(value).getValue());
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
            if(template.getLink().equals(dataValue)){
                template.getDataContent().setValue(url);
                template.getDataContent().setType(type);
            }
        }
    }

    /**
     * List中寻找对应的Action
     * @param srcModelActionList
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
     * List中寻找对应model的modelAction
     * @param srcModelActionList
     * @param targetPid
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
     * 寻找对应的processing
     * @param srcProcessingList
     * @param targetPid
     * @return
     */
    public  List<DataProcessing> getTargetProcessing(List<DataProcessing> srcProcessingList,String targetPid){
        List<DataProcessing> dataProcessingList = new ArrayList<>();

        for(DataProcessing dataProcessing : srcProcessingList){
            if(dataProcessing.getService().equals(targetPid)){
                dataProcessingList.add(dataProcessing);
            }
        }

        return dataProcessingList;
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
