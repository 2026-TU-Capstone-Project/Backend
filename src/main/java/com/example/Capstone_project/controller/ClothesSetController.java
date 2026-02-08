package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.config.CustomUserDetails;
import com.example.Capstone_project.domain.ClothesSet;
import com.example.Capstone_project.service.ClothesSetService;
import com.example.Capstone_project.dto.ClothesSetResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Clothes Set API", description = "코디 세트(폴더) 관리 및 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clothes-sets")
public class ClothesSetController {

    private final ClothesSetService clothesSetService;

    @Getter
    @NoArgsConstructor
    @Schema(description = "코디 저장 요청")
    public static class SaveRequest {
        @Schema(description = "폴더 이름", example = "주말 데이트룩")
        private String setName;
        @Schema(description = "포함할 옷(Clothes) ID 목록")
        private List<Long> clothesIds;
        @Schema(description = "가상 피팅 결과(FittingTask) ID")
        private Long fittingTaskId;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "폴더 이름 수정 요청")
    public static class UpdateRequest {
        @Schema(description = "새 폴더 이름", example = "데이트룩")
        private String newName;
    }

    @Operation(
        summary = "코디 저장",
        description = "새 폴더를 만들고, 가상 피팅 결과(착장)와 옷 ID들을 함께 저장합니다."
    )
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
    @Operation(summary = "폴더 이름 수정", description = "기존 코디 폴더의 이름을 변경합니다.")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateSetName(
            @Parameter(description = "코디 세트(폴더) ID") @PathVariable Long id,
            @RequestBody UpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        clothesSetService.updateSetName(id, request.getNewName(), userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("폴더 이름이 수정되었습니다.", null));
    }

    // 내 폴더 목록 조회
    @Operation(summary = "내 폴더 목록 조회", description = "사용자의 모든 코디 폴더 목록과 대표 이미지를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClothesSetResponseDto>>> getSets( // 여기 타입을 Dto로 변경!
                                                                             @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success("조회 성공", clothesSetService.getMySets(userDetails.getUser())));
    }

    /**
     * [착장 개별 삭제] 폴더 내의 특정 피팅 결과 삭제
     */
    @Operation(summary = "착장 개별 삭제", description = "폴더 내에 저장된 특정 피팅 결과(착장)를 삭제합니다.")
    @DeleteMapping("/fitting/{fittingTaskId}")
    public ResponseEntity<ApiResponse<String>> deleteFitting(
            @Parameter(description = "삭제할 피팅 작업(FittingTask) ID") @PathVariable Long fittingTaskId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        clothesSetService.deleteFittingFromSet(fittingTaskId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("선택한 착장이 삭제되었습니다.", null));
    }

    // 폴더 삭제
    @Operation(summary = "폴더 전체 삭제", description = "코디 폴더와 그 안의 모든 내용을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteSet(
            @Parameter(description = "코디 세트(폴더) ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        clothesSetService.deleteSet(id, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("폴더가 삭제되었습니다.", null));
    }
}