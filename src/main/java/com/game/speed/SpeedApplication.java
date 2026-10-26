package com.game.speed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SpringBootApplication
@RestController
public class SpeedApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpeedApplication.class, args);
	}

	@GetMapping("/api/hello")
	public Map<String, String> hello() {
		return Map.of(
				"status", "success",
				"message", "Ứng dụng Spring Boot đã chạy thành công!"
		);
	}

}
