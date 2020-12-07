package com.example.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @Author: wangming
 * @Date: 2019-11-05 20:49
 */
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelService {

    @Id
    String id;
    String ms_model;
    String mv_num;
    String ms_des;
    String ms_platform;
}
