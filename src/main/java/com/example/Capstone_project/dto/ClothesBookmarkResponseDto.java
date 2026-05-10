package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.ClothesBookmark;
import com.example.Capstone_project.domain.Feed;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "피드에서 저장한 옷 북마크")
public class ClothesBookmarkResponseDto {

    @Schema(description = "북마크 ID")
    private Long id;

    @Schema(description = "출처 피드 ID")
    private Long feedId;

    @Schema(description = "옷 ID (상세 조회 시 GET /clothes/{clothesId} 사용)")
    private Long clothesId;

    @Schema(description = "포지션 (TOP / BOTTOM)")
    private String position;

    @Schema(description = "옷 이미지 URL")
    private String imgUrl;

    @Schema(description = "옷 이름")
    private String name;

    @Schema(description = "카테고리 (Top / Bottom)")
    private String category;

    @Schema(description = "저장 일시")
    private LocalDateTime savedAt;

    public static ClothesBookmarkResponseDto from(ClothesBookmark bookmark) {
        Feed feed = bookmark.getFeed();
        boolean isTop = "TOP".equalsIgnoreCase(bookmark.getPosition());

        return ClothesBookmarkResponseDto.builder()
                .id(bookmark.getId())
                .feedId(feed.getId())
                .clothesId(isTop ? feed.getTopClothesId() : feed.getBottomClothesId())
                .position(bookmark.getPosition())
                .imgUrl(isTop ? feed.getTopImageUrl() : feed.getBottomImageUrl())
                .name(isTop ? feed.getTopName() : feed.getBottomName())
                .category(isTop ? "Top" : "Bottom")
                .savedAt(bookmark.getSavedAt())
                .build();
    }
}
