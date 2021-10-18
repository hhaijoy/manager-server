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

public interface RunTaskDao extends MongoRepository<RunTask, String> {
    RunTask findFirstById(String id);

    RunTask findFirstByRunTaskId(String runTaskId);

    // RunTask findFirstByMsrid(String msrid);

    RunTask findFirstByStatus(int status);

    List<RunTask> findAllByStatus(int status);

    List<RunTask> findAllByMd5(String md5);

    List<RunTask> findAllByMd5AndStatus(String md5, int status);

    List<RunTask> findAllByMd5AndStatusBetween(String md5, int from, int to);

    List<RunTask> findAllByMd5AndStatusOrStatus(String md5, int status1, int status2);
}
