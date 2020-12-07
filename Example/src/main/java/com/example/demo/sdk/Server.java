package com.example.demo.sdk;

/**
 * Created by wang ming on 2019/3/20.
 */
public class Server extends Service {

    public Server(String ip, int port) {
        super(ip, port);
    }

    public int setIPAndPort(String ip, int port){
        this.setIp(ip);

        if(port > 65535 || port < 0){
            return -1;
        }

        this.setPort(port);
        return 1;
    }

}
