package com.meetmind.meetmind_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeetmindBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeetmindBackendApplication.class, args);
	}

}
