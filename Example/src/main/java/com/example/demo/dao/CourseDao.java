package com.example.demo.dao;

import com.example.demo.pojo.Course;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CourseDao extends MongoRepository<Course, String> {

}
