package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.FittingTask;
import lombok.Getter;

/**
 * FittingTask + 유사도 점수 (0~1, 1에 가까울수록 유사)
 */
@Getter
public class FittingTaskWithScore {
    private final FittingTask task;
    private final double score;

    public FittingTaskWithScore(FittingTask task, double score) {
        this.task = task;
        this.score = score;
    }
}
