package com.example.demo.controller;

import com.example.demo.dto.FileFinishForm;
import com.example.demo.dto.FileForm;
import com.example.demo.service.UploadFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Map;

/**
 * Created by wang ming on 2019/4/23.
 */
@RestController
@RequestMapping("/file")
@CrossOrigin
@Api(value = "上传文件模块")
public class UploadFileController {

    @Autowired
    UploadFileService uploadFileService;

    @PostMapping("/isUpload")
    @ApiOperation(value = "判断所给文件是否已经上传过")
    public Map<String, Object> isUpload(@Valid FileForm form){
        return uploadFileService.findByFileMd5(form.getMd5());
    }

    @PostMapping("/upload")
    @ApiOperation(value = "上传文件")
    public Map<String, Object> upload(@Valid FileForm form,
                                      @RequestParam(value = "data", required = false)MultipartFile multipartFile){
        Map<String, Object> map = null;

        try{
            map = uploadFileService.realUpload(form,multipartFile);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return map;
    }

    @PostMapping("/merge")
    @ApiOperation(value = "合并文件")
    public Map<String, Object> merge(@Valid FileFinishForm fileFinishForm){
        Map<String, Object> map = null;
        try{
            map = uploadFileService.merge(fileFinishForm.getMd5(), fileFinishForm.getTotal());
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return map;
    }
}
