package com.example.demo.test.scene;

import com.example.demo.test.ResponseMsg;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by wang ming on 2019/2/22.
 */
@Component
@Data
public class TaskSet {

    private Set<DeferredResult<ResponseMsg<String>>> set = new HashSet<>();
}
