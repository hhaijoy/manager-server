package com.example.demo.dao;

import com.example.demo.domain.TaskNode;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by wang ming on 2019/2/18.
 */
public interface TaskNodeDao extends MongoRepository<TaskNode,String> {

    TaskNode findFirstByHostAndPort(String host, String port);

}
