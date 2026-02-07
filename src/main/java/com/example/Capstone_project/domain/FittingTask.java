package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.Capstone_project.domain.ClothesSet;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "fitting_tasks") // ERD 테이블 이름
public class FittingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // task_id

    @Column(name = "user_id")
    private Long userId; // 유저ID


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private User user;

    @Column(name = "top_id")
    private Long topId; // 상의ID (Clothes 참조)

    /**
     * 상의 엔티티 연관관계 (읽기 전용)
     * 실제 FK 컬럼은 topId 를 그대로 사용하고,
     * 기존 서비스 코드 변경 없이 엔티티에서 접근만 가능하도록 insertable/updatable 을 막아둡니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "top_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private Clothes top;

    @Column(name = "bottom_id")
    private Long bottomId; // 하의ID (Clothes 참조)

    /**
     * 하의 엔티티 연관관계 (읽기 전용)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "bottom_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private Clothes bottom;

    @Column(name = "body_img_url")
    private String bodyImgUrl; // 전신 사진 이미지 URL

    @Column(name = "is_saved")
    private boolean isSaved = false; // 기본값은 false (저장 안 함)

    // ★ 핵심: 현재 진행 상태
    @Enumerated(EnumType.STRING)
    private FittingStatus status;

    @Column(name = "result_img_url")
    private String resultImgUrl; // 가상 피팅 결과 이미지 URL

    @Column(name = "style_analysis", columnDefinition = "TEXT")
    private String styleAnalysis; // 가상 피팅 결과 이미지의 스타일 분석 (한글 텍스트, 예: "캐주얼한 남성 스타일")

    // 👈 FittingTask.java 파일 맨 아래 } 바로 위에 넣으세요.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_set_id")
    private ClothesSet clothesSet;

    // 생성자 (주문 들어왔을 때)
    public FittingTask(FittingStatus status) {
        this.status = status;
    }
}