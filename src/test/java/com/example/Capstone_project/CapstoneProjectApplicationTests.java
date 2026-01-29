package com.example.Capstone_project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"gemini.api.key=dummy-key-for-test",
	"gemini.api.analysis-model=gemini-2.5-flash"
})
class CapstoneProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
