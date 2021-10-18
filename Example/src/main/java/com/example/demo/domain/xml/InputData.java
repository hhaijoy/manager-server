package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @Author: wangming
 * @Date: 2019-11-15 21:24
 */
@Data
public class InputData implements Serializable {
    @JacksonXmlProperty(localName = "DataConfiguration")
    @JacksonXmlElementWrapper(useWrapping = false)
    private ArrayList<DataTemplate> inputs;

    @JacksonXmlProperty(localName = "Parameter")
    @JacksonXmlElementWrapper(useWrapping = false)
    private ArrayList<ActionParam> params;
}
