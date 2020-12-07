package com.example.demo.dao;

import com.example.demo.pojo.User;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by wang ming on 2019/2/15.
 */
public interface UserDao extends MongoRepository<User,String> {
}
