package com.example.demo.test.deferred;

import com.example.demo.test.ResponseMsg;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * 任务实体类
 * Created by wang ming on 2019/2/19.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    private int taskId;

    private DeferredResult<ResponseMsg<String>> taskResult;

    @Override
    public String toString(){
        return "Task{" + "taskId=" + taskId + ", taskResult" + "{responseMsg=" + taskResult.getResult() + "}" + "}";
    }

}
