package com.example.demo.controller;

import com.example.demo.bean.JsonResult;
import com.example.demo.domain.ModelContainer;
import com.example.demo.service.ModelContainerService;
import com.example.demo.utils.MyFileUtils;
import com.example.demo.utils.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

/**
 * 模型服务容器版本管理
 * Created by wang ming on 2019/4/22.
 */
@RestController
@Api(value = "模型服务容器版本管理模块")
@RequestMapping(value = "/modelContainer")
@CrossOrigin
public class ModelContainerController {

    private static final Logger log = LoggerFactory.getLogger(ModelContainerController.class);

    @Value("${prop.upload-folder}")
    private String UPLOAD_FOLDER;

    @Autowired
    ModelContainerService modelContainerService;

    @RequestMapping(value = "", method = RequestMethod.POST)
    @ApiOperation(value = "上传特定版本的模型服务容器程序")

    public JsonResult uploadModelContainer(@RequestParam("file")MultipartFile file, @RequestParam("version") String version){
        if(!file.isEmpty()){
            if(version.isEmpty() || version == null){
                return ResultUtils.error(-1, "版本字段不能为空");
            }
            return ResultUtils.success(modelContainerService.uploadModelContainer(file,version));
        }else{
            return ResultUtils.error(-1,"上传文件为空");
        }
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ApiOperation(value = "获取最新版本的模型服务容器版本号")
    public JsonResult getNewestVersion(){
        return ResultUtils.success(modelContainerService.getTheNewestRecord());
    }

    @RequestMapping(value = "/download/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "根据id下载对应版本的模型服务容器")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id){
        String path = modelContainerService.findFilePathById(id);
        if(path == null){
            return null;
        }
        File file = new File(UPLOAD_FOLDER + path);
        InputStream inputStream = MyFileUtils.getInputStream(file);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", "attachment;filename=" + file.getName());
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity
                .ok()
                .contentLength(file.length())
                .headers(headers)
                .body(new InputStreamResource(inputStream));
    }

    @RequestMapping(value = "/downloadNewest", method = RequestMethod.GET)
    @ApiOperation(value = "下载最新版本的模型服务容器exe")
    public ResponseEntity<InputStreamResource> downloadTheNewest(){
        ModelContainer modelContainer = modelContainerService.getTheNewestRecord();
        if(modelContainer == null){
            return null;
        }
        String filePath = modelContainer.getPath();
        File file = new File(UPLOAD_FOLDER + filePath);
        InputStream inputStream = MyFileUtils.getInputStream(file);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", "attachment; filename=" + file.getName());
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add("Last-Modified", new Date().toString());
        headers.add("ETag", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(new InputStreamResource(inputStream));

    }


}
