package com.example.demo.dto.computableModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by wang ming on 2019/3/22.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultDTO {
    String ip;
    int port;
    String tid;
    String pid;
    int status; // Inited: 0, Started: 1; Finished: 2, Error: -1
    List<ExDataDTO> inputs;
    List<ExDataDTO> outputs;

}
