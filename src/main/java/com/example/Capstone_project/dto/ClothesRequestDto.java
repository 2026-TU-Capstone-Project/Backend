package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Schema(description = "옷 일괄 분석 요청 (multipart/form-data). 각 필드는 선택적으로 업로드 가능.")
public class ClothesRequestDto {
    @Schema(description = "상의 사진 파일")
    private MultipartFile top;
    @Schema(description = "하의 사진 파일")
    private MultipartFile bottom;
    @Schema(description = "신발 사진 파일")
    private MultipartFile shoes;
}