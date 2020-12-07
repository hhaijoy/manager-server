package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by wang ming on 2019/4/23.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileFinishForm {

    private String md5;

    private int total;
}
