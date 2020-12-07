package com.example.demo.utils;

import io.swagger.models.auth.In;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;

/**
 * 文件操作工具类
 * Created by wang ming on 2019/3/7.
 */
public class MyFileUtils {


    public static String getSuffix(String filePath){
        if(StringUtils.INDEX_NOT_FOUND == StringUtils.indexOf(filePath, ".")){
            return StringUtils.EMPTY;
        }
        String suffix = StringUtils.substring(filePath, StringUtils.lastIndexOf(filePath, ".") + 1);
        return StringUtils.trimToEmpty(suffix);
    }

    public static String getFileName(String filePath){
        if(StringUtils.INDEX_NOT_FOUND == StringUtils.indexOf(filePath, ".")){
            return StringUtils.EMPTY;
        }
        String value = StringUtils.substring(filePath,0, StringUtils.lastIndexOf(filePath, "."));
        return StringUtils.trimToEmpty(value);
    }

    public static boolean writeInputStreamToFile(InputStream inputStream, File f){
        try{
            FileUtils.copyInputStreamToFile(inputStream, f);
            return true;
        }catch (IOException e){
            e.printStackTrace();
        }
        return false;
    }

    public static String getMD5(String filename){
        File f = new File(filename);
        try(FileInputStream fileInputStream = new FileInputStream(f)) {
            return DigestUtils.md5Hex(fileInputStream);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static String getMD5(InputStream inputStream){
        try {
            return DigestUtils.md5Hex(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static boolean writeURLToFile(URL url, File f){
        try{
            FileUtils.copyURLToFile(url, f);
            return true;
        }catch (IOException e){
            e.printStackTrace();
        }
        return false;
    }

    public static boolean writeStringToFile(String data, File file,String encoding) {
        try {
            FileUtils.writeStringToFile(file,data,encoding);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean writeByteArrayToFile( byte[] data,File file) {
        try {
            FileUtils.writeByteArrayToFile(file,data);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static File mkFile(String filePath){
        File f = new File(filePath);
        try{
            f.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
        }
        return f;
    }

    public static void deleteFiles(String directoryForDel, boolean keepItself)throws Exception{
        File file = new File(directoryForDel);
        if(keepItself){
            FileUtils.cleanDirectory(file);
        }else{
            FileUtils.deleteDirectory(file);
        }
    }

    public static void deleteFile(File file)throws Exception{
        FileUtils.forceDelete(file);
    }

    public static void copyFileWithSuffix(File srcDir, File destDir, String suffix){
        IOFileFilter suffixFileFilter = FileFilterUtils.suffixFileFilter(suffix);
        IOFileFilter fileFilter = FileFilterUtils.and(FileFileFilter.FILE, suffixFileFilter);
        try {
            FileUtils.copyDirectory(srcDir,destDir,fileFilter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  static  void moveDirectory(File srcDir,File destDir){
        try {
            FileUtils.moveDirectory(srcDir,destDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  static  void moveFile(File srcFile,File destFile){
        try {
            FileUtils.moveFile(srcFile,destFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getString(File file, String encode) {
        try {
            return FileUtils.readFileToString(file,encode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getByteArray(File file) {
        try {
            return FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InputStream getInputStream(File file) {
        try {
            return FileUtils.openInputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static OutputStream getOutputStream(File file, Boolean appendFlag) {
        try {
            if(appendFlag==true){
                return FileUtils.openOutputStream(file,true);
            }else{
                return FileUtils.openOutputStream(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
    * @Description: 得到目录下所有文件列表
    * @Param directory 遍历的文件夹
    * @Param extensions 文件后缀过滤
    * @Param recursive true为递归， false 则为非递归
    * @return: java.util.Collection<java.io.File>
    * @Author: WangMing
    * @Date: 2019/3/7
    */
    public static Collection<File> getFileList(File directory, String[] extensions, boolean recursive){
        return FileUtils.listFiles(directory,extensions,recursive);
    }

    /**
     *
     * @param directory 进行遍历的文件夹
     * @param fileFilter    文件需要满足的过滤器条件后缀
     *                  比如EmptyFileFilter、NameFileFilter、PrefixFileFilter
     *                     并且可以通过FileFilterUtils.and()进行组合
     * @param dirFilter   为空表示不进行递归遍历，DirectoryFileFilter.INSTANCE表示递归遍历文件夹
     * @return   文件
     */
    public static Collection<File> getFileList(File directory, IOFileFilter fileFilter, IOFileFilter dirFilter) {
        return FileUtils.listFiles(directory,fileFilter, dirFilter);
    }

    public static File multipartToFile(MultipartFile multifile)throws IOException{
        File f = new File(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + multifile.getOriginalFilename());
        MyFileUtils.writeInputStreamToFile(multifile.getInputStream(),f);
        return f;
    }

    /**
    * @Description: 对字符串进行加密
    * @Param: [buffer]
    * @return: java.lang.String
    * @Author: WangMing
    * @Date: 2019/3/15
    */
    public static String encryption(String buffer) throws UnsupportedEncodingException {
        byte[] temp1 = buffer.getBytes("UTF-8");
        Base64 base64 = new Base64();
        //hex 加密
        String encodedText = Hex.encodeHexString(temp1);
        //base64 加密
        byte[] temp2 = encodedText.getBytes("UTF-8");
        encodedText = base64.encodeAsString(temp2);
        //拼接字符串
        encodedText = RandomStringUtils.randomAlphabetic(5) + encodedText + RandomStringUtils.randomAlphabetic(5);
        byte[] temp3 = encodedText.getBytes("UTF-8");
        encodedText = base64.encodeAsString(temp3);

        return encodedText;
    }

    public static String decryption(String buffer) throws UnsupportedEncodingException, DecoderException {
        Base64 base64 = new Base64();
        //base64 解密
        String decodedText = new String(base64.decode(buffer), "UTF-8");
        //裁剪字符串，去除该字符串前面和后面五个字符
        decodedText = decodedText.substring(5,decodedText.length()-5);
        decodedText = new String(base64.decode(decodedText), "UTF-8");
        String result = new String(Hex.decodeHex(decodedText), "UTF-8");
        return result;
    }

    /**
     * wzh
     * 获得文件流内容
     * @param fileUrl
     * @return
     */
    public static String getValueFromFile(String fileUrl){
        String result = null;
        try{
            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setReadTimeout(60000);
            conn.setConnectTimeout(5000);

            InputStream inputStream = conn.getInputStream();

            byte[] buffer = new byte[1024];
            int length = 0;
            while(( length = inputStream.read(buffer))!=-1){
                String str = new String(buffer);
                System.out.println(str);
                result = str;
            }


        }catch (Exception e){
            System.out.println(e);
        }

        return result;
    }

    public static InputStream getRemoteFileStream(String fileUrl){
        try{
            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setReadTimeout(60000);
            conn.setConnectTimeout(5000);

            InputStream inputStream = conn.getInputStream();

            return inputStream;

        }catch (Exception e){
            System.out.println(e);
            return null;
        }
    }

    public static String getFileStreamSuffix(byte[] src){
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

}
