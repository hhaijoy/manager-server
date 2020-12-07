package com.example.demo;

import com.example.demo.domain.Scheduler.Task;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.concurrent.LinkedBlockingDeque;

@SpringBootApplication //核心注解，开启自动配置
@EnableAsync
@EnableCaching
@EnableSwagger2
public class DemoApplication {

	public static LinkedBlockingDeque<Task> linkedBlockingDeque = new LinkedBlockingDeque<>();
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}

