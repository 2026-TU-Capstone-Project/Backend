package com.example.Capstone_project.domain;

/**
 * 옷 업로드(분석) 작업 상태.
 * 가상 피팅 FittingStatus와 동일한 흐름 (WAITING → PROCESSING → COMPLETED/FAILED).
 */
public enum ClothesUploadStatus {
	WAITING,    // 대기
	PROCESSING, // 분석 중 (GCS 업로드, AI 분석)
	COMPLETED,  // 완료 (Clothes 저장됨)
	FAILED      // 실패
}
