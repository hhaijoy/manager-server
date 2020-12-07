package com.example.demo;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.controller.HelloWorld;
import com.example.demo.domain.Scheduler.Task;
import com.example.demo.domain.xml.ConditionCase;
import com.example.demo.domain.xml.ControlCondition;
import com.example.demo.utils.MyFileUtils;
import com.example.demo.utils.MyHttpUtils;
import com.example.demo.utils.TaskLoop;
import com.example.demo.utils.XmlParseUtils;
import org.dom4j.DocumentException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.equalTo;


@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoApplicationTests {


	private MockMvc mvc;

	@Before
	public void setUp(){
		mvc = MockMvcBuilders.standaloneSetup(new HelloWorld()).build();
	}

	@Test
	public void getHello() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/hello").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().string(equalTo("Hello World")));
	}

	@Test
	public void getFileContent(){
		MyFileUtils myFileUtils = new MyFileUtils();
		myFileUtils.getValueFromFile("http://221.226.60.2:8082/data?uid=c6f8c008-1fbc-48bd-8dc6-53bc31d55085");
	}

	@Test
	public void getSuffix() throws IOException {
		URL url = new URL("http://221.226.60.2:8062/data/5fb776d6ad4db01b2dc7c3d1?pwd=");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setReadTimeout(60000);
		conn.setConnectTimeout(5000);

		Map<String, List<String>> headers = conn.getHeaderFields();

		InputStream inputStream = conn.getInputStream();

		byte[] buffer = new byte[1024];

		inputStream.read(buffer, 0, buffer.length);

		String a = MyFileUtils.getFileStreamSuffix(buffer);

		System.out.println(a);
	}

	@Test
	public void taskLoopCalculate(){
		Task task = new Task();
		TaskLoop taskLoop = new TaskLoop("aaa");

		ConditionCase conditionCase = new ConditionCase();
		conditionCase.setOpertator(">");
		conditionCase.setStandard("50");
		conditionCase.setRelation("and");

		List<ConditionCase> conditionCases = new ArrayList<>();
		conditionCases.add(conditionCase);

		ConditionCase conditionCase1 = new ConditionCase();
		conditionCase1.setOpertator("<");
		conditionCase1.setStandard("80");
		conditionCase1.setRelation("and");
		conditionCases.add(conditionCase1);

		ControlCondition controlCondition = new ControlCondition();
		controlCondition.setConditionCases(conditionCases);
		controlCondition.setId("11");

		controlCondition.setValue("90");
		controlCondition.setFormat("number");

		taskLoop.judgeCondition(controlCondition);

	}

	@Test
	public void getText() throws DocumentException {
		String xml = "<root>\n" +
				"    <uid>http://111.229.14.128:8899/data?uid=5f6cf72f-45f6-4750-b70e-1009fa8f9727</uid>\n" +
				"    <stout> 3003084.73500096&#xD;\n" +
				"</stout>\n" +
				"</root>";
		XmlParseUtils.getNodeContent(xml,"uid");
	}

	@Test
	public void testDataprocessing() throws IOException, DocumentException {
		String baseUrl = "http://111.229.14.128:8898/invokeUrlDataPcs";
		String token = "+LSZXWI1mZ/yY+0nBSYaEw==";
		String service = "35c60f27-08f1-4dea-ae57-0fc8b0f095da";
		String data = "http://172.21.212.85:8062/data/5fc1c38e96e6891294fcd1dd?pwd=";

		JSONObject params = new JSONObject();
		params.put("token", URLEncoder.encode(token));
		params.put("pcsId",service);
		params.put("url",data);
//        params.put("params","Processing");

		Map<String,String> header = new HashMap<>();
		header.put("Content-Type","application/x-www-form-urlencoded");

		String result = MyHttpUtils.POSTWithJSON(baseUrl,"UTF-8",header,params);

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type","application/x-www-form-urlencoded");

		MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
		requestBody.add("token", URLEncoder.encode(token));
		requestBody.add("pcsId",service);
		requestBody.add("url",data);
		HttpEntity<MultiValueMap> requestEntity = new HttpEntity<MultiValueMap>(requestBody, headers);

		String resultXml = restTemplate.postForObject(baseUrl,requestEntity,String.class);

		String dataResult = XmlParseUtils.getNodeContent(resultXml,"uid");
	}
}

