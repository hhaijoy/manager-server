package com.example.demo.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @Description
 * @Author bin
 * @Date 2021/07/23
 */
@Data
public class DataItem {
    @JSONField(name = "StateId")
    private String StateId;
    @JSONField(name = "StateName")
    private String StateName;
    @JSONField(name = "Event")
    private String Event;
    @JSONField(name = "DataId")
    private String DataId;
    @JSONField(name = "Destoryed")
    private boolean Destoryed = false;
}