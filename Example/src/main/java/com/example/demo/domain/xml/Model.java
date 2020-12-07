package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @Author: wangming
 * @Date: 2019-11-15 19:44
 */
@Data
public class Model extends OgmsService implements Serializable {

    @JacksonXmlProperty(localName = "pid", isAttribute = true)
    private String pid;

    @JacksonXmlProperty(localName = "modelServiceUrl", isAttribute = true)
    private String modelServiceUrl;

}
