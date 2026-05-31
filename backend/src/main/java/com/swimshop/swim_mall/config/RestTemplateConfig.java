package com.swimshop.swim_mall.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

	/**
	 * 토스 등 외부 API 응답이 UTF-8 JSON인데, OS 기본 인코딩으로 읽으면 한글(method 등)이 깨져 DB에 저장됨.
	 */
	@Bean
	public RestTemplate restTemplate() {
		RestTemplate rt = new RestTemplate();
		List<HttpMessageConverter<?>> converters = rt.getMessageConverters();
		for (int i = 0; i < converters.size(); i++) {
			if (converters.get(i) instanceof StringHttpMessageConverter) {
				converters.set(i, new StringHttpMessageConverter(StandardCharsets.UTF_8));
				break;
			}
		}
		return rt;
	}
}
