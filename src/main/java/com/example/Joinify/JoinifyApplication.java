package com.example.Joinify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
public class JoinifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(JoinifyApplication.class, args);
	}

}
