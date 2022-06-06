package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class Action extends FlowElement implements Serializable {


    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name;

    @JacksonXmlProperty(localName = "Outputs")
    private OutputData outputData;

    @JacksonXmlProperty(localName = "Inputs")
    private InputData inputData;

    @JacksonXmlProperty(localName = "iterationType", isAttribute = true)
    private int iterationType = 1;//1--不循环，2--指定次数，3--根据条件

    @JacksonXmlProperty(localName = "iterationNum", isAttribute = true)
    private int iterationNum ;

    private int status = 0; // 0代表未开始，-1代表运行失败，1代表运行成功, 2代表运行超时(不存在运行中状态，省略)/缺少数据

//    private String taskIpAndPort;


    private String taskId;

    private String taskIp;

    private int port;

    private String condition;//与这个action相关的条件判断

    private int runTimes=0;//容错处理
//    private int runNum = 0;

//    private int sucNum = 0;

}
