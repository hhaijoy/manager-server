package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Zihuan
 * 2020.10.23
 */
@Data
public class ConvertTool implements Serializable {

    @JacksonXmlProperty(localName = "id", isAttribute = true)
    private String id;

    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name;

    @JacksonXmlProperty(localName = "type", isAttribute = true)
    private String type;//service、api

    @JacksonXmlProperty(localName = "source", isAttribute = true)
    private String source;//internal、external

    @JacksonXmlProperty(localName = "description", isAttribute = true)
    private String description;

    @JacksonXmlProperty(localName = "param", isAttribute = true)
    private String param;
}
