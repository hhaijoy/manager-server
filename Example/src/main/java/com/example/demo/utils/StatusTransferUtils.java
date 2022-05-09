package com.example.demo.utils;

/**
 * @Description 服务器之间状态的转换
 * @Author bin
 * @Date 2022/05/07
 */
public class StatusTransferUtils {

    //task server 的状态标识转换成 manager server的
    public static int TaskServer2ManagerServer(String status){

        int result = 0;

        switch (status){
            case "Started":{
                result = 1;
                break;
            }
            case "Finished":{
                result = 2;
                break;
            }
            case "Error":{
                result = -1;
                break;
            }
            default:
                result = 0;
        }

        return result;

    }

}
