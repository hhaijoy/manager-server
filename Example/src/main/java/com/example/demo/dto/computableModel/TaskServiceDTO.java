package com.example.demo.dto.computableModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by wang ming on 2019/3/20.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskServiceDTO {
    String ip;
    int port;
    String pid;
    String username;
    String ex_ip;
    int ex_port;
    int type;
    List<ExDataDTO> inputs;
    List<OutputDataDTO> outputs;
}
