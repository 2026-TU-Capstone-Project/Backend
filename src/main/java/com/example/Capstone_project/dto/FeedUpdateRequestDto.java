package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "피드 수정 요청 (제목·내용만)")
public class FeedUpdateRequestDto {

    @Schema(description = "피드 제목")
    private String feedTitle;

    @Schema(description = "피드 내용")
    private String feedContent;
}
