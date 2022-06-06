package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@Data
public class ControlCondition extends FlowElement implements Serializable {

    @JacksonXmlProperty(localName = "value", isAttribute = true)
    private String value;//待比较值

    @JacksonXmlProperty(localName = "format", isAttribute = true)
    private String format;

    @JacksonXmlProperty(localName = "true",isAttribute = true)
    private String trueAction;//actionId

    @JacksonXmlProperty(localName = "false",isAttribute = true)
    private String falseAction;//actionId

    @JacksonXmlProperty(localName = "Case")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<ConditionCase> conditionCases;

    private int status = 0;//1标识已经判断过
//    private int judgeResult = 0;//1标识已经判断过

    private Boolean judgeResult;//判断结果

//    public boolean getJudgeResult() {
//        return this.judgeResult;
//    }
}
