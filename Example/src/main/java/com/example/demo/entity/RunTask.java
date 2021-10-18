package com.example.demo.entity;

import com.example.demo.dto.computableModel.ExDataDTO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Author bin
 * @Date 2021/07/23
 */
@Document("RunTask")
@Data
public class RunTask {

    @Id
    private String id;

    private String runTaskId;

    private String md5;
    // private List<String> relatedTasks = new ArrayList<>();//保存对应提交任务的id

    private String ip;
    private int port;
    // private String mid;//模型容器中的模型对应id

    private List<ExDataDTO> inputs;
    private List<ExDataDTO> outputs;

    // private String msrid;
    // 模型状态 0:未运行 1:正在运行 2:运行完成 -1:运行失败
    private int status;
}
