package com.example.demo.dto.taskNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by wang ming on 2019/2/18.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskNodeAddDTO {
    String name;
    String host;
    String port;
    String system;
}
