package com.example.Capstone_project.domain; // 패키지명 확인!

public enum FittingStatus {
    WAITING,    // 대기 중 (진동벨 받음)
    PROCESSING, // AI가 옷 입히는 중
    COMPLETED,  // 완료 (사진 나옴)
    FAILED      // 실패 (에러)
}