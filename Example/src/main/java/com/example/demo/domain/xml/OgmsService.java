package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * @Author: wzh
 * @Date: 2020.11.03
 */
@Data
public class OgmsService {

    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name;

    @JacksonXmlProperty(localName = "description", isAttribute = true)
    private String description;

}
