package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class DataContent implements Serializable {

    @JacksonXmlProperty(localName = "type", isAttribute = true)
    private String type;//url link param insituData
    // mixed--循环中来自用户配置和内部输出两种 multiLink--循环中来自外部和内部输出两种 iteLink--来着循环内部

    @JacksonXmlProperty(localName = "value", isAttribute = true)
    private String value;//根据type的值

    @JacksonXmlProperty(localName = "link", isAttribute = true)
    private String link;//根据type的值

    private String iteLink; //记录循环内部连接

    private String outLink; //记录循环外部连接

    private String fileName;

    private String suffix;

}
