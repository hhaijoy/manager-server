package com.example.demo.dto.taskNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by wang ming on 2019/2/21.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskNodeCalDTO {
    List<TaskNodeReceiveDTO> data;
}
