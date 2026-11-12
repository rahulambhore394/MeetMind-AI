package com.meetmind.meetmind_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeetmindBackendApplication {

	public static void main(String[] args) {
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
		loadDotEnv();
		SpringApplication.run(MeetmindBackendApplication.class, args);
	}

	private static void loadDotEnv() {
		for (String path : java.util.List.of(".env", "../.env")) {
			java.io.File file = new java.io.File(path);
			if (file.exists() && file.isFile()) {
				try {
					for (String line : java.nio.file.Files.readAllLines(file.toPath())) {
						String trimmed = line.trim();
						if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
							int eqIdx = trimmed.indexOf('=');
							String k = trimmed.substring(0, eqIdx).trim();
							String v = trimmed.substring(eqIdx + 1).trim();
							if (System.getProperty(k) == null && System.getenv(k) == null) {
								System.setProperty(k, v);
							}
						}
					}
					break;
				} catch (Exception ignored) {}
			}
		}
	}

}
