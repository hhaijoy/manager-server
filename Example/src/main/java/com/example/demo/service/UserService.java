package com.example.demo.service;

import com.example.demo.dao.UserDao;
import com.example.demo.pojo.User;
import com.example.demo.pojo.UserAddDto;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Created by wang ming on 2019/2/15.
 */
@Service
public class UserService {

    @Autowired
    UserDao userDao;

    public void addUser(UserAddDto user)throws Exception{
        User user1 = new User();
        BeanUtils.copyProperties(user,user1);
        user1.setCreateDate(new Date());
        userDao.insert(user1);
    }
}
