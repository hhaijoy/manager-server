package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: wangming
 * @Date: 2019-11-15 19:46
 */
@Data
public class DataTemplate implements Serializable {

    @JacksonXmlProperty(localName = "state", isAttribute = true)
    private String state;

    @JacksonXmlProperty(localName = "event", isAttribute = true)
    private String event;

    private String link;

    @JacksonXmlProperty(localName = "id", isAttribute = true)
    private String dataId;

    @JacksonXmlProperty(localName = "Data")
    private DataContent dataContent = new DataContent();//输入输出的内容

    private String[] mutiFile;
    //标记这个数据是否已经准备好,设置默认值
    private boolean isPrepared = false;
}
