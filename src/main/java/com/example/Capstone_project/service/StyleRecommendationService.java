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
import lombok.Getter;
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

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int WEATHER_CANDIDATE_LIMIT = 30;

    private final GeminiService geminiService;
    private final FittingRepository fittingRepository;
    private final ClothesRepository clothesRepository;
    private final UserRepository userRepository;

    // =========================================================================
    // 날씨 조건 DTO
    // 프론트에서 OpenWeatherMap 데이터를 받아 WeatherCondition 객체로 변환 후 전달
    // =========================================================================

    // 날씨 조건 DTO
    // 프론트에서 OpenWeatherMap 응답값을 그대로 넘겨주면 됨
    // temp: 기온 (섭씨)
    // rain: 강수량 (mm/h, 없으면 0.0)
    // snow: 적설량 (mm/h, 없으면 0.0)
    // windSpeed: 풍속 (m/s, 없으면 0.0)
    // humidity: 습도 (%, 없으면 0)

    @Getter
    public static class WeatherCondition {
        private final double temp;
        private final double rain;
        private final double snow;
        private final double windSpeed;
        private final int humidity;

        public WeatherCondition(double temp, double rain, double snow, double windSpeed, int humidity) {
            this.temp = temp;
            this.rain = rain;
            this.snow = snow;
            this.windSpeed = windSpeed;
            this.humidity = humidity;
        }

        public boolean isRaining()  { return rain > 0.0; }
        public boolean isSnowing()  { return snow > 0.0; }
        public boolean isWindy()    { return windSpeed > 7.0; }  // 7m/s 이상 강풍
        public boolean isHumid()    { return humidity > 80; }    // 습도 80% 이상
    }

    // =========================================================================
    // 기온 버킷 (5단계로 세분화)
    // =========================================================================

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

    // =========================================================================
    // 기존 추천 메서드들 (변경 없음)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<FittingTaskWithScore> recommendByStyle(String userQuery, Double minScore, Long userId) {
        String genderFilter = null;
        if (userId != null) {
            genderFilter = userRepository.findById(userId)
                    .map(u -> u.getGender() != null ? u.getGender().name() : null)
                    .orElse(null);
        }
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

    // =========================================================================
    // 날씨 기반 추천 (기존 temp만 받던 메서드 → 하위 호환용으로 유지)
    // =========================================================================

    /**
     * 기온만 받는 기존 메서드 (하위 호환용)
     * 프론트 팀원이 추가 데이터 연동 완료 전까지 사용
     */
    @Transactional(readOnly = true)
    public List<FittingTaskWithScore> recommendByWeatherStyle(String userQuery, Double minScore, Long userId, double temperature) {
        WeatherCondition condition = new WeatherCondition(temperature, 0.0, 0.0, 0.0, 0);
        return recommendByWeatherStyleFull(userQuery, minScore, userId, condition);
    }

    /**
     * 날씨 전체 조건 기반 추천 (강수량, 적설량, 풍속, 습도 포함)
     * 프론트 팀원이 추가 데이터 연동 완료 후 컨트롤러에서 이 메서드로 교체
     *
     * [흐름]
     * 1. 유사도 후보 30개 조회 (pgvector, 전체 DB 훑지 않음)
     * 2. 날씨 보너스 적용 (기온 5단계 + 비/눈/바람/습도)
     * 3. 보너스 포함 점수로 재정렬
     * 4. 상위 10개 반환
     */
    @Transactional(readOnly = true)
    public List<FittingTaskWithScore> recommendByWeatherStyleFull(String userQuery, Double minScore, Long userId, WeatherCondition condition) {
        TempBucket bucket = TempBucket.from(condition.getTemp());
        log.info("🌤️ 날씨 추천 시작 - temp: {}°C, bucket: {}, rain: {}, snow: {}, wind: {}m/s, humidity: {}%",
                condition.getTemp(), bucket, condition.getRain(), condition.getSnow(),
                condition.getWindSpeed(), condition.getHumidity());

        String genderFilter = null;
        if (userId != null) {
            genderFilter = userRepository.findById(userId)
                    .map(u -> u.getGender() != null ? u.getGender().name() : null)
                    .orElse(null);
        }

        List<FittingTaskWithScore> candidates = recommendInternal(userQuery, minScore, genderFilter, WEATHER_CANDIDATE_LIMIT, null, false);
        if (candidates.isEmpty()) return candidates;

        List<FittingTaskWithScore> results = candidates.stream()
                .map(r -> {
                    double bonus = calcWeatherBonus(r.getTask(), bucket, condition);
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

    // =========================================================================
    // 날씨 보너스 계산
    //
    // 보너스 상한: 0.20 (기온 + 날씨 상태 합산)
    // 기온 보너스 상한: 0.15 / 날씨 상태 보너스 상한: 0.10
    // =========================================================================

    private double calcWeatherBonus(FittingTask task, TempBucket bucket, WeatherCondition condition) {
        if (task == null) return 0.0;

        Clothes top    = task.getTop();
        Clothes bottom = task.getBottom();

        String topSeason    = norm(top    != null ? top.getSeason()    : null);
        String topThickness = norm(top    != null ? top.getThickness()  : null);
        String topSleeve    = norm(top    != null ? top.getSleeveType() : null);
        String topMaterial  = norm(top    != null ? top.getMaterial()   : null);
        String topCategory  = norm(top    != null ? top.getCategory()   : null);
        String botSeason    = norm(bottom != null ? bottom.getSeason()    : null);
        String botThickness = norm(bottom != null ? bottom.getThickness() : null);

        double tempBonus    = calcTempBonus(bucket, topSeason, topThickness, topSleeve, botSeason, botThickness);
        double weatherBonus = calcWeatherStateBonus(condition, topMaterial, topCategory, topThickness);

        return Math.min(0.20, tempBonus + weatherBonus);
    }

    /**
     * 기온 보너스 (5단계)
     */
    private double calcTempBonus(TempBucket bucket,
                                 String topSeason, String topThickness, String topSleeve,
                                 String botSeason, String botThickness) {
        double bonus = 0.0;

        switch (bucket) {
            case VERY_HOT -> {
                // 30°C 이상: 민소매, 극초박 소재
                bonus += matchAny(topSeason,    "summer")              ? 0.06 : 0.0;
                bonus += matchAny(botSeason,    "summer")              ? 0.04 : 0.0;
                bonus += matchAny(topThickness, "thin")                ? 0.05 : 0.0;
                bonus += matchAny(botThickness, "thin")                ? 0.03 : 0.0;
                bonus += matchAny(topSleeve,    "sleeveless")          ? 0.05 : 0.0;
                bonus += matchAny(topSleeve,    "short")               ? 0.02 : 0.0;
            }
            case HOT -> {
                // 23~30°C: 반팔, 얇은 소재
                bonus += matchAny(topSeason,    "summer")              ? 0.05 : 0.0;
                bonus += matchAny(botSeason,    "summer")              ? 0.03 : 0.0;
                bonus += matchAny(topThickness, "thin")                ? 0.05 : 0.0;
                bonus += matchAny(botThickness, "thin")                ? 0.03 : 0.0;
                bonus += matchAny(topSleeve,    "short", "sleeveless") ? 0.04 : 0.0;
            }
            case MILD -> {
                // 15~23°C: 봄가을, 중간 두께
                bonus += matchAny(topSeason,    "spring", "fall")      ? 0.05 : 0.0;
                bonus += matchAny(botSeason,    "spring", "fall")      ? 0.03 : 0.0;
                bonus += matchAny(topThickness, "medium")              ? 0.05 : 0.0;
                bonus += matchAny(botThickness, "medium")              ? 0.03 : 0.0;
                bonus += matchAny(topSleeve,    "long")                ? 0.02 : 0.0;
            }
            case COLD -> {
                // 5~15°C: 겨울, 두꺼운 소재
                bonus += matchAny(topSeason,    "winter")              ? 0.05 : 0.0;
                bonus += matchAny(botSeason,    "winter")              ? 0.03 : 0.0;
                bonus += matchAny(topThickness, "thick")               ? 0.05 : 0.0;
                bonus += matchAny(botThickness, "thick")               ? 0.03 : 0.0;
                bonus += matchAny(topSleeve,    "long")                ? 0.03 : 0.0;
            }
            case VERY_COLD -> {
                // 5°C 미만: 극한 방한
                bonus += matchAny(topSeason,    "winter")              ? 0.06 : 0.0;
                bonus += matchAny(botSeason,    "winter")              ? 0.04 : 0.0;
                bonus += matchAny(topThickness, "thick")               ? 0.06 : 0.0;
                bonus += matchAny(botThickness, "thick")               ? 0.04 : 0.0;
                bonus += matchAny(topSleeve,    "long")                ? 0.03 : 0.0;
            }
        }

        return Math.min(0.15, bonus);
    }

    /**
     * 날씨 상태 보너스 (비/눈/바람/습도)
     * 소재(material), 카테고리(category), 두께(thickness) 기반으로 판단
     */
    private double calcWeatherStateBonus(WeatherCondition condition,
                                         String topMaterial, String topCategory, String topThickness) {
        double bonus = 0.0;

        if (condition.isRaining()) {
            // 비: 방수 소재(나일론, 폴리에스터), 아우터 보너스
            bonus += matchAny(topMaterial, "나일론", "폴리에스터", "방수", "nylon", "polyester", "waterproof") ? 0.05 : 0.0;
            bonus += matchAny(topCategory, "아우터", "outer", "jacket") ? 0.04 : 0.0;
        }

        if (condition.isSnowing()) {
            // 눈: 두꺼운 소재, 아우터, 방한 소재 보너스
            bonus += matchAny(topThickness, "thick")                                   ? 0.05 : 0.0;
            bonus += matchAny(topMaterial,  "울", "플리스", "wool", "fleece", "fur")   ? 0.04 : 0.0;
            bonus += matchAny(topCategory,  "아우터", "outer", "jacket", "coat")       ? 0.04 : 0.0;
        }

        if (condition.isWindy()) {
            // 강풍: 바람막이, 아우터 보너스
            bonus += matchAny(topCategory, "아우터", "outer", "jacket", "coat")        ? 0.04 : 0.0;
            bonus += matchAny(topMaterial, "나일론", "방풍", "nylon", "windproof")     ? 0.04 : 0.0;
        }

        if (condition.isHumid()) {
            // 고습도: 린넨, 얇은 소재 보너스
            bonus += matchAny(topMaterial, "린넨", "면", "linen", "cotton")            ? 0.04 : 0.0;
            bonus += matchAny(topThickness, "thin")                                    ? 0.03 : 0.0;
        }

        return Math.min(0.10, bonus);
    }

    // =========================================================================
    // 내부 추천 로직 (기존 코드 유지)
    // =========================================================================

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
        Map<Long, FittingTask> taskMap = fittingRepository.findAllByIdInWithClothes(ids).stream()
                .collect(Collectors.toMap(FittingTask::getId, t -> t));

        List<FittingTaskWithScore> results = new ArrayList<>();
        for (Object[] row : idWithDistance) {
            Long id       = ((Number) row[0]).longValue();
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

    // =========================================================================
    // 유틸
    // =========================================================================

    /**
     * null-safe 정규화 + 한국어 → 영어 매핑
     * DB에 한국어로 저장되므로 영어 토큰과 비교하기 위해 변환
     */
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