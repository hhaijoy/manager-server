package com.example.demo.domain;

import com.example.demo.domain.support.GeoInfoMeta;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by wang ming on 2019/2/18.
 */
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "Task Server的节点相关信息")
public class TaskNode {

    @Id
    String id;

    @ApiModelProperty("节点名称")
    String name;
    @ApiModelProperty("IP")
    String host;
    @ApiModelProperty("端口")
    String port;
    @ApiModelProperty("操作系统")
    String system;
    @ApiModelProperty("注册时间")
    Date createDate;

    @ApiModelProperty("地理位置信息")
    GeoInfoMeta geoInfo;
    @ApiModelProperty("注册状态")
    String register;

}
