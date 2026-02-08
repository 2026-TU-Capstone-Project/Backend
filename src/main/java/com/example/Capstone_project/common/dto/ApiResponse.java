package com.example.Capstone_project.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공통 API 응답 래퍼. success, message, data로 구성됩니다.")
public class ApiResponse<T> {
	@Schema(description = "요청 성공 여부", example = "true")
	private boolean success;
	@Schema(description = "결과 메시지", example = "조회 성공")
	private String message;
	@Schema(description = "응답 데이터 (실패 시 null)")
	private T data;
	
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, "Success", data);
	}
	
	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(true, message, data);
	}
	
	public static <T> ApiResponse<T> error(String message) {
		return new ApiResponse<>(false, message, null);
	}
}










