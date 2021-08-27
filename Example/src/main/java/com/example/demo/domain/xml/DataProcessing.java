package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class DataProcessing extends Action implements Serializable {

    @JacksonXmlProperty(localName = "type", isAttribute = true)
    private String type;//modelService,dataService

    @JacksonXmlProperty(localName = "token", isAttribute = true)
    private String token;//对应data service的计算节点

    @JacksonXmlProperty(localName = "service", isAttribute = true)
    private String service;//对应model service的md5或data service的标识

    @JacksonXmlProperty(localName = "method", isAttribute = true)
    private String method;//对应数据服务的处理/可视化服务

    private String remark;//因为dataservice不想加状态接口，只能这么搞

    private int runTimes=0;//因为dataservice不想加状态接口，只能这么搞

}
