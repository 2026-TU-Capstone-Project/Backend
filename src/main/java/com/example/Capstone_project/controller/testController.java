package com.example.Capstone_project.controller;

import com.example.Capstone_project.service.ClothesAnalysisService;
import com.example.Capstone_project.dto.ClothesRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test") // 주소가 "/api/v1/test"로 시작
@RequiredArgsConstructor
public class testController {

    // (비동기 엔진) 연결
    private final ClothesAnalysisService clothesAnalysisService;

    // 접속 확인용
    @GetMapping("/")
    public String test() {
        return "this is test";
    }

    // (비동기 분석 요청)
    // 주소: http://localhost:8080/api/v1/test/analysis
    @PostMapping("/analysis") // POST로 파일 보낼 거니까
    public String analyze(@ModelAttribute ClothesRequestDto requestDto) {

        // 서비스한테 바구니째로 넘김
        clothesAnalysisService.analyzeClothes(requestDto);
        return "✅ 옷 분석 요청을 보냈습니다! (로그 확인)";
    }
}