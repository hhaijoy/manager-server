package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * @ClassName IterationBodyElement.java
 * @Author wzh
 * @Version 1.0.0
 * @Description
 * @CreateDate(Y/M/D-H:M:S) 2022/03/11/ 00:19:00
 */
@Data
public class IterationElement {
    @JacksonXmlProperty(localName = "index", isAttribute = true)
    private String index;//对应元素的id

    @JacksonXmlProperty(localName = "type", isAttribute = true)
    private String type;//对应元素的类型 -- modelAction, dataProcessing, condition
}
