package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by wang ming on 2019/4/23.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileForm {
    private String md5;

    private String uuid;

    private String date;

    private String name;

    private String size;

    private String total;

    private String index;

    private String action;

    private String partMd5;
}
