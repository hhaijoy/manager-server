package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @Author: wangming
 * @Date: 2019-08-07 17:01
 */
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {


    @Indexed(unique = true)
    private String courseName;

    @DBRef
    private Student student;
}
