package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

/**
 * @ClassName Iteration.java
 * 用于表达流程中的循环
 * @Author wzh
 * @Version 1.0.0
 * @Description
 * @CreateDate(Y/M/D-H:M:S) 2022/03/10/ 22:33:00
 */
@Data
public class Iteration {
    @JacksonXmlProperty(localName = "id", isAttribute = true)
    private String id;

    @JacksonXmlProperty(localName = "type", isAttribute = true)
    private String type;//til--直到型循环，while--当型循环，times--指定次数型

    @JacksonXmlProperty(localName = "times", isAttribute = true)
    private String times;//指定次数时的循环次数

    @JacksonXmlProperty(localName = "Condition")
    private IterationElement condition;//循环体

    //条件判断为何结果时，开启/结束循环，和type、condition共同控制循环起止，
    // 以条件判断指向循环体内的关系为准
    private Boolean controlCase;

    @JacksonXmlElementWrapper(localName = "Body")
    @JacksonXmlProperty(localName = "Element")
    private List<IterationElement> elements;//循环体

    private List<String> contents;//循环中的所有元素，包括条件和循环体

    private int round;//循环轮数，全部执行完毕修改

    private int status;//0--未执行，1--执行中，2--执行完成


}
