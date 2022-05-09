package com.example.demo.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.bean.JsonResult;
import com.example.demo.domain.Scheduler.Task;
import com.example.demo.service.TaskConfigurationService;
import com.example.demo.utils.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


/**
 * 计算任务提交类
 * @Author: wangming
 * @Date: 2019-11-14 19:26
 */
@RestController
@Api(value = "计算任务提交计算模块")
@RequestMapping(value = "/task")
@CrossOrigin
public class TaskCalculatorController {

    private static final Logger log = LoggerFactory.getLogger(TaskCalculatorController.class);

    @Value("${prop.task-upload}")
    private String TASK_UPLOAD_FOLDER;

    @Autowired
    TaskConfigurationService taskConfigurationService;

    @RequestMapping(value = "/verify/{pid}",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "根据pid来验证是否存在可用的地理模型服务")
    public JsonResult verifyTask(@PathVariable("pid") String pid){
        return ResultUtils.success(taskConfigurationService.verifyTask(pid));
    }

    @RequestMapping(value = "", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "提交模型运行任务")
    JsonResult submitTask(@RequestParam("file") MultipartFile file, @RequestParam("userName") String userName){
        if (file.isEmpty()) {
            return ResultUtils.error(-1, "上传的文件为空");
        }else{
            String fileName = file.getOriginalFilename();
            String suffix = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
            //TODO 验证xml文件是否符合标准格式(校验函数待实现)
            if (!suffix.equals("xml") || false){
                return ResultUtils.error(-1, "上次的文件不是xml或者文件不符合规定");
            }else{
                //TODO 进行处理
                String uid = taskConfigurationService.handlerTaskConfiguration(file, userName);
                if(uid == null){
                    return ResultUtils.error(-1,"解析xml文件出现问题！");
                }
                return ResultUtils.success(uid);
            }
        }
    }


    /**
     * wzh
     * 提交模型任务，使用taskloop处理
     * @param file
     * @param userName
     * @return
     */
    @RequestMapping(value = "/runTask", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "提交模型运行任务")
    JsonResult runTask(@RequestParam("file") MultipartFile file, @RequestParam("userName") String userName){
        if (file.isEmpty()) {
            return ResultUtils.error(-1, "上传的文件为空");
        }else{
            String fileName = file.getOriginalFilename();
            String suffix = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
            //TODO 验证xml文件是否符合标准格式(校验函数待实现)
            if (!suffix.equals("xml") || false){
                return ResultUtils.error(-1, "上次的文件不是xml或者文件不符合规定");
            }else{
                //TODO 进行处理
                String uid = taskConfigurationService.runTask(file, userName);
                System.out.println("taskid是"+uid);
                if(uid == null){
                    return ResultUtils.error(-1,"解析xml文件出现问题！");
                }
                return ResultUtils.success(uid);
            }
        }
    }

    /**
     * wzh
     * 查询task的运行状况,
     * @param taskId
     * @return
     */
    @RequestMapping(value = "/checkTaskStatus", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "查询集成模型task的运行状况")
    JsonResult checkTaskStatus(@RequestParam("taskId") String taskId){
            return ResultUtils.success(taskConfigurationService.checkTaskStatus(taskId));
    }

    @RequestMapping(value = "/updateRecord", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "更新任务运行记录")
    JsonResult updateRecord(@RequestBody Task task){
        if(!taskConfigurationService.updateRecord(task)){
            return ResultUtils.error(-1,"update record error!");
        }else{
            return ResultUtils.success(true);
        }
    }

    @GetMapping(value = "/updateRunTask/{runTaskId}")
    @ApiOperation(value = "更新正在运行的记录")
    JsonResult updateRunTask(@PathVariable String runTaskId, @RequestParam String status){
        return taskConfigurationService.updateRunTask(runTaskId, status);
    }


    @RequestMapping(value="/checkRecord", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value="检查模型运行状态，并返回数据")
    JsonResult checkRecord(@RequestParam("taskId") String taskId){
        Task task=taskConfigurationService.getTask(taskId);
        int status=task.getStatus();
        JSONObject result=new JSONObject();
        result.put("status",status);
        switch (status){
            case 0:
                result.put("msg","Task is running.");
                break;
            case -1:
                result.put("msg","Task run failed!");
                break;
            case 1:
                result.put("msg","Task run finished.");
                result.put("models",task.getModels());
                break;

        }
        return  ResultUtils.success(result);
    }


}
