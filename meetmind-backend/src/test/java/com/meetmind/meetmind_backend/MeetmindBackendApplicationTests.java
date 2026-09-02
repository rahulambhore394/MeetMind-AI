package com.meetmind.meetmind_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MeetmindBackendApplicationTests {

	@org.springframework.boot.test.mock.mockito.MockBean
	private com.meetmind.meetmind_backend.event.KafkaEventPublisher kafkaEventPublisher;

	@Test
	void contextLoads() {
	}

}
