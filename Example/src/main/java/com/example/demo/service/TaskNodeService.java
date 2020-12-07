package com.example.demo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.dao.TaskNodeDao;
import com.example.demo.domain.TaskNode;
import com.example.demo.domain.support.GeoInfoMeta;
import com.example.demo.domain.support.TaskNodeStatusInfo;
import com.example.demo.dto.taskNode.TaskNodeAddDTO;
import com.example.demo.dto.taskNode.TaskNodeCalDTO;
import com.example.demo.dto.taskNode.TaskNodeFindDTO;
import com.example.demo.dto.taskNode.TaskNodeReceiveDTO;
import com.example.demo.enums.ResultEnum;
import com.example.demo.exception.MyException;
import com.example.demo.utils.MyHttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by wang ming on 2019/2/18.
 */

@Service
public class TaskNodeService {


    @Autowired
    TaskNodeDao taskNodeDao;


    private static final Logger log = LoggerFactory.getLogger(TaskNodeService.class);

    @CachePut(value = "taskNode", key = "#taskNodeAddDTO.host")
    public TaskNode insert(TaskNodeAddDTO taskNodeAddDTO){
        TaskNode taskNode = new TaskNode();
        BeanUtils.copyProperties(taskNodeAddDTO,taskNode);
        taskNode.setCreateDate(new Date());
        taskNode.setRegister("njgis");
        //调用第三方API根据ip获取经纬度等信息
        try{
            taskNode.setGeoInfo(getGeoInfoMeta(taskNodeAddDTO.getHost()));
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
        return taskNodeDao.insert(taskNode);
    }

    @CacheEvict(value = "taskNode", key = "#id")
    public void delete(String id){
        taskNodeDao.deleteById(id);
    }

    public TaskNode findTaskNodeByHost(String host, String port){
        return taskNodeDao.findFirstByHostAndPort(host,port);
    }

    public List<TaskNode> list(TaskNodeFindDTO taskNodeFindDTO){
        List<TaskNode> pageTaskNode;
        int page = taskNodeFindDTO.getPage();
        int pageSize = taskNodeFindDTO.getPageSize();
        Sort sort = new Sort(taskNodeFindDTO.getAsc() ? Sort.Direction.ASC : Sort.Direction.DESC, "createDate");
        pageTaskNode = taskNodeDao.findAll(PageRequest.of(page - 1,pageSize,sort)).getContent();
        return pageTaskNode;
    }

    //查询数据库所有记录并返回
    public List<TaskNodeReceiveDTO> listAll(){
        List<TaskNode> taskNodeList;
        taskNodeList = taskNodeDao.findAll();
        List<TaskNodeReceiveDTO> returnList = new ArrayList<TaskNodeReceiveDTO>();
        //筛选需要返回的字段
        taskNodeList.forEach((TaskNode obj) ->{
            TaskNodeReceiveDTO temp = new TaskNodeReceiveDTO();
            temp.setId(obj.getId());
            temp.setHost(obj.getHost());
            temp.setPort(obj.getPort());
            returnList.add(temp);
        });
        return returnList;
    }

    @Cacheable(value = "taskNode", key = "#id")
    public TaskNode getById(String id){
        return taskNodeDao.findById(id).orElseGet(() -> {
            System.out.println("有人乱查数据库！！该ID不存在对象");
            throw new MyException(ResultEnum.NO_OBJECT);
        });
    }

    public TaskNodeReceiveDTO getTaskServerForRegister(TaskNodeCalDTO taskNodeCalDTO){
        List<TaskNodeReceiveDTO> taskNodeReceiveDTOs = taskNodeCalDTO.getData();
        //根据TaskNodeReceiveDTO 的time字段进行排序，后续进行得分计算(目前数据信息就只有延迟信息)
        taskNodeReceiveDTOs.sort(new Comparator<TaskNodeReceiveDTO>() {
            @Override
            public int compare(TaskNodeReceiveDTO o1, TaskNodeReceiveDTO o2) {
                return o1.getTime() - o2.getTime();
            }
        });
       //taskNodeReceiveDTOs.sort(Comparator.comparing(TaskNodeReceiveDTO::getTime));
        //HashMap，key值为经排序后taskNodeReceiveDTOs的下标，value值为得分
        //TODO
        return taskNodeReceiveDTOs.get(0);
    }

    @Async
    public Future<TaskNodeStatusInfo> judgeTaskNodeByPid(TaskNodeReceiveDTO taskNodeReceiveDTO, String pid){
        TaskNodeStatusInfo taskNodeStatusInfo = new TaskNodeStatusInfo();
        taskNodeStatusInfo.setId(taskNodeReceiveDTO.getId());
        taskNodeStatusInfo.setHost(taskNodeReceiveDTO.getHost());
        taskNodeStatusInfo.setPort(taskNodeReceiveDTO.getPort());
        //测试案例所用url
        String url = "http://" + taskNodeReceiveDTO.getHost() + ":" + taskNodeReceiveDTO.getPort() + "/server/status?pid=" + pid;
        String result;
        try{
            result = MyHttpUtils.GET(url,"UTF-8",null);
        }catch (Exception e){
            result = null;
        }
        if(result == null){
            taskNodeStatusInfo.setStatus(false);
        }else{
            JSONObject res = JSON.parseObject(result);
            if(res.getIntValue("code") != 1){
                taskNodeStatusInfo.setStatus(false);
            }
            JSONObject data = res.getJSONObject("data");
            taskNodeStatusInfo.setStatus(data.getBoolean("status"));
            taskNodeStatusInfo.setRunning(data.getIntValue("running"));
        }
        return new AsyncResult<>(taskNodeStatusInfo);
    }

    @Async()
    public Future<TaskNodeStatusInfo> judgeTaskNodeAboutLocal(TaskNodeReceiveDTO taskNodeReceiveDTO){
        TaskNodeStatusInfo taskNodeStatusInfo = new TaskNodeStatusInfo();
        taskNodeStatusInfo.setId(taskNodeReceiveDTO.getId());
        taskNodeStatusInfo.setHost(taskNodeReceiveDTO.getHost());
        taskNodeStatusInfo.setPort(taskNodeReceiveDTO.getPort());
        //获取可以部署节点url
        String url = "http://" + taskNodeReceiveDTO.getHost() + ":" + taskNodeReceiveDTO.getPort() + "/server/localStatus";
        String result = "";
        try{
            result = MyHttpUtils.GET(url, "UTF-8", null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            result = null;
        } catch (IOException e) {
            e.printStackTrace();
            result = null;
        }

        if(result == null){
            taskNodeStatusInfo.setStatus(false);
        }else{
            JSONObject response = JSON.parseObject(result);
            if(response.getIntValue("code") == -1){
                taskNodeStatusInfo.setStatus(false);
            }else{
                JSONObject data = response.getJSONObject("data");
                taskNodeStatusInfo.setStatus(data.getBoolean("status"));
                taskNodeStatusInfo.setRunning(data.getIntValue("running"));
            }
        }
        return new AsyncResult<>(taskNodeStatusInfo);
    }



    /**
    * @Description: 判断该任务节点是否已经注册过
    * @Param: [host, port]
    * @return: boolean
    * @Author: WangMing
    * @Date: 2019/2/18
    */
    public boolean judgeTaskNode(String host, String port){
        boolean flag = false;
        TaskNode result = taskNodeDao.findFirstByHostAndPort(host,port);
        if(result != null){
            flag = true;
        }
        return flag;
    }

    private GeoInfoMeta getGeoInfoMeta(String host)throws Exception{
        String url = "http://ip-api.com/json/" + host;
        String result = MyHttpUtils.GET(url,"UTF-8",null);
        GeoInfoMeta geoInfoMeat = new GeoInfoMeta();
        JSONObject res = JSONObject.parseObject(result);
        //judge
        if(res.getString("status").equals("fail")){
            //后面移除该部分，说明该要注册的任务服务器不是公网服务器，直接抛出错误
            geoInfoMeat.setCity("Nanjing");
            geoInfoMeat.setRegion("Jiangsu");
            geoInfoMeat.setCountryCode("CN");
            geoInfoMeat.setCountryName("China");
            geoInfoMeat.setLatitude("32.0617");
            geoInfoMeat.setLongitude("118.7778");
        }else{
            geoInfoMeat.setCity(res.getString("city").replace(" ","_"));
            geoInfoMeat.setRegion(res.getString("region"));
            geoInfoMeat.setCountryCode(res.getString("countryCode"));
            geoInfoMeat.setCountryName(res.getString("country"));
            geoInfoMeat.setLatitude(res.getString("lat"));
            geoInfoMeat.setLongitude(res.getString("lon"));
        }
        return geoInfoMeat;
    }

    @Async
    public Future<List<String>> getAllServiceByTaskNode(TaskNodeReceiveDTO taskNodeReceiveDTO){

        //测试用例url
        String url = "http://" + taskNodeReceiveDTO.getHost() + ":" + taskNodeReceiveDTO.getPort() + "/server/modelPid/all";
        String result;
        try{
            result = MyHttpUtils.GET(url, "UTF-8", null);
        }catch (Exception e){
            e.printStackTrace();
            result = null;
        }
        if(result == null){
            return null;
        }else{
            JSONObject res = JSON.parseObject(result);
            if (res.getIntValue("code") != 1) {
                return new AsyncResult<>(new ArrayList<>());
            }
            JSONArray services = res.getJSONArray("data");
            List<String> array = new ArrayList<>();
            for (int i = 0; i < services.size(); i++) {
                array.add(services.getString(i));
            }
            return new AsyncResult<>(array);
        }

    }

}
