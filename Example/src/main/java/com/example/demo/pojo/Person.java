package com.example.demo.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by wang ming on 2019/1/19.
 */
@Data
public class Person {

    private String lastName;
    private Integer age;
    private Boolean boss;
    private Date birth;
    private Map<String, Object> maps;
    private List<String> lists;
    private Dog dog;
}
