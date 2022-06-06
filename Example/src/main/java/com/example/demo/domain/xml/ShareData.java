package com.example.demo.domain.xml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * @Author: wangming
 * @Date: 2019-11-15 22:18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShareData {//保存数据详情

    private String actionId;

    private String dataId;

    private List<String> values; //输出的值，可能是多输出文件

    private String exception; //出错标记

    private String type;


}
