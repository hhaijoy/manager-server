package com.example.demo.service.common;

import org.springframework.data.domain.Page;

/**
 * Created by wang ming on 2019/2/15.
 */
public interface QueryService<E,FD,UID> {
    Page<E> list(FD findDTO);

    E get(UID uid);
}
