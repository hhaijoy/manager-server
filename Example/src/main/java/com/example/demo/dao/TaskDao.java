package com.example.demo.dao;

import com.example.demo.domain.Scheduler.Task;
import com.example.demo.domain.xml.TaskConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @Author: wangming
 * @Date: 2019-11-15 20:30
 */
public interface TaskDao extends MongoRepository<Task, String> {

    public Task findTaskByTaskId(String taskId);
}
