package com.example.demo.sdk;

import com.example.demo.utils.MyHttpUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by wang ming on 2019/4/12.
 */
public class DCData extends Service implements Data {

    private String id;

    public DCData(String ip, int port, String id) {
        super(ip,port);
        this.id = id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    @Override
    public String getURL() {
        String dataId = this.getId();

        String url = this.getBaseUrl() + "dataResource/getResource?sourceStoreId=" + dataId;

        return url;
    }

    @Override
    public int download(String filePath) throws IOException {
        String url = this.getURL();
        File file = MyHttpUtils.downloadFile(url,filePath);
        if(file.exists()){
            return 1;
        }else{
            return 0;
        }
    }
}
