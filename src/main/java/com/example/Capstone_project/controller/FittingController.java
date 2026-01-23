package com.example.Capstone_project.controller;

import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.service.FittingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/fitting")
@RequiredArgsConstructor
public class FittingController {

    private final FittingService fittingService;

    @PostMapping("/start")
    public Long startFitting(
            @RequestPart("userImage") MultipartFile userImage,
            @RequestPart("topImage") MultipartFile topImage,
            @RequestPart("bottomImage") MultipartFile bottomImage
    ) throws IOException {

        // 1. 주문 생성
        FittingTask task = fittingService.createFittingTask();

        // 2. byte[] 변환 (서비스 파라미터 타입 매칭)
        byte[] userBytes = userImage.getBytes();
        byte[] topBytes = topImage.getBytes();
        byte[] bottomBytes = bottomImage.getBytes();

        // 3. 비동기 호출 (인자 4개 전달)
        fittingService.processFitting(task.getId(), userBytes, topBytes, bottomBytes);

        return task.getId();
    }

    @GetMapping("/status/{id}")
    public FittingTask checkStatus(@PathVariable Long id) {
        return fittingService.checkStatus(id);
    }
}