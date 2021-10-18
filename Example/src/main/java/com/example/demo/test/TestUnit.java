package com.example.demo.test;

import com.example.demo.dao.RunTaskDao;
import com.example.demo.dao.SubmittedTaskDao;
import com.example.demo.entity.SubmittedTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * @Description
 * @Author bin
 * @Date 2021/07/24
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestUnit {
    @Autowired
    RunTaskDao runTaskDao;

    @Autowired
    SubmittedTaskDao submittedTaskDao;

    @Test
    public void test1(){
        List<SubmittedTask> allByMd5AndStatusBetween = submittedTaskDao.findAllByMd5AndStatusBetween("51c650cd6320c08b54a71a0efa7b7d8a", -1, 2);
        SubmittedTask submittedTask = submittedTaskDao.findFirstByStatus(0);
        List<SubmittedTask> allByMd5AndStatusBetween1 = submittedTaskDao.findAllByMd5AndStatusOrStatus("51c650cd6320c08b54a71a0efa7b7d8a", 0, 1);
        System.out.println("allByMd5AndStatusBetween:" + allByMd5AndStatusBetween);
    }
}
