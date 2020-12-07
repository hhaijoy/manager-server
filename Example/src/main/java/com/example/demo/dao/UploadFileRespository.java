package com.example.demo.dao;

import com.example.demo.domain.UploadFile;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by wang ming on 2019/4/23.
 */
public interface UploadFileRespository extends MongoRepository<UploadFile, String> {
    UploadFile findFirstByFileMd5(String fileMd5);

    void deleteByFileMd5(String fileMd5);
}
