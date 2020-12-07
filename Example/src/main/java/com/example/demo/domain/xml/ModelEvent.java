package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class ModelEvent {

    @JacksonXmlProperty(localName = "dataId", isAttribute = true)
    private String dataId;
}
