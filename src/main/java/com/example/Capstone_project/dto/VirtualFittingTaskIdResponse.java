package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
@AllArgsConstructor
@Schema(description = "가상 피팅 작업 ID")
public class VirtualFittingTaskIdResponse {

	@Schema(example = "123", description = "작업 ID (문자열)")
	private Long taskId;
}

