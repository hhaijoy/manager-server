package com.example.demo.dao;

import com.example.demo.pojo.Student;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StudentDao extends MongoRepository<Student, String> {

    public void deleteStudentByStudentName(String name);
}
