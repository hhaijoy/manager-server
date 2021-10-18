package com.example.demo.dao;

import com.example.demo.entity.RunTask;
import com.example.demo.entity.SubmittedTask;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * @Description
 * @Author bin
 * @Date 2021/07/23
 */
public interface SubmittedTaskDao extends MongoRepository<SubmittedTask, String> {
    SubmittedTask findBySubmittedTaskId(String submittedTaskId);

    SubmittedTask findFirstBySubmittedTaskId(String submittedTaskId);

    SubmittedTask findFirstByStatus(int status);

    List<SubmittedTask> findAllByMd5AndStatusBetween(String md5, int from, int to);

    List<SubmittedTask> findAllByMd5AndStatusOrStatus(String md5, int from, int to);
}
