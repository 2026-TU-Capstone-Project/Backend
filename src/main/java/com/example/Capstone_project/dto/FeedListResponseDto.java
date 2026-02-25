package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.Feed;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "피드 목록 항목 (제목 + 스타일 이미지 + feedId)")
public class FeedListResponseDto {

    @Schema(description = "피드 ID")
    private Long feedId;
    @Schema(description = "피드 제목")
    private String feedTitle;
    @Schema(description = "스타일 이미지 URL")
    private String styleImageUrl;

    public static FeedListResponseDto from(Feed feed) {
        if (feed == null) return null;
        return FeedListResponseDto.builder()
                .feedId(feed.getId())
                .feedTitle(feed.getFeedTitle())
                .styleImageUrl(feed.getStyleImageUrl())
                .build();
    }
}
