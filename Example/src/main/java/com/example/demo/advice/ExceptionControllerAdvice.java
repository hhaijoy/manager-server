package com.example.demo.advice;

import com.example.demo.bean.JsonResult;
import com.example.demo.exception.MyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import com.example.demo.utils.ResultUtils;

/**
 * Created by wang ming on 2019/2/18.
 */
@ControllerAdvice
@ResponseBody
public class ExceptionControllerAdvice {

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<JsonResult> defalutErrorHandler(Exception e){
        //自定义异常
        if(e instanceof MyException){
            MyException myException = (MyException)e;
            return ResponseEntity.status(HttpStatus.OK).body(ResultUtils.error(myException.getCode(),myException.getMessage()));
        }else{
            //未定义的其他异常，表现为服务器内部的异常
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResultUtils.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),e.getMessage()));
        }
    }
}
