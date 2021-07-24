package com.example.demo.entity;

import com.example.demo.dto.computableModel.ExDataDTO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * @Description
 * @Author bin
 * @Date 2021/07/23
 */
@Document
@Data
public class SubmittedTask {

    @Id
    private String id;

    private String ip;
    private int port;

    private String submittedTaskId;
    private String taskName;
    private String userName;

    // private String model;
    private String md5;

    private String runTaskId;//保存对应的runtask id便于用户查询使用
    private int queueNum;
    private Date runTime;
    // 模型状态 0:未运行 1:正在运行 2:运行完成 -1:运行失败
    private int status;
    private List<ExDataDTO> inputs;
    private List<ExDataDTO> outputs;
}
