package com.example.demo.sdk;

import java.io.IOException;

/**
 * Created by wang ming on 2019/4/12.
 */
public interface Data {

    public String getURL();

    public int download(String filePath)throws IOException;
}
