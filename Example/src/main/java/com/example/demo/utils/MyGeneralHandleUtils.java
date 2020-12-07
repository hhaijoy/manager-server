package com.example.demo.utils;

import java.io.*;
import java.util.List;


/**
 * wzh
 * 2020.09.04
 * 通用处理工具
 */
public class MyGeneralHandleUtils {

    /**
     * wzh 2020.09.04
     * list序列化深拷贝
     * @param srcList
     * @param <T>
     * @return
     */
    public static <T> List<T> deepCopy(List<T> srcList) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(srcList);

            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
            ObjectInputStream inStream = new ObjectInputStream(byteIn);
            List<T> destList = (List<T>) inStream.readObject();
            return destList;
        } catch (IOException e) {
            e.printStackTrace();//读取Object流信息失败
        } catch (ClassNotFoundException e) {
            e.printStackTrace();//泛型类不存在
        }
        return null;
    }

}
