package com.example.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by wang ming on 2019/2/26.
 */
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComputableModel {
    @Id
    String id;
    String pid; //唯一标识计算模型服务id
    String name; //唯一标识计算模型名称
    Date createTime;
}
