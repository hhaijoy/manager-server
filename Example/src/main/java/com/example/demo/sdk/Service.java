package com.example.demo.sdk;

import com.example.demo.utils.MyHttpUtils;
import com.example.demo.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by wang ming on 2019/3/20.
 */
public class Service {

    private String ip;
    private int port;

    public Service(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public Service() {
        this.ip = "127.0.0.1";
        this.port = 8060;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    protected String getBaseUrl(){
        StringBuilder stringBuilder = new StringBuilder("");
        stringBuilder.append("http://").append(this.ip).append(":").append(this.port).append("/");
        return stringBuilder.toString();
    }

    public int connect() throws IOException, URISyntaxException {
        String url = this.getBaseUrl();
        url = url + "ping";
        String body = MyHttpUtils.GET(url,"UTF-8",null);
        String content = StringUtils.replaceBlank(body);
        // JSON Object
        if(content.equals("OK")){
            return 1;
        }
        return 0;
    }
}
