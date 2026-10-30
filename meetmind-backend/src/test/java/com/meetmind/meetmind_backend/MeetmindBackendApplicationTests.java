package com.meetmind.meetmind_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

@SpringBootTest
@ActiveProfiles("test")
@Import(com.meetmind.meetmind_backend.chat.TestRedisConfig.class)
class MeetmindBackendApplicationTests {

	@org.springframework.boot.test.mock.mockito.MockBean
	private com.meetmind.meetmind_backend.event.KafkaEventPublisher kafkaEventPublisher;

	@Test
	void contextLoads() {
	}

}
