package com.example.demo.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.dao.ComputableModelDao;
import com.example.demo.dao.RunTaskDao;
import com.example.demo.dao.SubmittedTaskDao;
import com.example.demo.domain.ComputableModel;
import com.example.demo.domain.support.TaskNodeStatusInfo;
import com.example.demo.dto.computableModel.*;
import com.example.demo.dto.taskNode.TaskNodeReceiveDTO;
import com.example.demo.entity.DataItem;
import com.example.demo.entity.RunTask;
import com.example.demo.entity.SubmittedTask;
import com.example.demo.enums.ResultEnum;
import com.example.demo.exception.MyException;
import com.example.demo.sdk.*;
import com.example.demo.utils.MyFileUtils;
import com.example.demo.utils.MyHttpUtils;
import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by wang ming on 2019/3/15.
 */
@Service
public class ComputableService {

    @Autowired
    ComputableModelDao computableModelDao;

    @Autowired
    TaskNodeService taskNodeService;

    @Autowired
    RunTaskDao runTaskDao;

    @Autowired
    SubmittedTaskDao submittedTaskDao;

    @Resource
    private MongoTemplate mongoTemplate;


    private static final Logger log = LoggerFactory.getLogger(ComputableService.class);


    //测试所用的computableModel
    public ComputableModel insert(String name, String pid){
        ComputableModel computableModel = new ComputableModel();
        computableModel.setName(name);
        computableModel.setPid(pid);
        computableModel.setCreateTime(new Date());
        return computableModelDao.insert(computableModel);
    }

    public Task createTask(TaskServiceDTO taskServiceDTO){
        String ip = taskServiceDTO.getIp();
        int port = taskServiceDTO.getPort();
        String pid = taskServiceDTO.getPid();
        String username = taskServiceDTO.getUsername();
        GeoTaskServer taskServer = OGMSService_DEBUG.CreateTaskServer(ip, port);
        Task task;
        //type value contains: 1 - DataExchangeServer, 2 - DataServiceServer
        if(taskServiceDTO.getEx_ip() != null && taskServiceDTO.getType() != 0){
            if(taskServiceDTO.getType() == 1){
                GeoDataExServer dataExServer = OGMSService_DEBUG.CreateDataExchangeServer(taskServiceDTO.getEx_ip(),taskServiceDTO.getEx_port(),username);
                task = taskServer.createTask(pid,dataExServer,username);
            }else{
                GeoDataServiceServer dataServiceServer = OGMSService_DEBUG.CreateDataServiceServer(taskServiceDTO.getEx_ip(),taskServiceDTO.getEx_port(),username);
                task = taskServer.createTask(pid,dataServiceServer,username);
            }
        }else{
            task = taskServer.createTask(pid,null,username);
        }
        return task;
    }

    public ExDataDTO uploadData(UploadDataDTO uploadDataDTO){
        int type = uploadDataDTO.getType();
        MultipartFile file = uploadDataDTO.getFile();
        String filename = file.getOriginalFilename();
        String filenameWithoutExt = filename.substring(0,filename.lastIndexOf("."));
        String ext = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        String ip = uploadDataDTO.getHost();
        int port = uploadDataDTO.getPort();
        String userName = uploadDataDTO.getUserName();
        ExDataDTO exDataDTO = null;
        if(type == 1){
            exDataDTO = uploadDataToExServer(file,ip,port,filename,ext);
        }else if(type == 2){
            exDataDTO = uploadDataToDCServer(file,ip,port,filenameWithoutExt,ext,userName);
        }
        return exDataDTO;
    }

    /**
     * 上传数据到数据交换服务器
     * modified by wangming at 2019.11.07: 修改上传的函数参数，添加了文件后缀
     * @param file
     * @param ip
     * @param port
     * @param tag
     * @return com.example.demo.dto.computableModel.ExDataDTO
     * @author wangming
     * @date 2019/11/7 10:01
     */
    private ExDataDTO uploadDataToExServer(MultipartFile file,String ip, int port, String tag, String ext){
        ExDataDTO exDataDTO = new ExDataDTO();
        try {
            InputStream is = file.getInputStream();
            String md5 = MyFileUtils.getMD5(is);
            String url = "http://" + ip + ":" + port + "/data?md5=" + md5;
            String response = MyHttpUtils.GET(url,"UTF-8",null);
            JSONObject jResponse =  JSONObject.parseObject(response);
            if(jResponse.getString("result").equals("suc")){
                int code = jResponse.getIntValue("code");
                if(code == 1){
                    JSONObject jData = jResponse.getJSONObject("data");
                    String pwd = jData.getString("d_pwd");
                    pwd = MyFileUtils.decryption(MyFileUtils.decryption(pwd));
                    String id = jData.getString("id");
                    ExData tempData = new ExData(ip,port,id,pwd);
                    exDataDTO.setTag(tag);
                    exDataDTO.setUrl(tempData.getURL());
                    exDataDTO.setSuffix(ext);
                    is.close();
                    return exDataDTO;
                }else{
                    Map<String, String> params = new HashMap<String, String>();

                    params.put("datatag", tag);
                    params.put("pwd", "true");

                    String actionUrl = "http://" + ip + ":" + port + "/data";
                    Map<String,MultipartFile> fileMap = new HashMap<>();
                    fileMap.put("datafile",file);
                    String result = MyHttpUtils.POSTMultiPartFileToDataServer(actionUrl,"UTF-8",params,fileMap);
                    JSONObject jResult = JSONObject.parseObject(result);
                    if(jResult.getString("result").equals("suc")){
                        JSONObject jData = jResult.getJSONObject("data");
                        String pwd = jData.getString("d_pwd");
                        pwd = MyFileUtils.decryption(MyFileUtils.decryption(pwd));
                        String id = jData.getString("id");
                        ExData tempData = new ExData(ip,port,id,pwd);
                        exDataDTO.setTag(tag);
                        exDataDTO.setUrl(tempData.getURL());
                        exDataDTO.setSuffix(ext);
                        is.close();
                        return exDataDTO;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage());
        } catch (DecoderException e) {
            throw new RuntimeException(e.getMessage());
        }
        return null;
    }

    /**
     * 上传数据到数据服务容器
     * modified by wangming at 2019.11.07: 修改了返回的数据内容
     * @param file
     * @param ip
     * @param port
     * @param fileName
     * @param ext
     * @param userName
     * @return com.example.demo.dto.computableModel.ExDataDTO
     * @author wangming
     * @date 2019/11/7 10:03
     */
    private ExDataDTO uploadDataToDCServer(MultipartFile file, String ip, int port, String fileName, String ext, String userName){
        ExDataDTO exDataDTO = new ExDataDTO();
        try{
            String url = "http://" + ip + ":" + port + "/file/upload/store_dataResource_files";
            Map<String,MultipartFile> fileMap = new HashMap<>();
            fileMap.put("file",file);
            String result = MyHttpUtils.POSTMultiPartFileToDataServer(url,"UTF-8",null,fileMap);
            JSONObject jResponse = JSONObject.parseObject(result);
            if(jResponse.getIntValue("code") == 0){
                JSONObject dataObject = jResponse.getJSONObject("data");
                String dataId = dataObject.getString("source_store_id");
                //拼接post请求
                String dataUrl = "http://" + ip + ":" + port + "/dataResource";
                JSONObject formData = new JSONObject();
                formData.put("author", userName);
                formData.put("fileName", fileName);
                formData.put("sourceStoreId", dataId);
                formData.put("suffix", ext);
                formData.put("type", "OTHER");
                formData.put("fromWhere","MODELCONTAINER");

                String result2 = MyHttpUtils.POSTWithJSON(dataUrl,"UTF-8",null,formData);
                JSONObject jResult = JSONObject.parseObject(result2);
                if(jResult.getIntValue("code") == 0){
                    DCData tempData = new DCData(ip,port,dataId);
                    exDataDTO.setTag(fileName);
                    exDataDTO.setUrl(tempData.getURL());
                    exDataDTO.setSuffix(ext);
                    return exDataDTO;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return null;
    }


    /**
     * 调用模型容器前需先把相同输入数据的任务合并成一个
     * @author bin
     * @date 2021/7/23
     */
    public JSONObject invokeModelPrepare(TaskServiceDTO taskServiceDTO){
        // 需要先创建一个表来存储每个任务的信息
        // PS:其实不需要submittedTask来存每个任务的信息，用runTask存运行的信息就可以了
        /*SubmittedTask submittedTask = new SubmittedTask();
        submittedTask.setIp(taskServiceDTO.getIp());
        submittedTask.setPort(taskServiceDTO.getPort());
        submittedTask.setSubmittedTaskId(UUID.randomUUID().toString());
        submittedTask.setUserName(taskServiceDTO.getUsername());
        submittedTask.setMd5(taskServiceDTO.getPid());
        submittedTask.setStatus(0);
        submittedTask.setRunTime(new Date());
        submittedTask.setInputs(taskServiceDTO.getInputs());
        submittedTaskDao.insert(submittedTask);*/
        // 不太清楚OutputDataDTO类中的template是做什么用的，所以没有对传入的outputs做改动
        JSONObject result = mergeTask(taskServiceDTO);

        return result;

    }

    /**
     * 合并输入数据相同的任务，有相同的任务则合并，没有的话将该任务放到RunTask表中
     * 合并后数据相同的任务拥有相同的tid
     * @author bin
     * @date 2021/7/23
     */
    // TODO 未做ExDataDTO中urls属性的判断
    public JSONObject mergeTask(TaskServiceDTO taskServiceDTO){//如有参数相同的提交任务，合并到同一个执行任务，没有则新建一个

        String md5 = taskServiceDTO.getPid();

        List<RunTask> runTaskList = runTaskDao.findAllByMd5AndStatusOrStatus(md5,0,1);

        // 返回的数据
        JSONObject result = new JSONObject();

        if(runTaskList.size()!=0){
            for(RunTask runTask:runTaskList){
                if(compareTask(taskServiceDTO,runTask)==0){
                    continue;
                }else {
                    // List<String> relateTaskIds = runTask.getRelatedTasks();

                    // 合并的时候submittedTask需要更新如下字段
                    // Update updateST = new Update();
                    // updateST.set("runTaskId",runTask.getRunTaskId());
                    // updateST.set("ip",runTask.getIp());
                    // updateST.set("port",runTask.getPort());
                    // mongoTemplate.updateFirst(
                    //     Query.query(Criteria.where("submittedTaskId").is(submittedTask.getSubmittedTaskId())),
                    //     updateST,
                    //     SubmittedTask.class
                    // );
                    // submittedTask.setRunTaskId(runTask.getRunTaskId());
                    // submittedTaskDao.save(submittedTask);

                    // relateTaskIds.add(submittedTask.getSubmittedTaskId());
                    //runtask的字段必须单独更新，否则可能出现并发覆盖之前的修改
                    // Query query = Query.query(Criteria.where("runTaskId").is(runTask.getRunTaskId()));
                    // Update update = new Update();
                    // update.set("relatedTasks",relateTaskIds);
                    // mongoTemplate.updateFirst(query, update, RunTask.class);

                    result.put("tid",runTask.getRunTaskId());
                    result.put("ip",runTask.getIp());
                    result.put("port",runTask.getPort());

                    return result;
                }
            }
        }

        //如果没有相同的，则新建
        RunTask runTask = new RunTask();
        // runTask.setRunTaskId(UUID.randomUUID().toString());
        runTask.setIp(taskServiceDTO.getIp());
        runTask.setPort(taskServiceDTO.getPort());
        runTask.setMd5(taskServiceDTO.getPid());
        runTask.setStatus(0);
        runTask.setInputs(taskServiceDTO.getInputs());
        // runTask.setOutputs(submittedTask.getOutputs());
        // runTask与SubmittedTask进行关联
        // runTask.getRelatedTasks().add(submittedTask.getSubmittedTaskId());
        // runTask的id为调用TaskServer返回的tid
        result = invokeModel(taskServiceDTO);
        runTask.setRunTaskId(result.getString("tid"));

        runTaskDao.insert(runTask);

        // submittedTask.setRunTaskId(runTask.getRunTaskId());
        // submittedTaskDao.save(submittedTask);
        // mongoTemplate.updateFirst(
        //     Query.query(Criteria.where("submittedTaskId").is(submittedTask.getSubmittedTaskId())),
        //     (new Update()).set("runTaskId",runTask.getRunTaskId()),
        //     SubmittedTask.class
        // );

        return result;

    }

    public int compareTask(TaskServiceDTO taskServiceDTO, RunTask runTask){
        List<ExDataDTO> runInputs = runTask.getInputs();
        List<ExDataDTO> submitInputs = taskServiceDTO.getInputs();

        int flag;
        for(ExDataDTO runInput:runInputs){
            flag = 0;
            String runEvent = runInput.getEvent();
            String runData = runInput.getUrl();
            for(ExDataDTO submitInput:submitInputs){
                if(runEvent.equals(submitInput.getEvent())){
                    // 判断提交的数据Url是否与默认的几个选项的Url相同
                    if( runData.equals(submitInput.getUrl())){
                        flag = 1;
                        break;
                    }else {
                        return 0;
                    }
                }
            }

            if(flag==0){
                return 0;
            }
        }

        return 1;
    }


    public JSONObject invokeModel(TaskServiceDTO taskServiceDTO){
        //利用taskServiceDTO拼凑提交任务的form表单，从而提交任务
        String ip = taskServiceDTO.getIp();
        int port = taskServiceDTO.getPort();
        String pid = taskServiceDTO.getPid();
        String username = taskServiceDTO.getUsername();
        List<ExDataDTO> inputs = taskServiceDTO.getInputs();
        List<OutputDataDTO> outputs = taskServiceDTO.getOutputs();
        JSONObject params = new JSONObject();
        String inputsArray = convertItems2JSON(inputs);
        String outputsArray = convertOutputItems2JSON(outputs);
        params.put("inputs", inputsArray);
        params.put("username",username);
        params.put("pid", pid);
        params.put("outputs", outputsArray);

        String actionUrl = "http://" + ip + ":" + port + "/task";
        JSONObject result = new JSONObject();

        try{
            String resJson = MyHttpUtils.POSTWithJSON(actionUrl,"UTF-8",null,params);
            JSONObject jResponse = JSONObject.parseObject(resJson);
            if(jResponse.getString("result").equals("suc")){
                String tid = jResponse.getString("data");
                if(tid.equals("")){
                    throw new MyException(ResultEnum.NO_OBJECT);
                }
                result.put("tid",tid);
                result.put("ip",ip);
                result.put("port",port);

            }
        }catch (IOException e){
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }

    private String convertOutputItems2JSON(List<OutputDataDTO> outputs){
        JSONArray resultJson = new JSONArray();
        for(OutputDataDTO output:outputs){
            JSONObject temp = new JSONObject();
            temp.put("StateName",output.getStatename());
            temp.put("Event", output.getEvent());
            temp.put("Type",output.getTemplate().getType());
            temp.put("Value",output.getTemplate().getValue());
            resultJson.add(temp);
        }
        return resultJson.toJSONString();
    }

    public TaskResultDTO refreshRecord(TaskResultDTO taskResultDTO){
        String ip = taskResultDTO.getIp();
        int port = taskResultDTO.getPort();
        String tid = taskResultDTO.getTid();

        String url = "http://" + ip + ":" + port + "/task/" + tid;

        try{
            String resJson = MyHttpUtils.GET(url,"UTF-8",null);
            JSONObject jResponse = JSONObject.parseObject(resJson);
            if(jResponse.getString("result").equals("suc")){
                JSONObject jData = jResponse.getJSONObject("data");
                if(jData == null){
                    return null;
                }
                String taskStatus = jData.getString("t_status");
                int convertedStatus = convertStatus(taskStatus);
                taskResultDTO.setStatus(convertedStatus);
                // 更新runTask的状态
                // Query query = Query.query(Criteria.where("runTaskId").is(tid));
                // Update update = new Update();
                // update.set("status",convertedStatus);
                // mongoTemplate.updateFirst(query, update, RunTask.class);
                taskResultDTO.setPid(jData.getString("t_pid"));
                List<ExDataDTO> outputItems = new ArrayList<>();
                if(jData.getJSONArray("t_outputs") != null){
                    outputItems = convertJSON2Items(jData.getJSONArray("t_outputs"));
                    taskResultDTO.setOutputs(outputItems);
                }
            }else{
                return null;
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return taskResultDTO;
    }

    //代码重构，将公用模块抽出来成为一个方法
    public List<TaskNodeStatusInfo> getSuitableTaskNode(String pid){
        List<TaskNodeStatusInfo> result = new ArrayList<>();
        List<TaskNodeReceiveDTO> taskNodeList = taskNodeService.listAll();
        List<Future<TaskNodeStatusInfo>> futures = new ArrayList<>();
        //开启异步任务
        taskNodeList.forEach((TaskNodeReceiveDTO obj) ->{
            Future<TaskNodeStatusInfo> future = taskNodeService.judgeTaskNodeByPid(obj,pid);
            futures.add(future);
        });
        futures.forEach((future) ->{
            try {
                TaskNodeStatusInfo taskNodeStatusInfo = (TaskNodeStatusInfo)future.get();
                //判断
                if(taskNodeStatusInfo != null && taskNodeStatusInfo.isStatus()){
                    result.add(taskNodeStatusInfo);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        return result;
    }

    public JSONObject submitTask(TaskSubmitDTO taskSubmitDTO){
        String pid = taskSubmitDTO.getPid();
        List<TaskNodeStatusInfo> response = getSuitableTaskNode(pid);
        //TODO 根据算法进行择优选择,目前一个字段,所以排序获得就行（得分规则后面完善）
        response.sort(Comparator.comparingInt(TaskNodeStatusInfo::getRunning));
        TaskNodeStatusInfo taskNodeStatusInfo;
        if(response.size() != 0){
            taskNodeStatusInfo = response.get(0);
            //TODO 提交模型运行任务
            TaskServiceDTO taskServiceDTO = new TaskServiceDTO();
            taskServiceDTO.setIp(taskNodeStatusInfo.getHost());
            taskServiceDTO.setPort(Integer.parseInt(taskNodeStatusInfo.getPort()));
            taskServiceDTO.setPid(pid);
            taskServiceDTO.setUsername(taskSubmitDTO.getUserName());
            taskServiceDTO.setInputs(taskSubmitDTO.getInputs());
            taskServiceDTO.setOutputs(taskSubmitDTO.getOutputs());

            JSONObject result = invokeModel(taskServiceDTO);
            return result;
        }else{
            return null;
        }
    }

    private int convertStatus(String taskStatus){
        int status;
        if(taskStatus.equals("Inited")){
            status = 0;
        }else if(taskStatus.equals("Started")){
            status = 1; //started
        }else if(taskStatus.equals("Finished")){
            status = 2; //Finished
        }else {
            status = -1;
        }
        return status;
    }

    private String convertItems2JSON(List<ExDataDTO> inputs){
        JSONArray resultJson = new JSONArray();
        for(ExDataDTO input: inputs){
            JSONObject temp = new JSONObject();
            temp.put("StateName", input.getStatename());
            temp.put("Event", input.getEvent());
            temp.put("Url", input.getUrl());
            temp.put("Tag", input.getTag());
            temp.put("Suffix",input.getSuffix());
            resultJson.add(temp);
        }
        return resultJson.toJSONString();
    }

    private List<ExDataDTO> convertJSON2Items(JSONArray jOutputs){
        List<ExDataDTO> outputItems = new ArrayList<>();
        for(int i = 0; i < jOutputs.size(); i++){
            JSONObject temp = jOutputs.getJSONObject(i);
            if(temp.get("Type")==null){//Type为none说明output无内容，有内容则无此字段
                ExDataDTO exDataDTO = new ExDataDTO();
                exDataDTO.setStatename(temp.getString("StateName"));
                exDataDTO.setEvent(temp.getString("Event"));
                exDataDTO.setTag(temp.getString("Tag"));
                exDataDTO.setUrl(temp.getString("Url"));
                if (temp.getString("Url").contains("["))
                    exDataDTO.setUrls(temp.getJSONArray("Url"));
                exDataDTO.setSuffix(temp.getString("Suffix"));
                outputItems.add(exDataDTO);
            }
        }
        return outputItems;
    }

}
