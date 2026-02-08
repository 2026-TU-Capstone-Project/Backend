package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.Capstone_project.domain.ClothesSet;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
    private String styleAnalysis; // 가상 피팅 결과 이미지의 스타일 분석 (한글 텍스트, 유사도 검색용)

    @Column(name = "style_embedding", columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    private float[] styleEmbedding; // 스타일 분석 텍스트의 임베딩 벡터 (pgvector 유사도 검색용)

    /**
     * 결과 이미지 속 인물의 성별 (Gemini가 이미지 분석으로 판별)
     * 사용자가 다른 성별 사진으로 피팅해도 실제 이미지 기준으로 필터링 가능
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "result_gender", length = 10)
    private Gender resultGender;

    // 👈 FittingTask.java 파일 맨 아래 } 바로 위에 넣으세요.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_set_id")
    private ClothesSet clothesSet;

    // 생성자 (주문 들어왔을 때)
    public FittingTask(FittingStatus status) {
        this.status = status;
    }
}