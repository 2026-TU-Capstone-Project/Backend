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
@Schema(description = "피드 목록 항목")
public class FeedListResponseDto {

    @Schema(description = "피드 ID")
    private Long feedId;
    @Schema(description = "피드 제목")
    private String feedTitle;
    @Schema(description = "스타일 이미지 URL")
    private String styleImageUrl;
    @Schema(description = "작성자 닉네임")
    private String authorNickname;
    @Schema(description = "작성자 프로필 이미지 URL")
    private String authorProfileImageUrl;
    @Schema(description = "좋아요 수")
    private int likeCount;
    @Schema(description = "현재 사용자 좋아요 여부")
    private boolean isLiked;
    @Schema(description = "공개 범위")
    private String visibility;

    public static FeedListResponseDto from(Feed feed) {
        if (feed == null) return null;
        return FeedListResponseDto.builder()
                .feedId(feed.getId())
                .feedTitle(feed.getFeedTitle())
                .styleImageUrl(feed.getStyleImageUrl())
                .authorNickname(feed.getUser() != null ? feed.getUser().getNickname() : null)
                .authorProfileImageUrl(feed.getUser() != null ? feed.getUser().getProfileImageUrl() : null)
                .likeCount(0)
                .isLiked(false)
                .visibility(feed.getVisibility() != null ? feed.getVisibility().name() : "PUBLIC")
                .build();
    }
}
