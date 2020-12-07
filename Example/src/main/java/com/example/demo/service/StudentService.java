package com.example.demo.service;

import com.example.demo.dao.StudentDao;
import com.example.demo.pojo.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: wangming
 * @Date: 2019-08-07 17:27
 */
@Service
public class StudentService {

    @Autowired
    StudentDao studentDao;

    public void addStudent(Student student)throws Exception{
        studentDao.insert(student);
    }

    public void delete(String name){
        studentDao.deleteStudentByStudentName(name);
    }
}
