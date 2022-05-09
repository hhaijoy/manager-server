package com.example.demo.controller;

import com.example.demo.bean.JsonResult;
import com.example.demo.domain.TaskNode;
import com.example.demo.domain.support.TaskNodeStatusInfo;
import com.example.demo.dto.taskNode.TaskNodeAddDTO;
import com.example.demo.dto.taskNode.TaskNodeCalDTO;
import com.example.demo.dto.taskNode.TaskNodeFindDTO;
import com.example.demo.dto.taskNode.TaskNodeReceiveDTO;
import com.example.demo.service.TaskNodeService;
import com.example.demo.utils.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Created by wang ming on 2019/2/18.
 */
@RestController
@Api(value = "任务服务器模块")
@RequestMapping(value = "/taskNode")
public class TaskNodeController {

    private static final Logger log = LoggerFactory.getLogger(TaskNodeController.class);

    @Autowired
    TaskNodeService taskNodeService;

    //WebAsyncTask异步请求处理
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ApiOperation(value = "任务服务器向管理服务器的注册")
    public WebAsyncTask<JsonResult> asyncTask(@RequestBody TaskNodeAddDTO taskNodeAddDTO){

        WebAsyncTask<JsonResult> webAsyncTask = new WebAsyncTask<JsonResult>(10000, new Callable<JsonResult>() {
            @Override
            public JsonResult call() throws Exception {
                if(taskNodeService.judgeTaskNode(taskNodeAddDTO.getHost(),taskNodeAddDTO.getPort())){
                    return ResultUtils.error(-1,"the task node has been registered");
                }else{
                    return ResultUtils.success(taskNodeService.insert(taskNodeAddDTO));
                }
            }
        });

        webAsyncTask.onCompletion(() ->{
            log.info("内部线程： " + Thread.currentThread().getName() + "执行完毕");
        });

        webAsyncTask.onTimeout(() ->{
            log.info("内部线程： " + Thread.currentThread().getName() + "onTimeout");
            throw new TimeoutException("调用超时");
        });
        return webAsyncTask;

    }


    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ApiOperation(value = "delete TaskNode By Id")
    JsonResult delete(@PathVariable("id") String id){
        taskNodeService.delete(id);
        return ResultUtils.success();
    }

    @RequestMapping(value = "/{id}",method = RequestMethod.GET)
    @ApiOperation(value = "get TaskNode By Id")
    JsonResult get(@PathVariable String id) {
        return ResultUtils.success(taskNodeService.getById(id));
    }


    @RequestMapping(value = "", method = RequestMethod.GET)
    @ApiOperation(value = "get TaskNode list by page and pagesize")
    JsonResult list(TaskNodeFindDTO taskNodeFindDTO)
    {
        return ResultUtils.success(taskNodeService.list(taskNodeFindDTO));
    }

    @RequestMapping(value = "/all", method = RequestMethod.GET)
    @ApiOperation(value = "get all TaskNode record")
    JsonResult listAll(){
        return ResultUtils.success(taskNodeService.listAll());
    }

    @RequestMapping(value = "/getTaskForRegister", method = RequestMethod.POST)
    @ApiOperation(value = "根据各个任务服务器的延迟信息进行运算，获取最适合注册的Task服务器")
    JsonResult getTaskServer(@RequestBody TaskNodeCalDTO taskNodeCalDTO){

        return ResultUtils.success(taskNodeService.getTaskServerForRegister(taskNodeCalDTO));
    }

    @RequestMapping(value = "/getServiceTask/{pid}", method = RequestMethod.GET)
    @ApiOperation(value = "根据模型pid找到最适合的任务服务器节点")
    JsonResult getTaskServerByPid(@PathVariable("pid") String pid){
        List<TaskNodeReceiveDTO> taskNodeList = taskNodeService.listAll();
        // taskNodeList = new ArrayList<>(Arrays.asList(taskNodeList.get(0)));
        List<Future<TaskNodeStatusInfo>> futures = new ArrayList<>();
        //开启异步任务
        taskNodeList.forEach((TaskNodeReceiveDTO obj) ->{
            Future<TaskNodeStatusInfo> future = taskNodeService.judgeTaskNodeByPid(obj,pid);
            futures.add(future);
        });
        List<TaskNodeStatusInfo> response = new ArrayList<>();
        futures.forEach((future) ->{
            try {
                TaskNodeStatusInfo taskNodeStatusInfo = (TaskNodeStatusInfo)future.get();
                //判断
                if(taskNodeStatusInfo != null && taskNodeStatusInfo.isStatus()){
                    response.add(taskNodeStatusInfo);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });

        //TODO 根据算法进行择优选择,目前一个字段,所以排序获得就行（得分规则后面完善）
        response.sort((o1,o2) ->{
            return o1.getRunning() - o2.getRunning();
        });
        if(response.size() != 0){
            return ResultUtils.success(response.get(0));
        }else{
            return ResultUtils.success();
        }
    }

    @RequestMapping(value = "/unregister", method = RequestMethod.POST)
    @ApiOperation(value = "unregister the task server")
    JsonResult unRegister(@RequestBody TaskNodeAddDTO taskNodeAddDTO){
        if(!taskNodeService.judgeTaskNode(taskNodeAddDTO.getHost(),taskNodeAddDTO.getPort())){
            return ResultUtils.error(-1,"the task node has not been registered");
        }else{
            TaskNode taskNode = taskNodeService.findTaskNodeByHost(taskNodeAddDTO.getHost(),taskNodeAddDTO.getPort());
            String id = taskNode.getId();
            taskNodeService.delete(id);
            return ResultUtils.success();
        }
    }

    @RequestMapping(value = "/getTaskForMicroService", method = RequestMethod.GET)
    @ApiOperation(value = "获取task服务器上是否存在type = 1的模型容器，之后根据各个服务器的延迟信息进行计算，获取最适合部署的Task服务器")
    JsonResult getTaskServerForMicroService(){
        //获取到所有的taskNode
        List<TaskNodeReceiveDTO> taskNodeList = taskNodeService.listAll();
        //TaskNodeStatusInfo: 包含 id, host, port ,status and running count
        List<Future<TaskNodeStatusInfo>> futures = new ArrayList<>();
        //开启异步任务
        taskNodeList.forEach((TaskNodeReceiveDTO obj) ->{
            Future<TaskNodeStatusInfo> future = taskNodeService.judgeTaskNodeAboutLocal(obj);
            futures.add(future);
        });

        //获取异步任务返回的结果
        List<TaskNodeStatusInfo> results = new ArrayList<>();
        futures.forEach((future) ->{
            try{
                TaskNodeStatusInfo taskNodeStatusInfo = future.get();
                //进行判断
                if(taskNodeStatusInfo != null && taskNodeStatusInfo.isStatus()){
                    results.add(taskNodeStatusInfo);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage());
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getMessage());
            }
        });

        //TODO 根据算法进行择优选择,目前一个字段,running count, 所以排序获得就行（得分规则后面完善）
        results.sort((o1,o2) -> Integer.compare(o1.getRunning(),o2.getRunning()));
        if(results.size() != 0){
            return ResultUtils.success(results.get(0));
        }else{
            return ResultUtils.error(-1,"No suitable task node for deploying model!");
        }
    }

    @RequestMapping(value = "/getAllServices", method = RequestMethod.GET)
    @ApiOperation(value = "获取到所有可用的模型服务")
    JsonResult getAllModelService(){
        List<TaskNodeReceiveDTO> taskNodeReceiveDTOList = taskNodeService.listAll();
        Set<String> result = new TreeSet<>();
        List<Future<List<String>>> futures = new ArrayList<>();
        //开启异步任务
        taskNodeReceiveDTOList.forEach((taskNodeReceiveDTO -> {
            Future<List<String>> future = taskNodeService.getAllServiceByTaskNode(taskNodeReceiveDTO);
            futures.add(future);
        }));
        futures.forEach((future) ->{
            try {
                List<String> serviceList = (List<String>) future.get();
                result.addAll(serviceList);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        return ResultUtils.success(result);
    }




}
