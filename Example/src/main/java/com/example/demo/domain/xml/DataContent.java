package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class DataContent implements Serializable {

    @JacksonXmlProperty(localName = "type", isAttribute = true)
    private String type;//url link param insituData

    @JacksonXmlProperty(localName = "value", isAttribute = true)
    private String value;//根据type的值

    @JacksonXmlProperty(localName = "link", isAttribute = true)
    private String link;//根据type的值

    private String fileName;

    private String suffix;

}
