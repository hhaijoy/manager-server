package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class ProcessingTool extends OgmsService implements Serializable {

    @JacksonXmlProperty(localName = "type", isAttribute = true)
    private String type;

    @JacksonXmlProperty(localName = "source", isAttribute = true)
    private String source;

    @JacksonXmlProperty(localName = "service", isAttribute = true)
    private String service;

    @JacksonXmlProperty(localName = "param", isAttribute = true)
    private String param;//对应的调用说明

}
