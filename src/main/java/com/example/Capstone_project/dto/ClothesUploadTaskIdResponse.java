package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
@AllArgsConstructor
@Schema(description = "옷 업로드 작업 ID (SSE 스트림 구독용)")
public class ClothesUploadTaskIdResponse {

	@Schema(example = "1", description = "업로드 작업 ID. GET /api/v1/clothes/upload/{taskId}/stream 으로 진행 상황 수신")
	private Long taskId;
}
