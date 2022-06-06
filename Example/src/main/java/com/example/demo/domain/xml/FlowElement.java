package com.example.demo.domain.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * @ClassName FlowElement.java
 * @Author wzh
 * @Version 1.0.0
 * @Description
 * @CreateDate(Y/M/D-H:M:S) 2022/03/10/ 23:56:00
 */
@Data
public class FlowElement {
    @JacksonXmlProperty(localName = "id", isAttribute = true)
    private String id;

    private String iteration;//表示该元素在哪一个循环中
    private int round;//循环执行轮数，执行完毕才修改
}
