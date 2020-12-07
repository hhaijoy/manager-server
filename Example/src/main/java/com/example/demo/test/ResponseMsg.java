package com.example.demo.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by wang ming on 2019/2/21.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMsg<T> {
    int code;
    String msg;
    T data;
}
