package com.example.demo.service;

import com.example.demo.Common.Constant;
import com.example.demo.dao.UploadFileRespository;
import com.example.demo.domain.UploadFile;
import com.example.demo.dto.FileForm;
import com.example.demo.utils.FileMd5Util;
import com.example.demo.utils.MyFileUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by wang ming on 2019/4/23.
 */
@Service
public class UploadFileService {
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    @Autowired
    UploadFileRespository uploadFileRespository;

    public Map<String, Object> findByFileMd5(String md5){
        UploadFile uploadFile = uploadFileRespository.findFirstByFileMd5(md5);

        Map<String, Object> map = null;

        if(uploadFile == null){
            //说明未上传过文件
            map = new HashMap<>();
            map.put("flag", 0);
            map.put("fileId", UUID.randomUUID().toString());
            map.put("date", simpleDateFormat.format(new Date()));
        }else{
            //上传过文件，判断文件现在还存不存在
            File file = new File(uploadFile.getFilePath());

            if(file.exists()){
                if(uploadFile.getFileStatus() == 1){
                    //文件只上传了一部分
                    map = new HashMap<>();
                    map.put("flag","1");
                    map.put("fileId", uploadFile.getFileId());
                    map.put("date", simpleDateFormat.format(new Date()));
                }else if(uploadFile.getFileStatus() == 2){
                    //文件早已上传完整
                    map = new HashMap<>();
                    map.put("flag","2");
                    map.put("fileId", uploadFile.getFileId());
                    map.put("date", simpleDateFormat.format(new Date()));
                }
            }else{
                map = new HashMap<>();
                map.put("flag", "0");
                map.put("fileId", uploadFile.getFileId());
                map.put("date", simpleDateFormat.format(new Date()));
            }
        }
        return map;
    }

    public Map<String, Object> realUpload(FileForm form, MultipartFile multipartFile)throws Exception{
        //action指定了执行怎样的操作， check: 检验分片是否上传过； upload:直接上传分片
        String action = form.getAction();
        String fileId = form.getUuid();
        Integer index = Integer.valueOf(form.getIndex());
        String partMd5 = form.getPartMd5();
        String md5 = form.getMd5();
        Integer total = Integer.valueOf(form.getTotal());
        String fileName = form.getName();
        String size = form.getSize();
        String suffix = MyFileUtils.getSuffix(fileName);

        String saveDirectory = Constant.PATH + File.separator + fileId;
        String filePath = saveDirectory + File.separator + fileId + "." + suffix;
        //验证文件目录是否存在，不存在则创建目录
        File path = new File(saveDirectory);
        if(!path.exists()){
            path.mkdirs();
        }

        //文件分片位置
        File file = new File(saveDirectory, fileId + "_" + index);
        Map<String, Object> map = null;
        if(action.equals("check")){
            String md5Str = FileMd5Util.getFileMD5(file);
            if(md5Str != null && md5Str.length() == 31 ){
                System.out.println("check length =" + partMd5.length() + " md5Str length" + md5Str.length() + "   " + partMd5 + " " + md5Str);
                md5Str = "0" + md5Str;
            }
            if(md5Str != null && md5Str.equals(partMd5)){
                //分片已经上传过
                map = new HashMap<>();
                map.put("flag","1");
                map.put("fileId", fileId);
                if(index != total){
                    return map;
                }
            }else {
                //分片未上传
                map = new HashMap<>();
                map.put("flag", "0");
                map.put("fileId", fileId);
                return map;
            }
        }else if(action.equals("upload")){
            //分片上传过程中出错，有残余时需要先删除分块后，重新上传
            if(file.exists()){
                file.delete();
            }
            multipartFile.transferTo(new File(saveDirectory, fileId + "_" + index));

            //文件第一个分片上传时插入记录到数据库
            if(index == 1){
                UploadFile uploadFile = new UploadFile();
                uploadFile.setFileMd5(md5);
                String name = MyFileUtils.getFileName(fileName);
                if (name.length() > 50) {
                    name = name.substring(0, 50);
                }
                uploadFile.setFileName(name);
                uploadFile.setFileSuffix(suffix);
                uploadFile.setFileId(fileId);
                uploadFile.setFilePath(filePath);
                uploadFile.setFileSize(size);
                uploadFile.setFileStatus(1);

                uploadFileRespository.insert(uploadFile);
            }

            map = new HashMap<>();
            map.put("flag", "1");
            map.put("fileId", fileId);
        }
        return map;
    }

    public Map<String, Object> merge(String md5, int total)throws Exception{
        UploadFile uploadFile = uploadFileRespository.findFirstByFileMd5(md5);
        Map<String, Object> map = null;
        if(uploadFile == null){
            map = new HashMap<>();
            map.put("status", "0");
            return map;
        }else{
            String fileId = uploadFile.getFileId();
            String saveDirectory = Constant.PATH + File.separator + fileId;
            String suffix = uploadFile.getFileSuffix();
            File path = new File(saveDirectory);
            if(path.isDirectory()){
                File[] fileArray = path.listFiles();
                if(fileArray != null){
                    if(fileArray.length == total){
                        //分块全部上传完毕，合并
                        File newFile = new File(saveDirectory,fileId + "." + suffix);
                        FileOutputStream outputStream = new FileOutputStream(newFile, true); //文件追加模式
                        byte[] byt = new byte[10*1024*1024];
                        int len;
                        FileInputStream temp = null;
                        for(int i = 0; i < total; i++){
                            int j = i + 1;
                            temp = new FileInputStream(new File(saveDirectory, fileId + "_" + j));
                            while((len = temp.read(byt)) != -1){
                                outputStream.write(byt, 0, len);
                            }
                            temp.close();
                        }

                        outputStream.close();

                        //删除分片文件
                        for(int j = 0 ; j < total; j++){
                            int t = j + 1;
                            File file = new File(saveDirectory, fileId + "_" + t);
                            FileUtils.forceDelete(file);
                        }

                        //结束后更新数据库
                        uploadFile.setFileStatus(2);
                        uploadFileRespository.save(uploadFile);
                        map = new HashMap<>();
                        map.put("status", "1");
                        return map;
                    }else{
                        map = new HashMap<>();
                        map.put("status", "0");
                        return map;
                    }
                }
            }
            map = new HashMap<>();
            map.put("status", "0");
            return map;
        }
    }
}
