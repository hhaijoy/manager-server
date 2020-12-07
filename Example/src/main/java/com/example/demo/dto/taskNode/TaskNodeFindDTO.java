package com.example.demo.dto.taskNode;

import lombok.Data;

/**
 * Created by wang ming on 2019/2/20.
 */
@Data
public class TaskNodeFindDTO {
    private Integer page = 1;
    private Integer pageSize = 10;
    private Boolean asc = false; //降序
}
