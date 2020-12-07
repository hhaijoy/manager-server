package com.example.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by wang ming on 2019/4/23.
 */
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadFile {
    /* uuid */
    @Id
    private String fileId;

    /* 文件路径 */
    private String filePath;

    /* 文件大小 */
    private String fileSize;

    /* 文件后缀 */
    private String fileSuffix;

    /* 文件名字 */
    private String fileName;

    /* 文件md5 */
    private String fileMd5;

    /* 文件上传状态 */
    private Integer fileStatus;

    private Date createTime;

    private Date updateTime;
}
