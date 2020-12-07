package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by wang ming on 2019/1/18.
 */
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    String userName;
    String password;
    Date createDate;
}
