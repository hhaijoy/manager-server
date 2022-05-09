package com.example.demo.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.bean.JsonResult;
import com.example.demo.dto.computableModel.TaskResultDTO;
import com.example.demo.dto.computableModel.TaskServiceDTO;
import com.example.demo.dto.computableModel.TaskSubmitDTO;
import com.example.demo.dto.computableModel.UploadDataDTO;
import com.example.demo.service.ComputableService;
import com.example.demo.utils.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

/**
 * Created by wang ming on 2019/2/26.
 */
@RestController
@Api(value = "计算任务模块")
@RequestMapping(value = "/computableModel")
public class ComputableModelController {

    private static final Logger log = LoggerFactory.getLogger(ComputableModelController.class);

    @Autowired
    ComputableService computableService;

    @RequestMapping(value = "", method = RequestMethod.POST)
    @ApiOperation(value = "计算模型名称与模型pid的相互绑定")
    public JsonResult upload(@RequestParam("name") String name, @RequestParam("pid") String pid){
        return ResultUtils.success(computableService.insert(name, pid));
    }

    @RequestMapping(value = "/createTask", method = RequestMethod.POST)
    @ApiOperation(value = "创建taskServer,预准备过程", notes = "根据TaskServiceDTO创建一个Task")
    public JsonResult createTask(@RequestBody TaskServiceDTO taskServiceDTO){
        return ResultUtils.success(computableService.createTask(taskServiceDTO));
    }

    @RequestMapping(value = "/uploadData", method = RequestMethod.POST)
    @ApiOperation(value = "上传模型运行数据", notes = "根据UploadDataDTO上传用户数据")
    public JsonResult uploadData(UploadDataDTO uploadDataDTO){
        MultipartFile file = uploadDataDTO.getFile();
        if(!file.isEmpty()){
            return ResultUtils.success(computableService.uploadData(uploadDataDTO));
        }else{
            return ResultUtils.error(-1,"上传文件为空");
        }

    }

    @RequestMapping(value = "/invoke", method = RequestMethod.POST)
    @ApiOperation(value = "运行模型服务，提交task任务")
    public JsonResult invokeModel(@RequestBody TaskServiceDTO taskServiceDTO){

        return ResultUtils.success(computableService.invokeModelPrepare(taskServiceDTO));
    }

    @RequestMapping(value = "/refreshTaskRecord", method = RequestMethod.POST)
    @ApiOperation(value = "获取task运行记录信息", notes = "根据Task Server的IP和Port以及Task ID获取运行记录")
    public JsonResult refreshTaskRecord(@RequestBody TaskResultDTO taskResultDTO){
        TaskResultDTO temp = computableService.refreshRecord(taskResultDTO);
        if (temp == null){
            return ResultUtils.error(-1,"任务服务器出错");
        }
        return ResultUtils.success(temp);
    }

    @RequestMapping(value = "/submitTask", method = RequestMethod.POST, produces = { "application/json;charset=UTF-8" })
    @ApiImplicitParam(paramType = "body", dataType = "TaskSubmitDTO", name = "taskSubmitDTO", value = "任务提交实体", required = true)
    @ApiOperation(value = "用户提交模型pid，直接运行模型")
    public JsonResult submitTask(@Valid @RequestBody TaskSubmitDTO taskSubmitDTO){
        //首先根据pid找到最适合的Task-Server节点
        JSONObject result = computableService.submitTask(taskSubmitDTO);
        if (result == null){
            return ResultUtils.error(-1,"找不到可用的地理模型服务运行");
        }else{
            return ResultUtils.success(result);
        }
    }

    @RequestMapping(value = "/checkDeployed", method = RequestMethod.GET)
    @ApiImplicitParam(paramType = "body", dataType = "String", name = "md5", value = "检查是否有该md5对应的模型服务", required = true)
    @ApiOperation(value = "检查是否有该md5对应的模型服务")
    public JsonResult checkDeployed(@RequestParam(value="md5") String md5){
        //首先根据pid找到最适合的Task-Server节点
        Boolean deployed = computableService.checkDeplyed(md5);

        return ResultUtils.success(deployed);

    }

}
