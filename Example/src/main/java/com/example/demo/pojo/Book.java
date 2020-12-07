package com.example.demo.pojo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by wang ming on 2019/2/14.
 */

@ConfigurationProperties(prefix = "book")
@Component
@Data
public class Book {

    private String bookAuthor;
    private String bookName;

}
