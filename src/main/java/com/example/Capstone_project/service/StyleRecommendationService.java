package com.example.Capstone_project.service;

import com.example.Capstone_project.common.exception.BadRequestException;
import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.dto.FittingTaskWithScore;
import com.example.Capstone_project.repository.FittingRepository;
import com.example.Capstone_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 사용자 검색어 기반 스타일 추천 서비스
 * 스타일 분석 임베딩과 유사도 검색으로 최대 10개 추천
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StyleRecommendationService {

    private static final int DEFAULT_LIMIT = 10;

    private final GeminiService geminiService;
    private final FittingRepository fittingRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 검색어와 유사한 스타일의 가상 피팅 결과 최대 10개 추천
     *
     * @param userQuery 예: "결혼식에 입고 갈 단정하고 깔끔한 스타일 추천해줘"
     * @param minScore  최소 유사도 점수 (0~1, null이면 필터 없음). 예: 0.5 → 점수 0.5 이상만 반환
     * @param userId    로그인 사용자 ID. 있으면 해당 사용자 성별과 같은 스타일만 추천 (User.gender)
     * @return 유사도 순 정렬된 (FittingTask, score) 리스트 (최대 10개)
     */
    @Transactional(readOnly = true)
    public List<FittingTaskWithScore> recommendByStyle(String userQuery, Double minScore, Long userId) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new BadRequestException("검색어를 입력해주세요.");
        }
        if (minScore != null && (minScore < 0 || minScore > 1)) {
            throw new BadRequestException("minScore는 0~1 사이여야 합니다.");
        }

        // 성별 필터: userId가 있으면 User에서 성별 조회
        String genderFilter = null;
        if (userId != null) {
            genderFilter = userRepository.findById(userId)
                .map(u -> u.getGender() != null ? u.getGender().name() : null)
                .orElse(null);
        }

        log.info("🔍 스타일 추천 검색 - 쿼리: {}, minScore: {}, genderFilter: {}", userQuery, minScore, genderFilter);

        // 1. 사용자 검색어를 RETRIEVAL_QUERY로 임베딩
        float[] queryEmbedding = geminiService.embedText(userQuery.trim(), "RETRIEVAL_QUERY");

        // 2. pgvector 포맷 문자열로 변환 "[0.1,0.2,...]"
        String queryVectorStr = toPgVectorString(queryEmbedding);

        // 3. maxDistance = 1 - minScore (코사인 거리. 낮을수록 유사, score=1-distance)
        Double maxDistance = minScore != null ? (1.0 - minScore) : null;

        // 4. 유사도 검색 (거리 + 성별 필터, 최대 10개)
        List<Object[]> idWithDistance = fittingRepository.findSimilarIdsWithDistance(
            queryVectorStr, maxDistance, genderFilter, DEFAULT_LIMIT
        );

        if (idWithDistance.isEmpty()) {
            log.info("✅ 스타일 추천 완료 - 조건에 맞는 결과 없음");
            return List.of();
        }

        // 5. id 목록으로 엔티티 조회 (순서 유지)
        List<Long> ids = idWithDistance.stream()
            .map(row -> ((Number) row[0]).longValue())
            .toList();
        Map<Long, FittingTask> taskMap = fittingRepository.findAllByIdInWithClothes(ids).stream()
            .collect(Collectors.toMap(FittingTask::getId, t -> t));

        // 6. (task, score) 조합. score = 1 - distance
        List<FittingTaskWithScore> results = new ArrayList<>();
        for (Object[] row : idWithDistance) {
            Long id = ((Number) row[0]).longValue();
            double distance = ((Number) row[1]).doubleValue();
            double score = 1.0 - distance;
            FittingTask task = taskMap.get(id);
            if (task != null) {
                results.add(new FittingTaskWithScore(task, Math.max(0, Math.min(1, score))));
            }
        }

        log.info("✅ 스타일 추천 완료 - {}건 반환 (minScore={}, genderFilter={})", results.size(), minScore, genderFilter);
        return results;
    }

    @Transactional(readOnly = true)
    public List<FittingTaskWithScore> recommendByWeatherStyle(String userQuery, Double minScore, Long userId, double temperature) {
        List<FittingTaskWithScore> baseResults = recommendByStyle(userQuery, minScore, userId);
        if (baseResults.isEmpty()) return baseResults;

        TempBucket bucket = TempBucket.from(temperature);

        return baseResults.stream()
                .map(r -> {
                    double bonus = calcWeatherBonus(r.getTask(), bucket);
                    double newScore = clamp01(r.getScore() + bonus);
                    return new FittingTaskWithScore(r.getTask(), newScore);
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .toList();
    }

    private enum TempBucket {
        HOT, MILD, COLD;

        static TempBucket from(double temp) {
            if (temp >= 25.0) return HOT;
            if (temp >= 15.0) return MILD;
            return COLD;
        }
    }

    private double calcWeatherBonus(FittingTask task, TempBucket bucket) {
        if (task == null) return 0.0;

        Clothes top = task.getTop();
        Clothes bottom = task.getBottom();

        String topSeason = norm(top != null ? top.getSeason() : null);
        String topThickness = norm(top != null ? top.getThickness() : null);
        String topSleeve = norm(top != null ? top.getSleeveType() : null);

        String botSeason = norm(bottom != null ? bottom.getSeason() : null);
        String botThickness = norm(bottom != null ? bottom.getThickness() : null);

        double bonus = 0.0;

        switch (bucket) {
            case HOT -> {
                bonus += matchAny(topSeason, "summer") ? 0.05 : 0.0;
                bonus += matchAny(botSeason, "summer") ? 0.03 : 0.0;
                bonus += matchAny(topThickness, "thin") ? 0.05 : 0.0;
                bonus += matchAny(botThickness, "thin") ? 0.03 : 0.0;
                bonus += matchAny(topSleeve, "short") ? 0.04 : 0.0;
            }
            case MILD -> {
                bonus += matchAny(topSeason, "spring", "fall", "autumn") ? 0.05 : 0.0;
                bonus += matchAny(botSeason, "spring", "fall", "autumn") ? 0.03 : 0.0;
                bonus += matchAny(topThickness, "medium") ? 0.05 : 0.0;
                bonus += matchAny(botThickness, "medium") ? 0.03 : 0.0;
            }
            case COLD -> {
                bonus += matchAny(topSeason, "winter") ? 0.06 : 0.0;
                bonus += matchAny(botSeason, "winter") ? 0.04 : 0.0;
                bonus += matchAny(topThickness, "thick") ? 0.06 : 0.0;
                bonus += matchAny(botThickness, "thick") ? 0.04 : 0.0;
                bonus += matchAny(topSleeve, "long") ? 0.03 : 0.0;
            }
        }

        return Math.min(0.15, bonus);
    }

    private String norm(String s) {
        return (s == null) ? "" : s.trim().toLowerCase();
    }

    private boolean matchAny(String value, String... tokens) {
        if (value == null || value.isEmpty()) return false;
        for (String t : tokens) {
            if (value.contains(t)) return true;
        }
        return false;
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private String toPgVectorString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new BadRequestException("Invalid embedding");
        }
        String values = IntStream.range(0, embedding.length)
                .mapToObj(i -> String.valueOf(embedding[i]))
                .collect(Collectors.joining(","));
        return "[" + values + "]";
    }
}
