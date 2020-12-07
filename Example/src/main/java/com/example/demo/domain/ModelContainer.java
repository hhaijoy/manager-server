package com.example.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by wang ming on 2019/4/22.
 */
@Data
@Document
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelContainer {

    @Id
    String id;
    String version;
    String path;
    Date createDate;
}
