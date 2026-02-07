package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 자동 배포(CD) 테스트용 엔드포인트.
 * push 후 CD가 끝나면 이 값을 바꿔서 배포가 반영됐는지 확인할 수 있음.
 */
@RestController
@RequestMapping("/api/v1")
public class DeployTestController {

	// 배포 테스트할 때마다 이 문자열을 바꿔서 push → CD 후 응답이 바뀌는지 확인
	private static final String DEPLOY_TEST_MARKER = "auto-deploy-test-v1";

	@GetMapping("/deploy-info")
	public ResponseEntity<ApiResponse<Map<String, Object>>> deployInfo() {
		Map<String, Object> data = Map.of(
			"deployTest", DEPLOY_TEST_MARKER,
			"serverTime", Instant.now().toString(),
			"status", "ok"
		);
		return ResponseEntity.ok(ApiResponse.success("배포 확인용", data));
	}
}
