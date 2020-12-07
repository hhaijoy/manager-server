package com.example.demo.dto.computableModel;

import com.example.demo.domain.support.TemplateInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: wangming
 * @Date: 2020-01-05 10:36
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutputDataDTO {
    String statename;
    String event;
    TemplateInfo template;
}
