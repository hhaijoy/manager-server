package com.example.demo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.DemoApplication;
import com.example.demo.bean.JsonResult;
import com.example.demo.dao.RunTaskDao;
import com.example.demo.dao.TaskDao;
import com.example.demo.domain.ModelContainer;
import com.example.demo.domain.Scheduler.Task;
import com.example.demo.domain.support.TaskNodeStatusInfo;
import com.example.demo.domain.xml.*;
import com.example.demo.dto.taskNode.TaskNodeReceiveDTO;
import com.example.demo.entity.RunTask;
import com.example.demo.thread.TaskLoopHandler;
import com.example.demo.utils.ResultUtils;
import com.example.demo.utils.StatusTransferUtils;
import com.example.demo.utils.TaskLoop;
import com.example.demo.utils.XmlParseUtils;
//import com.sun.xml.internal.ws.api.ha.StickyFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @Author: wangming
 * @Date: 2019-11-15 20:32
 */
@Service
public class TaskConfigurationService {

    @Autowired
    TaskDao taskDao;

    @Autowired
    ComputableService computableService;

    @Autowired
    RunTaskDao runTaskDao;

    private static final Logger log = LoggerFactory.getLogger(TaskConfigurationService.class);

    public String handlerTaskConfiguration(MultipartFile file, String userName){
        UUID taskId = UUID.randomUUID();
        //解析xml文档
        try {
            TaskConfiguration taskConfiguration = XmlParseUtils.parseXmlBaseOnStream(file.getInputStream(),"UTF-8");
            //TODO 验证文档中是否存在具体的模型服务
            //构建Task任务，存入数据库，同时提交任务到全局阻塞队列中，由另外线程进行处理
            Task task = new Task();
            task.setUid(taskConfiguration.getUid());
            task.setDate(new Date());
            task.setName(taskConfiguration.getName());
            task.setVersion(taskConfiguration.getVersion());
            task.setModels(taskConfiguration.getModels());
            task.setTaskId(taskId.toString());
            task.setUserName(userName);
            //数据库插入记录
            String id = taskDao.insert(task).getId();
            task.setId(id);
            LinkedBlockingDeque<Task> linkedBlockingDeque = DemoApplication.linkedBlockingDeque;
            linkedBlockingDeque.put(task);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return taskId.toString();
    }

    public String runTask(MultipartFile file, String userName){
        UUID taskId = UUID.randomUUID();
        log.info("}}}}}}}"+taskId.toString());
        //解析xml文档
        try {
            TaskConfiguration taskConfiguration = XmlParseUtils.parseXmlBaseOnStream(file.getInputStream(),"UTF-8");
            //TODO 验证文档中是否存在具体的模型服务
            Task task = new Task();
            task.setUid(taskConfiguration.getUid());
            log.info(task.getUid());
            task.setDate(new Date());
            task.setName(taskConfiguration.getName());
            task.setVersion(taskConfiguration.getVersion());
            task.setModels(taskConfiguration.getModels());
            List<ModelAction> ModelActions = taskConfiguration.getModelActions();
            List<ModelAction> modelActionList = new ArrayList<>();
            List<ModelAction> i_ModelActionList = new ArrayList<>();
            try{
                for(ModelAction modelAction : ModelActions){
                    if(modelAction.getIterationNum()<=1){
                        modelActionList.add(modelAction);
                    }else{
                        i_ModelActionList.add(modelAction);
                    }
                }
                task.setModelActions(modelActionList);
                task.setI_modelActions(i_ModelActionList);
            }catch (Exception e){

            }
            log.info(task.getUid());
            task.setDataProcessings(taskConfiguration.getDataProcessings());
            task.setControlConditions(taskConfiguration.getConditions());
            if(taskConfiguration.getDataLinks()!=null){
                Map<String,DataLink> dataLinkMap = new HashMap<>();
                for(DataLink dataLink:taskConfiguration.getDataLinks()){
                    dataLinkMap.put(dataLink.getTo(),dataLink);//to在前，便于后面以input为基准进行索引
                }
                task.setDataLink(dataLinkMap);
            }

            task.setTaskId(taskId.toString());
            task.setUserName(userName);
            //数据库插入记录
            String id = taskDao.insert(task).getId();
            TaskLoopHandler taskLoopHandler = new TaskLoopHandler(task);
            new Thread(taskLoopHandler).start();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return taskId.toString();
    }

    public JSONObject checkTaskStatus(String taskId) throws IOException, URISyntaxException {
        Task task = taskDao.findTaskByTaskId(taskId);
        TaskLoop taskLoop = new TaskLoop(task.getUserName());
        taskLoop.finalCheck(task);
        JSONObject result = new JSONObject();
        int status = task.getStatus();
        switch (status){
            case 0:
                result.put("msg","Task is running.");
                break;
            case -1:
                result.put("msg","Task run failed!");
                break;
            case 1:
                result.put("msg","Task run finished.");

                break;

        }
        result.put("taskInfo",getTaskInfo(task));
        result.put("status",status);

        return result;
    }

    public JSONObject getTaskInfo(Task task){
        JSONObject taskInfo = new JSONObject();

        TaskLoop taskLoop = new TaskLoop(task.getUserName());
        try {
            Map<String,Object> actionList = taskLoop.checkActions(task);

            taskInfo.put("taskId",task.getTaskId());
            taskInfo.put("name",task.getName());
            taskInfo.put("models",task.getModels());
            taskInfo.put("modelActions",task.getModelActions());
            taskInfo.put("modelActionList",actionList.get("model"));
            taskInfo.put("dataProcessingList",actionList.get("processing"));
            taskInfo.put("date",task.getDate());
            taskInfo.put("finish",task.getFinish());
            taskInfo.put("iterationCount",task.getIterationCount());

        }catch (Exception e){
            System.out.println("result error");
        }

        return taskInfo;
    }

    public boolean verifyTask(String pid){
        List<TaskNodeStatusInfo> result = computableService.getSuitableTaskNode(pid);
        if(result.size() == 0){
            return false;
        }else{
            return true;
        }
    }

    public boolean updateRecord(Task task){
        Task temp = taskDao.findTaskByTaskId(task.getTaskId());
        if(temp == null){
            return false;
        }else{
            task.setFinish(new Date());
            taskDao.save(task);
        }
        return true;
    }

    public Task getTask(String taskId){
        return taskDao.findTaskByTaskId(taskId);
    }


    public JsonResult updateRunTask(String runTaskId, String status) {
        RunTask runTask = runTaskDao.findFirstByRunTaskId(runTaskId);

        if (runTask == null)
            return ResultUtils.error(-1,"no object" );

        int st = StatusTransferUtils.TaskServer2ManagerServer(status);

        runTask.setStatus(st);

        runTaskDao.save(runTask);

        // System.out.println(status);

        return ResultUtils.success();

    }
}
