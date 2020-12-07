package com.example.demo.thread;

import com.example.demo.domain.Scheduler.Task;
import com.example.demo.domain.xml.DataProcessing;
import com.example.demo.domain.xml.Model;
import com.example.demo.domain.xml.ModelAction;
import com.example.demo.utils.TaskLoop;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskLoopHandler implements Runnable {

    private Task task;

    public TaskLoopHandler(Task task){
        this.task = task;
    }

    @Override
    public void run() {
        TaskLoop taskLoop = new TaskLoop(task.getUserName());

        try {
            taskLoop.initTaskRun(task);

            List<ModelAction> waitingModels = ((Map<String,List<ModelAction>>)taskLoop.checkActions(task).get("model")).get("waiting");
            List<DataProcessing> waitingProcessings = ((Map<String,List<DataProcessing>>)taskLoop.checkActions(task).get("processing")).get("waiting");


            while (true){
                taskLoop.query(waitingModels,waitingProcessings,task);

                Map<String, Object> checkedList = taskLoop.checkActions(task);

                waitingModels = ((Map<String,List<ModelAction>>)taskLoop.checkActions(task).get("model")).get("waiting");
                waitingProcessings = ((Map<String,List<DataProcessing>>)taskLoop.checkActions(task).get("processing")).get("waiting");

                if(taskLoop.finalCheck(task)){
                    System.out.println("全部运行结束");
                    break;
                }
            }
            System.out.println("线程结束");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


    }

}
