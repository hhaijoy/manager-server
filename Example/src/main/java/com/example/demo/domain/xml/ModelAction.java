package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class ModelAction extends Action implements Serializable {

    @JacksonXmlProperty(localName = "model", isAttribute = true)//对应model的md5值
    private String md5;

    @JacksonXmlProperty(localName = "doi", isAttribute = true)//对应model的doi
    private String doi;

    @JacksonXmlProperty(localName = "description", isAttribute = true)
    private String description;

    @JacksonXmlProperty(localName = "order", isAttribute = true)
    private String order;//同一个模型对应的两个action不同

    @JacksonXmlProperty(localName = "step", isAttribute = true)
    private int step ;


}
