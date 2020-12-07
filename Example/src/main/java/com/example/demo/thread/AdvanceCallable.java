package com.example.demo.thread;

import com.example.demo.domain.xml.Model;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * @Author: wangming
 * @Date: 2019-11-15 22:31
 */
public class AdvanceCallable implements Callable<Model> {

    private AdvanceHandler advanceHandler;

    private int index;

    public AdvanceCallable(AdvanceHandler advanceHandler, int index) {
        this.advanceHandler = advanceHandler;
        this.index = index;
    }

    @Override
    public Model call() throws Exception {
        return this.advanceHandler.runModel(index);
    }
}
