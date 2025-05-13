package com.cvitae;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {})
@EnableAsync
public class CVitaeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CVitaeApplication.class, args);
	}

}
