package com.example.demo.controller;

import com.example.demo.DemoApplication;
import com.example.demo.bean.JsonResult;
import com.example.demo.domain.Scheduler.Task;
import com.example.demo.pojo.Book;
import com.example.demo.service.AsyncService;
import com.example.demo.utils.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by wang ming on 2019/1/18.
 */
@RestController
@Api(value = "用户模块")
public class HelloWorld {

    public static Logger log = LoggerFactory.getLogger(HelloWorld.class);

    @Autowired
    private Book book;

    @Autowired
    private AsyncService asyncService;


    @GetMapping("/hello")
    @ApiOperation(value = "Test Swagger")
    public String index(){
        return "Hello World0.1.4";
    }

    @GetMapping("/book")
    @ApiOperation(value = "Test book")
    public String book(){
        log.info(book.toString());
        return "hello SpringBoot-swagger";
    }

    @RequestMapping(value = "test3",method = RequestMethod.GET)
    public String testAsyncNoReturn() throws Exception {
        long start = System.currentTimeMillis();
        asyncService.doNoReturn();
        return String.format("任务执行成功，耗时{%s}毫秒", System.currentTimeMillis() - start);
    }

    @RequestMapping(value = "test",method = RequestMethod.GET)
    public JsonResult test(@RequestParam("name") String name){
        LinkedBlockingDeque<Task> linkedBlockingDeque = DemoApplication.linkedBlockingDeque;
        Task task = new Task();
        try {
            linkedBlockingDeque.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ResultUtils.success("任务添加成功");
    }


}
