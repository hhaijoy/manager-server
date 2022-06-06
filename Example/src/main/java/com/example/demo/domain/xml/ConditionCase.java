package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class ConditionCase  {

    private String orderId;

    @JacksonXmlProperty(localName = "opertator",isAttribute = true)
    private String opertator;//< > = != <= >=

    @JacksonXmlProperty(localName = "standard",isAttribute = true)
    private String standard;//判断标准

    @JacksonXmlProperty(localName = "relation",isAttribute = true)
    private String relation;//与顺序下一个的case的关系 and or

}
