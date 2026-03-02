package com.example.Capstone_project.service;

import com.example.Capstone_project.common.exception.BadRequestException;
import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.domain.Gender;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.dto.ClothesRecommendationResponse;
import com.example.Capstone_project.dto.ClothesResponseDto;
import com.example.Capstone_project.dto.FittingTaskWithScore;
import com.example.Capstone_project.repository.ClothesRepository;
import com.example.Capstone_project.repository.FittingRepository;
import com.example.Capstone_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private static final int MAX_LIMIT = 50;

    private final GeminiService geminiService;
    private final FittingRepository fittingRepository;
    private final ClothesRepository clothesRepository;
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
        // 성별 필터: userId가 있으면 User에서 성별 조회
        String genderFilter = null;
        if (userId != null) {
            genderFilter = userRepository.findById(userId)
                    .map(u -> u.getGender() != null ? u.getGender().name() : null)
                    .orElse(null);
        }
        return recommendInternal(userQuery, minScore, genderFilter, DEFAULT_LIMIT, null, false);
    }

    /**
     * 검색어·성별·limit 지정 스타일 추천 (챗봇 Gemini function calling 및 외부 API 공용).
     * userIdForMyCloset가 null이면 전체, 널이 아니면 해당 사용자 옷장만 검색.
     *
     * @param userQuery         예: "결혼식에 입고 갈 단정하고 깔끔한 스타일 추천해줘"
     * @param minScore          최소 유사도 점수 (0~1, null이면 필터 없음)
     * @param gender            성별 필터 (MALE/FEMALE, 대소문자 무관, null/blank면 필터 없음)
     * @param limit             최대 개수 (기본 10, 최대 50)
     * @param userIdForMyCloset  null이면 전체 피팅 검색, 널이 아니면 해당 사용자(user_id) 피팅만 검색 (내 옷장)
     */
    @Transactional(readOnly = true)
    public List<FittingTaskWithScore> recommendByStyleWithFilters(String userQuery, Double minScore, String gender, Integer limit, Long userIdForMyCloset) {
        String genderFilter = parseGenderOrNull(gender);
        int resolvedLimit = resolveLimit(limit);
        return recommendInternal(userQuery, minScore, genderFilter, resolvedLimit, userIdForMyCloset, false);
    }

    /**
     * 피드 전용: 피드에 올라온 코디만 대상으로 스타일 유사도 추천.
     */
    @Transactional(readOnly = true)
    public List<FittingTaskWithScore> recommendFromFeed(String userQuery, Double minScore, String gender, Integer limit) {
        String genderFilter = parseGenderOrNull(gender);
        int resolvedLimit = resolveLimit(limit);
        return recommendInternal(userQuery, minScore, genderFilter, resolvedLimit, null, true);
    }

    /**
     * 상의만 추천: 스타일 추천과 동일한 유사도 검색 후, 추천된 피팅 결과에서 상의만 추출해 유사도 순으로 반환. (챗봇·외부 API 공용)
     */
    @Transactional(readOnly = true)
    public ClothesRecommendationResponse recommendTopsByStyle(String userQuery, Double minScore, String gender, Integer limit) {
        int resolvedLimit = resolveLimit(limit);
        List<FittingTaskWithScore> tasks = recommendInternal(userQuery, minScore, parseGenderOrNull(gender), MAX_LIMIT, null, false);
        return extractClothesByCategory(tasks, true, resolvedLimit);
    }

    /**
     * 하의만 추천: 스타일 추천과 동일한 유사도 검색 후, 추천된 피팅 결과에서 하의만 추출해 유사도 순으로 반환. (챗봇·외부 API 공용)
     */
    @Transactional(readOnly = true)
    public ClothesRecommendationResponse recommendBottomsByStyle(String userQuery, Double minScore, String gender, Integer limit) {
        int resolvedLimit = resolveLimit(limit);
        List<FittingTaskWithScore> tasks = recommendInternal(userQuery, minScore, parseGenderOrNull(gender), MAX_LIMIT, null, false);
        return extractClothesByCategory(tasks, false, resolvedLimit);
    }

    /**
     * 내 옷장 전용 - 상의만 추천 (티셔츠, 셔츠 등). 해당 사용자 피팅 결과에서만 검색.
     */
    @Transactional(readOnly = true)
    public ClothesRecommendationResponse recommendTopsByStyleFromMyCloset(String userQuery, Double minScore, String gender, Integer limit, Long userId) {
        int resolvedLimit = resolveLimit(limit);
        List<FittingTaskWithScore> tasks = recommendInternal(userQuery, minScore, parseGenderOrNull(gender), MAX_LIMIT, userId, false);
        return extractClothesByCategory(tasks, true, resolvedLimit);
    }

    /**
     * 내 옷장 전용 - 하의만 추천. 해당 사용자 피팅 결과에서만 검색.
     */
    @Transactional(readOnly = true)
    public ClothesRecommendationResponse recommendBottomsByStyleFromMyCloset(String userQuery, Double minScore, String gender, Integer limit, Long userId) {
        int resolvedLimit = resolveLimit(limit);
        List<FittingTaskWithScore> tasks = recommendInternal(userQuery, minScore, parseGenderOrNull(gender), MAX_LIMIT, userId, false);
        return extractClothesByCategory(tasks, false, resolvedLimit);
    }

    private ClothesRecommendationResponse extractClothesByCategory(List<FittingTaskWithScore> tasks, boolean isTop, int limit) {
        Map<Long, Double> idToBestScore = new LinkedHashMap<>();
        for (FittingTaskWithScore tws : tasks) {
            Long clothesId = isTop ? tws.getTask().getTopId() : tws.getTask().getBottomId();
            if (clothesId == null) continue;
            idToBestScore.merge(clothesId, tws.getScore(), Math::max);
        }
        List<Long> orderedIds = idToBestScore.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Long, Double>>comparingDouble(Map.Entry::getValue).reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
        if (orderedIds.isEmpty()) {
            return ClothesRecommendationResponse.builder().items(List.of()).build();
        }
        Map<Long, Clothes> clothesMap = clothesRepository.findAllById(orderedIds).stream().collect(Collectors.toMap(Clothes::getId, c -> c));
        List<ClothesRecommendationResponse.ClothesRecommendationItem> items = orderedIds.stream()
                .map(id -> {
                    Clothes c = clothesMap.get(id);
                    if (c == null) return null;
                    Double score = idToBestScore.get(id);
                    return ClothesRecommendationResponse.ClothesRecommendationItem.builder()
                            .clothes(ClothesResponseDto.from(c))
                            .score(score != null ? Math.round(score * 100.0) / 100.0 : null)
                            .build();
                })
                .filter(item -> item != null)
                .toList();
        return ClothesRecommendationResponse.builder().items(items).build();
    }

    private List<FittingTaskWithScore> recommendInternal(String userQuery, Double minScore, String genderFilter, int limit, Long userIdForMyCloset, boolean fromFeedOnly) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new BadRequestException("검색어를 입력해주세요.");
        }
        if (minScore != null && (minScore < 0 || minScore > 1)) {
            throw new BadRequestException("minScore는 0~1 사이여야 합니다.");
        }

        String normalizedQuery = userQuery.trim();
        log.info("🔍 스타일 추천 검색 - 쿼리: {}, minScore: {}, genderFilter: {}, limit: {}, myCloset: {}, feedOnly: {}", normalizedQuery, minScore, genderFilter, limit, userIdForMyCloset, fromFeedOnly);

        // 1. 사용자 검색어를 RETRIEVAL_QUERY로 임베딩
        float[] queryEmbedding = geminiService.embedText(normalizedQuery, "RETRIEVAL_QUERY");

        // 2. pgvector 포맷 문자열로 변환 "[0.1,0.2,...]"
        String queryVectorStr = toPgVectorString(queryEmbedding);

        // 3. maxDistance = 1 - minScore (코사인 거리. 낮을수록 유사, score=1-distance)
        Double maxDistance = minScore != null ? (1.0 - minScore) : null;

        // 4. 유사도 검색 (내 옷장 / 피드 / 전체)
        List<Object[]> idWithDistance;
        if (fromFeedOnly) {
            idWithDistance = fittingRepository.findSimilarIdsWithDistanceFromFeed(queryVectorStr, maxDistance, genderFilter, limit);
        } else if (userIdForMyCloset != null) {
            idWithDistance = fittingRepository.findSimilarIdsWithDistanceByUser(queryVectorStr, maxDistance, genderFilter, userIdForMyCloset, limit);
        } else {
            idWithDistance = fittingRepository.findSimilarIdsWithDistance(queryVectorStr, maxDistance, genderFilter, limit);
        }

        if (idWithDistance.isEmpty()) {
            log.info("✅ 스타일 추천 완료 - 조건에 맞는 결과 없음");
            return List.of();
        }

        // 5. id 목록으로 엔티티 조회 (순서 유지)
        List<Long> ids = idWithDistance.stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();
        Map<Long, FittingTask> taskMap = fittingRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(FittingTask::getId, t -> t));
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

        log.info("✅ 스타일 추천 완료 - {}건 반환 (minScore={}, genderFilter={}, limit={}, myCloset={}, feedOnly={})", results.size(), minScore, genderFilter, limit, userIdForMyCloset, fromFeedOnly);
        return results;
    }

    private String parseGenderOrNull(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        String normalized = gender.trim().toUpperCase();
        try {
            return Gender.valueOf(normalized).name();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("gender는 MALE 또는 FEMALE 이어야 합니다.");
        }
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0) {
            throw new BadRequestException("limit은 1 이상이어야 합니다.");
        }
        if (limit > MAX_LIMIT) {
            return MAX_LIMIT;
        }
        return limit;

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
