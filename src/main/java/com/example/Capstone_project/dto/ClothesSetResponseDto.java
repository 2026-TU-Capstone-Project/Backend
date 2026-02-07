package com.example.Capstone_project.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "코디 세트 응답 정보")
public class ClothesSetResponseDto {
    private Long id;
    private String setName;
    private String representativeImageUrl;
    private List<FittingDto> fittingTasks;
    private List<ClothesDto> clothes;

    @Getter
    @AllArgsConstructor
    public static class FittingDto {
        private Long id;
        private String imageUrl;
    }

    @Getter
    @AllArgsConstructor
    public static class ClothesDto {
        private Long id;
        private String name;
        private String category;
        private String imgUrl;
    }
}