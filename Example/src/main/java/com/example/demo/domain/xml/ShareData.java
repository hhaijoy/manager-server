package com.example.demo.domain.xml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private String value;

    private String type;
}
