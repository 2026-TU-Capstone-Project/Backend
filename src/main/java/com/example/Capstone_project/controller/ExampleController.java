package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.dto.ExampleRequest;
import com.example.Capstone_project.dto.ExampleResponse;
import com.example.Capstone_project.service.ExampleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
public class ExampleController {
	
	private final ExampleService exampleService;
	
	@GetMapping
	public ResponseEntity<ApiResponse<List<ExampleResponse>>> getAll() {
		List<ExampleResponse> examples = exampleService.findAll();
		return ResponseEntity.ok(ApiResponse.success(examples));
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<ExampleResponse>> getById(@PathVariable Long id) {
		ExampleResponse example = exampleService.findById(id);
		return ResponseEntity.ok(ApiResponse.success(example));
	}
	
	@PostMapping
	public ResponseEntity<ApiResponse<ExampleResponse>> create(@Valid @RequestBody ExampleRequest request) {
		ExampleResponse example = exampleService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success("Example created successfully", example));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<ExampleResponse>> update(
		@PathVariable Long id,
		@Valid @RequestBody ExampleRequest request
	) {
		ExampleResponse example = exampleService.update(id, request);
		return ResponseEntity.ok(ApiResponse.success("Example updated successfully", example));
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
		exampleService.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Example deleted successfully", null));
	}
}

