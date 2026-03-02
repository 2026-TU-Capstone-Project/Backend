package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.ClothesUploadStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "옷 업로드 작업 상태 (SSE 이벤트 name=status)")
public class ClothesUploadStatusResponse {

	@Schema(description = "작업 ID")
	private Long taskId;
	@Schema(description = "상태 (WAITING, PROCESSING, COMPLETED, FAILED)", example = "COMPLETED")
	private ClothesUploadStatus status;
	@Schema(description = "완료 시 저장된 옷(Clothes) ID, 미완료 시 null")
	private Long clothesId;
	@Schema(description = "실패 시 오류 메시지, 성공 시 null")
	private String errorMessage;
}
