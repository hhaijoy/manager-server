package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.awt.*;

@Data
public class DataLink {
    @JacksonXmlProperty(localName = "from", isAttribute = true)
    private String from;

    @JacksonXmlProperty(localName = "to", isAttribute = true)
    private String to;

//    @JacksonXmlProperty(localName = "source", isAttribute = true)
//    private String source;
//
//    @JacksonXmlProperty(localName = "target", isAttribute = true)
//    private String target;

//    @JacksonXmlProperty(localName = "tool", isAttribute = true)
//    private String tool;

}
