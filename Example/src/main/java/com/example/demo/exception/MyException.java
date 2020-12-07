package com.example.demo.exception;

import com.example.demo.enums.ResultEnum;

/**
 * Created by wang ming on 2019/2/18.
 */
public class MyException extends RuntimeException {

    private Integer code;

    public MyException(ResultEnum resultEnum) {
        super(resultEnum.getMsg());
        this.code = resultEnum.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
