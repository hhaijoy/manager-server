package com.example.demo.dto.taskNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by wang ming on 2019/2/20.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskNodeReceiveDTO {
    String id;
    String host;
    String port;
    int time = 0;
}
