package com.example.demo.sdk;

import com.example.demo.utils.MyFileUtils;
import com.example.demo.utils.MyHttpUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by wang ming on 2019/3/20.
 */
public class ExData extends Service implements Data {

    private String id;
    private String pwd;

    public ExData(String ip, int port, String id, String pwd) {
        super(ip, port);
        this.id = id;
        this.pwd = pwd;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    @Override
    public String getURL(){
        String dataId = this.getId();
        String pwd_c = "";
        if(!this.getPwd().equals("")){
            try {
                pwd_c = MyFileUtils.encryption(this.getPwd());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        }
        String url = this.getBaseUrl() + "data/" + dataId + "?pwd=" + pwd_c;
        return url;
    }

    @Override
    public int download(String filePath) throws IOException {
        String url = this.getURL();
        File file = MyHttpUtils.downloadFile(url,filePath);
        if(file.exists()){
            return 1;
        }else {
            return 0;
        }
    }


}
