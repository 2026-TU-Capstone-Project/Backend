package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.config.CustomUserDetails;
import com.example.Capstone_project.domain.ClothesSet;
import com.example.Capstone_project.service.ClothesSetService;
import com.example.Capstone_project.dto.ClothesSetResponseDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clothes-sets")
public class ClothesSetController {

    private final ClothesSetService clothesSetService;

    @Getter
    @NoArgsConstructor
    public static class SaveRequest {
        private String setName;
        private List<Long> clothesIds;
        private Long fittingTaskId;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String newName;
    }

    // 코디 저장 (폴더 생성 및 첫 착장 저장)

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Long>> saveSet(
            @RequestBody SaveRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ClothesSet saved = clothesSetService.saveFavoriteCoordination(
                request.getSetName(), request.getClothesIds(), request.getFittingTaskId(), userDetails.getUser()
        );

        return ResponseEntity.ok(ApiResponse.success("코디가 성공적으로 저장되었습니다.", saved.getId()));
    }

    // 폴더 이름 수정
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateSetName(
            @PathVariable Long id,
            @RequestBody UpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        clothesSetService.updateSetName(id, request.getNewName(), userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("폴더 이름이 수정되었습니다.", null));
    }

    // 내 폴더 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClothesSetResponseDto>>> getSets( // 여기 타입을 Dto로 변경!
                                                                             @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success("조회 성공", clothesSetService.getMySets(userDetails.getUser())));
    }

    /**
     * [착장 개별 삭제] 폴더 내의 특정 피팅 결과 삭제
     */
    @DeleteMapping("/fitting/{fittingTaskId}")
    public ResponseEntity<ApiResponse<String>> deleteFitting(
            @PathVariable Long fittingTaskId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        clothesSetService.deleteFittingFromSet(fittingTaskId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("선택한 착장이 삭제되었습니다.", null));
    }

    // 폴더 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteSet(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        clothesSetService.deleteSet(id, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("폴더가 삭제되었습니다.", null));
    }
}