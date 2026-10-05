package com.hottalk.hottalkserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotTalkServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(HotTalkServerApplication.class, args);
		System.out.println("스프링부트 서버 실행 완료");
	}

}
