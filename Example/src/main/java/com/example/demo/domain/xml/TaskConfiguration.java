package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.LinkedList;

/**
 * @Author: wangming
 * @Date: 2019-11-15 19:41
 */
@Data
@JacksonXmlRootElement(localName = "TaskConfiguration")
public class TaskConfiguration {

    @JacksonXmlProperty(localName = "uid", isAttribute = true)
    private String uid;

    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name;

    @JacksonXmlProperty(localName = "version" ,isAttribute = true)
    private String version;

    @JacksonXmlProperty(localName = "Model")
    @JacksonXmlElementWrapper(localName = "Models")
    private LinkedList<Model> models;

    @JacksonXmlProperty(localName = "ProcessingTool")
    @JacksonXmlElementWrapper(localName = "ProcessingTools")
    private LinkedList<ProcessingTool> processingTools;

    @JacksonXmlProperty(localName = "ModelAction")
    @JacksonXmlElementWrapper(localName = "ModelActions")
    private LinkedList<ModelAction> modelActions;

    @JacksonXmlProperty(localName = "DataProcessing")
    @JacksonXmlElementWrapper(localName = "DataProcessings")
    private LinkedList<DataProcessing> dataProcessings;

    @JacksonXmlProperty(localName = "Condition")
    @JacksonXmlElementWrapper(localName = "Conditions")
    private LinkedList<ControlCondition> conditions;

    @JacksonXmlProperty(localName = "DataLink")
    @JacksonXmlElementWrapper(localName = "DataLinks")
    private LinkedList<DataLink> dataLinks;

    @JacksonXmlProperty(localName = "Iteration")
    @JacksonXmlElementWrapper(localName = "Iterations")
    private LinkedList<Iteration> iterations;


    @Id
    private String id;

}
