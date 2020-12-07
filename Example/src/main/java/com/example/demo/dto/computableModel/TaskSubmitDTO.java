package com.example.demo.dto.computableModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @Author: wangming
 * @Date: 2019-11-15 23:50
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmitDTO {
    @NotNull
    String pid;
    @NotNull
    String userName;
    @NotNull
    List<ExDataDTO> inputs;

    List<OutputDataDTO> outputs;
}
