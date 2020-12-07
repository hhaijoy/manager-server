package com.example.demo.service.common;

/**
 * Created by wang ming on 2019/2/15.
 */
public interface BaseService<E,AD,UD,UID,FD> extends
        CreateService<AD>,
        QueryService<E,FD,UID>,
        DeleteService<UID>,
        UpdateService<UID,UD>{
}
