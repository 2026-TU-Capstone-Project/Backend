package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "fitting_tasks") // ERD 테이블 이름
public class FittingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // task_id

    // ★ 핵심: 현재 진행 상태
    @Enumerated(EnumType.STRING)
    private FittingStatus status;

    private String resultImgUrl; // 결과 사진 URL

    // 생성자 (주문 들어왔을 때)
    public FittingTask(com.example.Capstone_project.domain.FittingStatus status) {
        this.status = status;
    }

    // 필요한 다른 필드들(user_id 등)은 형님 기존 코드에 맞춰 추가하면 됩니다.
}