package com.example.demo.controller;

import com.example.demo.pojo.UserAddDto;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by wang ming on 2019/2/15.
 */
@Controller
public class UserController {

    @Autowired
    UserService userService;

    @ResponseBody
    @RequestMapping(value = "/user",method = RequestMethod.POST)
    public UserAddDto addUser(UserAddDto user) throws Exception {
        userService.addUser(user);
        return user;
    }
}
