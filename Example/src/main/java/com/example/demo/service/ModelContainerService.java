package com.example.demo.service;

import com.example.demo.dao.ModelContainerDao;
import com.example.demo.domain.ModelContainer;
import com.example.demo.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 模型服务容器版本管理
 * Created by wang ming on 2019/4/22.
 */
@Service
public class ModelContainerService {

    @Autowired
    ModelContainerDao modelContainerDao;

    @Value("${prop.upload-folder}")
    private String UPLOAD_FOLDER;

    public String findFilePathById(String id){
        ModelContainer modelContainer = modelContainerDao.findById(id).orElse(null);
        return (modelContainer == null) ? null : modelContainer.getPath();
    }

    public ModelContainer uploadModelContainer(MultipartFile file, String version){
        //获取文件名称与后缀
        String fileName = file.getOriginalFilename();
        //String filePath = ClassUtils.getDefaultClassLoader().getResource("").getPath() + "static/upload/" + UUID.randomUUID();
        //构建文件目录及文件名
        String path = UUID.randomUUID() + File.separator + fileName;
        String store_filePath = UPLOAD_FOLDER + path;
        File dest = new File(store_filePath);
        //检查文件目录是否存在
        if(!dest.getParentFile().exists()){
            dest.getParentFile().mkdirs();
        }
        try{
            file.transferTo(dest);
        }catch (IOException e){
            throw new RuntimeException(e.getMessage());
        }
        //数据库存储
        ModelContainer modelContainer = new ModelContainer();
        modelContainer.setVersion(version);
        modelContainer.setPath(path);
        modelContainer.setCreateDate(new Date());
        return modelContainerDao.insert(modelContainer);
    }

    public ModelContainer getTheNewestRecord(){
        List<ModelContainer> modelContainerList = modelContainerDao.findAll();
        modelContainerList.sort(new Comparator<ModelContainer>() {
            @Override
            public int compare(ModelContainer o1, ModelContainer o2) {
                return StringUtils.compareAppVersion(o1.getVersion(), o2.getVersion());
            }
        }.reversed());
        return modelContainerList.get(0);
    }



}
