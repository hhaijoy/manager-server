package com.example.demo.utils;

import com.example.demo.domain.xml.TaskConfiguration;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
//import com.sun.beans.decoder.DocumentHandler;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

//import javax.swing.text.Document;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;

/**
 * @Author: wangming
 * @Date: 2019-11-15 20:10
 */
public class XmlParseUtils {

    /**
     * 根据文件路径解析xml文件
     * @param filePath
     * @return com.example.demo.domain.xml.TaskConfiguration
     * @author wangming
     * @date 2019/11/15 20:12
     */
    public static TaskConfiguration parseXml(String filePath, String encode){
        File f = new File(filePath);
        if(!f.exists()){
            return null;
        }
        TaskConfiguration taskConfiguration = null;
        XmlMapper xmlMapper = new XmlMapper();
        //自动忽略无法对应pojo的字段
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        xmlMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        xmlMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
        xmlMapper.enable(MapperFeature.USE_STD_BEAN_NAMING);
        try {
            String conent = FileUtils.readFileToString(f, encode);
            taskConfiguration = xmlMapper.readValue(conent,TaskConfiguration.class);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            return taskConfiguration;
        }
    }

    public static TaskConfiguration parseXmlBaseOnStream(InputStream inputStream, String encoding){
        TaskConfiguration taskConfiguration = null;
        XmlMapper xmlMapper = new XmlMapper();
        //自动忽略无法对应pojo的字段
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        xmlMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        xmlMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
        xmlMapper.enable(MapperFeature.USE_STD_BEAN_NAMING);
        try {
            String conent = IOUtils.toString(inputStream,encoding);
            taskConfiguration = xmlMapper.readValue(conent,TaskConfiguration.class);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            return taskConfiguration;
        }



    }

    public static String getNodeContent(String xml, String nodeTag) throws DocumentException {
        Document doc = null;
        doc = DocumentHelper.parseText(xml);
        Element root = doc.getRootElement();
        String result = root.elementText(nodeTag);

        return result;

    }
}
