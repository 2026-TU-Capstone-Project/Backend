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
@Schema(description = "피드 상세 (작성자 + 스타일/상의/하의 이미지·이름·ID + 제목·내용)")
public class FeedDetailResponseDto {

    @Schema(description = "작성자 사용자 ID")
    private Long authorId;
    @Schema(description = "작성자 닉네임")
    private String authorNickname;
    @Schema(description = "작성자 프로필 이미지 URL")
    private String authorProfileImageUrl;
    @Schema(description = "스타일 이미지 URL")
    private String styleImageUrl;
    @Schema(description = "스타일 이미지(가상 피팅 작업) ID")
    private Long styleImageId;
    @Schema(description = "상의 이미지 URL")
    private String topImageUrl;
    @Schema(description = "상의 이름")
    private String topName;
    @Schema(description = "상의(Clothes) ID")
    private Long topClothesId;
    @Schema(description = "하의 이미지 URL")
    private String bottomImageUrl;
    @Schema(description = "하의 이름")
    private String bottomName;
    @Schema(description = "하의(Clothes) ID")
    private Long bottomClothesId;
    @Schema(description = "피드 제목")
    private String feedTitle;
    @Schema(description = "피드 내용")
    private String feedContent;
    @Schema(description = "좋아요 수")
    private int likeCount;
    @Schema(description = "현재 사용자 좋아요 여부")
    private boolean isLiked;

    public static FeedDetailResponseDto from(Feed feed) {
        if (feed == null) return null;
        return FeedDetailResponseDto.builder()
                .authorId(feed.getUser().getId())
                .authorNickname(feed.getUser().getNickname() != null ? feed.getUser().getNickname() : "")
                .authorProfileImageUrl(feed.getUser().getProfileImageUrl())
                .styleImageUrl(feed.getStyleImageUrl())
                .styleImageId(feed.getFittingTaskId())
                .topImageUrl(feed.getTopImageUrl())
                .topName(feed.getTopName())
                .topClothesId(feed.getTopClothesId())
                .bottomImageUrl(feed.getBottomImageUrl())
                .bottomName(feed.getBottomName())
                .bottomClothesId(feed.getBottomClothesId())
                .feedTitle(feed.getFeedTitle())
                .feedContent(feed.getFeedContent())
                .build();
    }
}
