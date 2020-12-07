package com.example.demo.dto.computableModel;

import com.alibaba.fastjson.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by wang ming on 2019/3/21.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExDataDTO {
    String statename;
    String event;
    String url;
    String tag;
    String suffix;
    JSONArray urls;
}
