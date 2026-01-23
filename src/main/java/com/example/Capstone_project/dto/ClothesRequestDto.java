package com.example.Capstone_project.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ClothesRequestDto {
    // 파일이 없으면 null로 들어옴 (선택적 업로드 가능)
    private MultipartFile top;    // 상의 사진
    private MultipartFile bottom; // 하의 사진
    private MultipartFile shoes;  // 신발 사진
}