package com.example.demo.utils;

import com.example.demo.bean.JsonResult;
import com.example.demo.enums.ResultEnum;

/**
 * Created by wang ming on 2019/2/18.
 */
public class ResultUtils {

    public static JsonResult success(){
        return success(null);
    }

    public static JsonResult success(Object obj) {
        JsonResult jsonResult = new JsonResult();
        jsonResult.setMsg(ResultEnum.SUCCESS.getMsg());
        jsonResult.setCode(ResultEnum.SUCCESS.getCode());
        jsonResult.setData(obj);
        return jsonResult;
    }

    public static JsonResult error(Integer code, String msg){
        JsonResult jsonResult = new JsonResult();
        jsonResult.setCode(code);
        jsonResult.setMsg(msg);
        jsonResult.setData(null);
        return jsonResult;
    }
}
