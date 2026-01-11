package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class Fitting {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 비동기 작업 관리
    private String taskId;      // AI 서버에서 발급받은 작업 ID
    private String status;      // 상태 (WAITING, PROCESSING, DONE, FAILED)

    private String resultImgUrl; // 완성된 피팅 사진 주소

    private LocalDateTime fittedAt; // 요청 시간

    // 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;      // 누가

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_id")
    private Clothes clothes;    // 어떤 옷을

    @PrePersist
    public void prePersist() {
        this.fittedAt = LocalDateTime.now();
    }
}