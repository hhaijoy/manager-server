package com.example.demo.domain.Scheduler;

import com.example.demo.domain.xml.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author: wangming
 * @Date: 2019-11-15 14:37
 */
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
    @Id
    private String id;
    private String uid;//前台赋值的uid
    private String taskId;
    private String name;
    private String version;
    private List<Model> models;
    private List<ModelAction> modelActions;
    private List<ModelAction> i_modelActions;//标识要迭代的模型步骤
    private List<DataProcessing> dataProcessings;
    private List<ControlCondition> controlConditions;

    private List<Iteration> iterations;

    private int status; // 0-started, 1 - finished, -1 - failed

    private Map<String,List<DataLink>> dataLinks;//k:目的数据

    private Map<String, List<DataLink>> reDataLinks;//k:出发地数据

    private Map<String,Integer> iterationCount;

    private Date date;
    private String userName;
    private Date finish;
}
