package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.FittingStatus;
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
@Schema(description = "가상 피팅 작업 상태 응답")
public class VirtualFittingStatusResponse {
    @Schema(description = "작업 ID")
    private Long taskId;
    @Schema(description = "상태 (WAITING, PROCESSING, COMPLETED, FAILED)", example = "COMPLETED")
    private FittingStatus status;
    @Schema(description = "완료 시 결과 이미지 URL (미완료 시 null)")
    private String resultImgUrl;
}

