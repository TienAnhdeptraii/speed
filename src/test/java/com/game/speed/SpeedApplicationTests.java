package com.game.speed;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Public test-only key; the running application still requires APP_JWT_SECRET.
@SpringBootTest(properties = {
		"app.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class SpeedApplicationTests {

	@Test
	void contextLoads() {
	}

}
