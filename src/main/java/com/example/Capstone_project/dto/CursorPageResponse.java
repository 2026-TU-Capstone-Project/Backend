package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "커서 기반 페이지네이션 응답")
public class CursorPageResponse<T> {

    @Schema(description = "결과 목록")
    private List<T> items;

    @Schema(description = "다음 페이지 커서 (null이면 마지막 페이지)")
    private String nextCursor;

    @Schema(description = "다음 페이지 존재 여부")
    private boolean hasMore;
}
