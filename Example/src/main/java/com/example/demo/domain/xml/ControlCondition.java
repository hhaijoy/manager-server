package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class ControlCondition {

    @JacksonXmlProperty(localName = "id", isAttribute = true)
    private String id;

    @JacksonXmlProperty(localName = "value", isAttribute = true)
    private String value;//待比较值

    @JacksonXmlProperty(localName = "format", isAttribute = true)
    private String format;

    @JacksonXmlProperty(localName = "true",isAttribute = true)
    private String trueAction;//actionId

    @JacksonXmlProperty(localName = "false",isAttribute = true)
    private String falseAction;//actionId

    @JacksonXmlProperty(localName = "When")
    @JacksonXmlElementWrapper(localName = "Case")
    private List<ConditionCase> conditionCases;

    private int status = 0;//1标识已经判断过
}
