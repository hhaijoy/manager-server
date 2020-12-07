package com.example.demo.domain.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by wang ming on 2019/2/21.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskNodeStatusInfo {
    String id;
    String host;
    String port;
    boolean status;
    int running;

}
