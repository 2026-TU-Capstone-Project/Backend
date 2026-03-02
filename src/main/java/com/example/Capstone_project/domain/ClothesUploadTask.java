package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 옷 1건 업로드(분석) 작업.
 * FittingTask와 동일 패턴: taskId 반환 → SSE로 상태 스트리밍.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "clothes_upload_tasks")
public class ClothesUploadTask {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 20)
	private String category; // Top, Bottom, Shoes

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ClothesUploadStatus status;

	@Column(name = "clothes_id")
	private Long clothesId; // 완료 시 저장된 Clothes.id

	@Column(name = "error_message", length = 500)
	private String errorMessage; // 실패 시 사유

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
