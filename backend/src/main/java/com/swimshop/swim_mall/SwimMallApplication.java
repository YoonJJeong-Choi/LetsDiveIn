package com.swimshop.swim_mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 스케줄러 활성화 (자동 구매 확정용)
public class SwimMallApplication {

	public static void main(String[] args) {
		SpringApplication.run(SwimMallApplication.class, args);
	}

}
