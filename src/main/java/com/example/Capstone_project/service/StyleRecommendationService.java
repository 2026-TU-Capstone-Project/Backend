package com.example.Capstone_project.service;

import com.example.Capstone_project.common.exception.BadRequestException;
import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.domain.Gender;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.dto.ClothesRecommendationResponse;
import com.example.Capstone_project.dto.ClothesResponseDto;
import com.example.Capstone_project.dto.FittingTaskWithScore;
import com.example.Capstone_project.dto.WeatherConditionDto;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class StyleRecommendationService {

    private static final int DEFAULT_LIMIT          = 10;
    private static final int MAX_LIMIT              = 50;
    private static final int WEATHER_CANDIDATE_LIMIT = 30;

    private final GeminiService geminiService;
    private final FittingRepository fittingRepository;
    private final ClothesRepository clothesRepository;
    private final UserRepository userRepository;

    // 기온 버킷 (5단계)
    private enum TempBucket {
        VERY_HOT,  // 30°C 이상
        HOT,       // 23~30°C
        MILD,      // 15~23°C
        COLD,      // 5~15°C
        VERY_COLD; // 5°C 미만

        static TempBucket from(double temp) {
            if (temp >= 30.0) return VERY_HOT;
            if (temp >= 23.0) return HOT;
            if (temp >= 15.0) return MILD;
            if (temp >= 5.0)  return COLD;
            return VERY_COLD;
        }
    }

    // 기존 추천 메서드들

    @Transactional(readOnly = true)
    public List<FittingTaskWithScore> recommendByStyle(String userQuery, Double minScore, Long userId) {
        String genderFilter = resolveGenderFilter(userId);
        return recommendInternal(userQuery, minScore, genderFilter, DEFAULT_LIMIT, null, false);
    }

    @Transactional(readOnly = true)
    public List<FittingTaskWithScore> recommendByStyleWithFilters(String userQuery, Double minScore, String gender, Integer limit, Long userIdForMyCloset) {
        return recommendInternal(userQuery, minScore, parseGenderOrNull(gender), resolveLimit(limit), userIdForMyCloset, false);
    }

    @Transactional(readOnly = true)
    public List<FittingTaskWithScore> recommendFromFeed(String userQuery, Double minScore, String gender, Integer limit) {
        return recommendInternal(userQuery, minScore, parseGenderOrNull(gender), resolveLimit(limit), null, true);
    }

    @Transactional(readOnly = true)
    public ClothesRecommendationResponse recommendTopsByStyle(String userQuery, Double minScore, String gender, Integer limit) {
        List<FittingTaskWithScore> tasks = recommendInternal(userQuery, minScore, parseGenderOrNull(gender), MAX_LIMIT, null, false);
        return extractClothesByCategory(tasks, true, resolveLimit(limit));
    }

    @Transactional(readOnly = true)
    public ClothesRecommendationResponse recommendBottomsByStyle(String userQuery, Double minScore, String gender, Integer limit) {
        List<FittingTaskWithScore> tasks = recommendInternal(userQuery, minScore, parseGenderOrNull(gender), MAX_LIMIT, null, false);
        return extractClothesByCategory(tasks, false, resolveLimit(limit));
    }

    @Transactional(readOnly = true)
    public ClothesRecommendationResponse recommendTopsByStyleFromMyCloset(String userQuery, Double minScore, String gender, Integer limit, Long userId) {
        List<FittingTaskWithScore> tasks = recommendInternal(userQuery, minScore, parseGenderOrNull(gender), MAX_LIMIT, userId, false);
        return extractClothesByCategory(tasks, true, resolveLimit(limit));
    }

    @Transactional(readOnly = true)
    public ClothesRecommendationResponse recommendBottomsByStyleFromMyCloset(String userQuery, Double minScore, String gender, Integer limit, Long userId) {
        List<FittingTaskWithScore> tasks = recommendInternal(userQuery, minScore, parseGenderOrNull(gender), MAX_LIMIT, userId, false);
        return extractClothesByCategory(tasks, false, resolveLimit(limit));
    }

    /**
     * 날씨 기반 스타일 추천
     *
     * 흐름:
     * 1. pgvector 유사도로 후보 30개 조회
     * 2. 날씨 보너스 적용 (기온 5단계 + 비/눈/바람/습도)
     * 3. 보너스 포함 점수로 재정렬 → 상위 10개 반환
     */
    @Transactional(readOnly = true)
    public List<FittingTaskWithScore> recommendByWeather(String userQuery, Double minScore, Long userId, WeatherConditionDto condition) {
        TempBucket bucket = TempBucket.from(condition.getTemp());
        log.info("🌤️ 날씨 추천 시작 - temp: {}°C, bucket: {}, rain: {}, snow: {}, wind: {}m/s, humidity: {}%",
                condition.getTemp(), bucket, condition.getRain(), condition.getSnow(),
                condition.getWindSpeed(), condition.getHumidity());

        List<FittingTaskWithScore> candidates = recommendInternal(
                userQuery, minScore, resolveGenderFilter(userId), WEATHER_CANDIDATE_LIMIT, null, false);

        if (candidates.isEmpty()) return candidates;

        List<FittingTaskWithScore> results = candidates.stream()
                .map(r -> {
                    double bonus    = calcWeatherBonus(r.getTask(), bucket, condition);
                    double newScore = clamp01(r.getScore() + bonus);
                    log.debug("📊 task={}, score={}, bonus={}, newScore={}", r.getTask().getId(), r.getScore(), bonus, newScore);
                    return new FittingTaskWithScore(r.getTask(), newScore);
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(DEFAULT_LIMIT)
                .toList();

        log.info("✅ 날씨 추천 완료 - bucket: {}, 후보: {}개 → 반환: {}개", bucket, candidates.size(), results.size());
        return results;
    }

    // 날씨 보너스 계산 (보너스 상한: 0.20)

    private double calcWeatherBonus(FittingTask task, TempBucket bucket, WeatherConditionDto condition) {
        if (task == null) return 0.0;

        Clothes top    = task.getTop();
        Clothes bottom = task.getBottom();

        double tempBonus    = calcTempBonus(bucket, top, bottom);
        double weatherBonus = calcWeatherStateBonus(condition, top);

        return Math.min(0.20, tempBonus + weatherBonus);
    }

    // 기온 보너스 (5단계, 상한 0.15)
    private double calcTempBonus(TempBucket bucket, Clothes top, Clothes bottom) {
        String topSeason    = norm(top    != null ? top.getSeason()    : null);
        String topThickness = norm(top    != null ? top.getThickness()  : null);
        String topSleeve    = norm(top    != null ? top.getSleeveType() : null);
        String botSeason    = norm(bottom != null ? bottom.getSeason()    : null);
        String botThickness = norm(bottom != null ? bottom.getThickness() : null);

        double bonus = switch (bucket) {
            case VERY_HOT -> {
                double b = 0.0;
                b += matchAny(topSeason,    "summer")              ? 0.06 : 0.0;
                b += matchAny(botSeason,    "summer")              ? 0.04 : 0.0;
                b += matchAny(topThickness, "thin")                ? 0.05 : 0.0;
                b += matchAny(botThickness, "thin")                ? 0.03 : 0.0;
                b += matchAny(topSleeve,    "sleeveless")          ? 0.05 : 0.0;
                b += matchAny(topSleeve,    "short")               ? 0.02 : 0.0;
                yield b;
            }
            case HOT -> {
                double b = 0.0;
                b += matchAny(topSeason,    "summer")              ? 0.05 : 0.0;
                b += matchAny(botSeason,    "summer")              ? 0.03 : 0.0;
                b += matchAny(topThickness, "thin")                ? 0.05 : 0.0;
                b += matchAny(botThickness, "thin")                ? 0.03 : 0.0;
                b += matchAny(topSleeve,    "short", "sleeveless") ? 0.04 : 0.0;
                yield b;
            }
            case MILD -> {
                double b = 0.0;
                b += matchAny(topSeason,    "spring", "fall")      ? 0.05 : 0.0;
                b += matchAny(botSeason,    "spring", "fall")      ? 0.03 : 0.0;
                b += matchAny(topThickness, "medium")              ? 0.05 : 0.0;
                b += matchAny(botThickness, "medium")              ? 0.03 : 0.0;
                b += matchAny(topSleeve,    "long")                ? 0.02 : 0.0;
                yield b;
            }
            case COLD -> {
                double b = 0.0;
                b += matchAny(topSeason,    "winter")              ? 0.05 : 0.0;
                b += matchAny(botSeason,    "winter")              ? 0.03 : 0.0;
                b += matchAny(topThickness, "thick")               ? 0.05 : 0.0;
                b += matchAny(botThickness, "thick")               ? 0.03 : 0.0;
                b += matchAny(topSleeve,    "long")                ? 0.03 : 0.0;
                yield b;
            }
            case VERY_COLD -> {
                double b = 0.0;
                b += matchAny(topSeason,    "winter")              ? 0.06 : 0.0;
                b += matchAny(botSeason,    "winter")              ? 0.04 : 0.0;
                b += matchAny(topThickness, "thick")               ? 0.06 : 0.0;
                b += matchAny(botThickness, "thick")               ? 0.04 : 0.0;
                b += matchAny(topSleeve,    "long")                ? 0.03 : 0.0;
                yield b;
            }
        };

        return Math.min(0.15, bonus);
    }

    // 날씨 상태 보너스 (비/눈/바람/습도, 상한 0.10)
    private double calcWeatherStateBonus(WeatherConditionDto condition, Clothes top) {
        String topMaterial  = norm(top != null ? top.getMaterial()  : null);
        String topCategory  = norm(top != null ? top.getCategory()  : null);
        String topThickness = norm(top != null ? top.getThickness() : null);

        double bonus = 0.0;

        if (condition.isRaining()) {
            bonus += matchAny(topMaterial, "나일론", "폴리에스터", "방수", "nylon", "polyester", "waterproof") ? 0.05 : 0.0;
            bonus += matchAny(topCategory, "아우터", "outer", "jacket")                                     ? 0.04 : 0.0;
        }
        if (condition.isSnowing()) {
            bonus += matchAny(topThickness, "thick")                                  ? 0.05 : 0.0;
            bonus += matchAny(topMaterial,  "울", "플리스", "wool", "fleece", "fur")  ? 0.04 : 0.0;
            bonus += matchAny(topCategory,  "아우터", "outer", "jacket", "coat")      ? 0.04 : 0.0;
        }
        if (condition.isWindy()) {
            bonus += matchAny(topCategory, "아우터", "outer", "jacket", "coat")       ? 0.04 : 0.0;
            bonus += matchAny(topMaterial, "나일론", "방풍", "nylon", "windproof")    ? 0.04 : 0.0;
        }
        if (condition.isHumid()) {
            bonus += matchAny(topMaterial,  "린넨", "면", "linen", "cotton")          ? 0.04 : 0.0;
            bonus += matchAny(topThickness, "thin")                                   ? 0.03 : 0.0;
        }

        return Math.min(0.10, bonus);
    }

    // 내부 추천 로직

    private List<FittingTaskWithScore> recommendInternal(String userQuery, Double minScore, String genderFilter, int limit, Long userIdForMyCloset, boolean fromFeedOnly) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new BadRequestException("검색어를 입력해주세요.");
        }
        if (minScore != null && (minScore < 0 || minScore > 1)) {
            throw new BadRequestException("minScore는 0~1 사이여야 합니다.");
        }

        String normalizedQuery = userQuery.trim();
        log.info("🔍 스타일 추천 - 쿼리: {}, minScore: {}, gender: {}, limit: {}, myCloset: {}, feedOnly: {}",
                normalizedQuery, minScore, genderFilter, limit, userIdForMyCloset, fromFeedOnly);

        float[] queryEmbedding = geminiService.embedText(normalizedQuery, "RETRIEVAL_QUERY");
        String queryVectorStr  = toPgVectorString(queryEmbedding);
        Double maxDistance     = minScore != null ? (1.0 - minScore) : null;

        List<Object[]> idWithDistance;
        if (fromFeedOnly) {
            idWithDistance = fittingRepository.findSimilarIdsWithDistanceFromFeed(queryVectorStr, maxDistance, genderFilter, limit);
        } else if (userIdForMyCloset != null) {
            idWithDistance = fittingRepository.findSimilarIdsWithDistanceByUser(queryVectorStr, maxDistance, genderFilter, userIdForMyCloset, limit);
        } else {
            idWithDistance = fittingRepository.findSimilarIdsWithDistance(queryVectorStr, maxDistance, genderFilter, limit);
        }

        if (idWithDistance.isEmpty()) {
            log.info("✅ 스타일 추천 완료 - 결과 없음");
            return List.of();
        }

        List<Long> ids = idWithDistance.stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();

        List<FittingTask> loadedTasks;
        try {
            loadedTasks = fittingRepository.findAllByIdInWithClothes(ids);
        } catch (Exception e) {
            log.warn("배치 로드 실패, 개별 로드로 전환: {}", e.getMessage());
            loadedTasks = new ArrayList<>();
            for (Long id : ids) {
                try {
                    fittingRepository.findByIdWithClothes(id).ifPresent(loadedTasks::add);
                } catch (Exception ex) {
                    log.warn("FittingTask {} 스킵 (삭제된 옷 참조): {}", id, ex.getMessage());
                }
            }
        }

        Map<Long, FittingTask> taskMap = loadedTasks.stream()
                .collect(Collectors.toMap(FittingTask::getId, t -> t));

        List<FittingTaskWithScore> results = new ArrayList<>();
        for (Object[] row : idWithDistance) {
            Long id         = ((Number) row[0]).longValue();
            double distance = ((Number) row[1]).doubleValue();
            double score    = 1.0 - distance;
            FittingTask task = taskMap.get(id);
            if (task != null) {
                results.add(new FittingTaskWithScore(task, Math.max(0, Math.min(1, score))));
            }
        }

        log.info("✅ 스타일 추천 완료 - {}건 반환", results.size());
        return results;
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

        if (orderedIds.isEmpty()) return ClothesRecommendationResponse.builder().items(List.of()).build();

        Map<Long, Clothes> clothesMap = clothesRepository.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(Clothes::getId, c -> c));

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

    // 유틸 메서드

    /** 한국어 → 영어 정규화 (DB에 한국어로 저장되므로 영어 토큰과 비교하기 위해 변환) */
    private String norm(String s) {
        if (s == null) return "";
        return switch (s.trim()) {
            case "봄"    -> "spring";
            case "여름"  -> "summer";
            case "가을"  -> "fall";
            case "겨울"  -> "winter";
            case "사계절" -> "all-season";
            case "얇음"  -> "thin";
            case "보통"  -> "medium";
            case "두꺼움" -> "thick";
            case "반팔"  -> "short";
            case "긴팔"  -> "long";
            case "민소매" -> "sleeveless";
            case "없음"  -> "none";
            case "상의"  -> "top";
            case "하의"  -> "bottom";
            case "아우터" -> "outer";
            default     -> s.trim().toLowerCase();
        };
    }

    private boolean matchAny(String value, String... tokens) {
        if (value == null || value.isEmpty()) return false;
        for (String t : tokens) {
            if (value.contains(t.toLowerCase())) return true;
        }
        return false;
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private String resolveGenderFilter(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(u -> u.getGender() != null ? u.getGender().name() : null)
                .orElse(null);
    }

    private String parseGenderOrNull(String gender) {
        if (gender == null || gender.isBlank()) return null;
        try {
            return Gender.valueOf(gender.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("gender는 MALE 또는 FEMALE 이어야 합니다.");
        }
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        if (limit <= 0) throw new BadRequestException("limit은 1 이상이어야 합니다.");
        return Math.min(limit, MAX_LIMIT);
    }

    private String toPgVectorString(float[] embedding) {
        if (embedding == null || embedding.length == 0) throw new BadRequestException("Invalid embedding");
        String values = IntStream.range(0, embedding.length)
                .mapToObj(i -> String.valueOf(embedding[i]))
                .collect(Collectors.joining(","));
        return "[" + values + "]";
    }
}