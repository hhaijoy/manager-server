package com.example.demo.service.common;

/**
 * Created by wang ming on 2019/2/15.
 */
public interface UpdateService<UID,UD> {
    void update(UID id, UD updateDTO);
}
