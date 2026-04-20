package com.example.Capstone_project.common.exception;

import com.example.Capstone_project.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<?>> handleResourceNotFoundException(ResourceNotFoundException e) {
		log.warn("Resource not found: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiResponse.error(e.getMessage()));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiResponse<?>> handleBadRequestException(BadRequestException e) {
		log.warn("Bad request: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(e.getMessage()));
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ApiResponse<?>> handleForbiddenException(ForbiddenException e) {
		log.warn("Forbidden: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(ApiResponse.error(e.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<?>> handleIllegalArgument(IllegalArgumentException e) {
		log.warn("Illegal argument: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
		log.error("Unhandled exception: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
		String clientMessage = "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponse.error(clientMessage));
	}
}










