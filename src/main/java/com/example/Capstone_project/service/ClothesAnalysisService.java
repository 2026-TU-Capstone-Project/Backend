package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.dto.ClothesRequestDto;
import com.example.Capstone_project.repository.ClothesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAnalysisService {

    private final GoogleVisionService googleVisionService;
    private final ClothesRepository clothesRepository;
    private final GoogleCloudStorageService gcsService;

    // 1. 한국어 -> 영어 변환 사전 (형님 코드 분석 완료)
    private static final Map<String, String> DICTIONARY = new HashMap<>();

    // 2. 영어 -> 한국어 변환 사전 (DB 저장용)
    private static final Map<String, String> ENGLISH_TO_KOREAN = new HashMap<>();

    static {
        // === [Category: 상의/하의/아우터 종류] ===
        DICTIONARY.put("후드", "Hoodie");
        DICTIONARY.put("후드티", "Hoodie");
        DICTIONARY.put("맨투맨", "Sweatshirt");
        DICTIONARY.put("니트", "Knitwear");
        DICTIONARY.put("스웨터", "Sweater");
        DICTIONARY.put("셔츠", "Shirt");
        DICTIONARY.put("티셔츠", "T-shirt");
        DICTIONARY.put("코트", "Coat");
        DICTIONARY.put("패딩", "Padding");
        DICTIONARY.put("재킷", "Jacket");
        DICTIONARY.put("블레이저", "Blazer");
        DICTIONARY.put("청바지", "Jeans");
        DICTIONARY.put("데님팬츠", "Jeans");
        DICTIONARY.put("반바지", "Shorts");
        DICTIONARY.put("치마", "Skirt");
        DICTIONARY.put("원피스", "Dress");

        // === [Material: 소재] (형님 코드 로직 반영) ===
        DICTIONARY.put("데님", "Denim");
        DICTIONARY.put("청", "Denim");
        DICTIONARY.put("가죽", "Leather");
        DICTIONARY.put("레더", "Leather");
        DICTIONARY.put("퍼", "Fur");
        DICTIONARY.put("털", "Fur");
        DICTIONARY.put("실크", "Silk");
        DICTIONARY.put("트위드", "Tweed");
        DICTIONARY.put("벨벳", "Velvet");
        DICTIONARY.put("울", "Wool");
        DICTIONARY.put("린넨", "Linen");
        DICTIONARY.put("나일론", "Nylon");
        DICTIONARY.put("면", "Cotton");
        DICTIONARY.put("코튼", "Cotton");

        // === [Pattern: 무늬] ===
        DICTIONARY.put("줄무늬", "Stripe");
        DICTIONARY.put("스트라이프", "Stripe");
        DICTIONARY.put("체크", "Check");
        DICTIONARY.put("플라워", "Floral");
        DICTIONARY.put("꽃무늬", "Floral");
        DICTIONARY.put("땡땡이", "Dot");
        DICTIONARY.put("도트", "Dot");
        DICTIONARY.put("로고", "Print/Logo");

        // === [Fit: 핏] ===
        DICTIONARY.put("오버핏", "Oversized");
        DICTIONARY.put("박시", "Oversized");
        DICTIONARY.put("슬림핏", "Slim");
        DICTIONARY.put("스키니", "Slim");
        DICTIONARY.put("크롭", "Crop");

        // === [Style: 스타일] ===
        DICTIONARY.put("정장", "Formal");
        DICTIONARY.put("포멀", "Formal");
        DICTIONARY.put("운동복", "Sporty");
        DICTIONARY.put("스포티", "Sporty");
        DICTIONARY.put("빈티지", "Vintage");
        DICTIONARY.put("스트릿", "Street");

        // === [Detail: 디테일] ===
        DICTIONARY.put("찢청", "Distressed");
        DICTIONARY.put("구제", "Distressed");
        DICTIONARY.put("카고", "Pocket");
        DICTIONARY.put("주머니", "Pocket");
        DICTIONARY.put("목폴라", "Turtleneck");
        DICTIONARY.put("터틀넥", "Turtleneck");
        DICTIONARY.put("브이넥", "V-neck");

        // === [영어 -> 한글 변환 사전] ===
        // Material (소재)
        ENGLISH_TO_KOREAN.put("Denim", "데님");
        ENGLISH_TO_KOREAN.put("Leather", "가죽");
        ENGLISH_TO_KOREAN.put("Fur", "털");
        ENGLISH_TO_KOREAN.put("Silk", "실크");
        ENGLISH_TO_KOREAN.put("Tweed", "트위드");
        ENGLISH_TO_KOREAN.put("Velvet", "벨벳");
        ENGLISH_TO_KOREAN.put("Wool", "울");
        ENGLISH_TO_KOREAN.put("Linen", "린넨");
        ENGLISH_TO_KOREAN.put("Nylon", "나일론");
        ENGLISH_TO_KOREAN.put("Cotton", "면");
        ENGLISH_TO_KOREAN.put("Knit", "니트");
        ENGLISH_TO_KOREAN.put("Polyester", "폴리에스터");

        // Color (색상)
        ENGLISH_TO_KOREAN.put("Black", "검정");
        ENGLISH_TO_KOREAN.put("White", "흰색");
        ENGLISH_TO_KOREAN.put("Grey", "회색");
        ENGLISH_TO_KOREAN.put("Red", "빨강");
        ENGLISH_TO_KOREAN.put("Orange", "주황");
        ENGLISH_TO_KOREAN.put("Yellow", "노랑");
        ENGLISH_TO_KOREAN.put("Green", "초록");
        ENGLISH_TO_KOREAN.put("Blue", "파랑");
        ENGLISH_TO_KOREAN.put("Navy", "네이비");
        ENGLISH_TO_KOREAN.put("Purple", "보라");
        ENGLISH_TO_KOREAN.put("Pink", "분홍");
        ENGLISH_TO_KOREAN.put("Brown", "갈색");
        ENGLISH_TO_KOREAN.put("Beige", "베이지");
        ENGLISH_TO_KOREAN.put("Unknown", "알 수 없음");

        // Pattern (무늬)
        ENGLISH_TO_KOREAN.put("Solid", "단색");
        ENGLISH_TO_KOREAN.put("Stripe", "줄무늬");
        ENGLISH_TO_KOREAN.put("Check", "체크");
        ENGLISH_TO_KOREAN.put("Floral", "꽃무늬");
        ENGLISH_TO_KOREAN.put("Dot", "도트");
        ENGLISH_TO_KOREAN.put("Print/Logo", "로고");

        // NeckLine (넥라인)
        ENGLISH_TO_KOREAN.put("Round Neck", "라운드넥");
        ENGLISH_TO_KOREAN.put("Collar", "칼라");
        ENGLISH_TO_KOREAN.put("V-neck", "브이넥");
        ENGLISH_TO_KOREAN.put("Turtleneck", "터틀넥");
        ENGLISH_TO_KOREAN.put("Hood", "후드");

        // SleeveType (소매)
        ENGLISH_TO_KOREAN.put("Long Sleeve", "긴소매");
        ENGLISH_TO_KOREAN.put("Short Sleeve", "짧은소매");
        ENGLISH_TO_KOREAN.put("Sleeveless", "민소매");

        // Closure (여밈)
        ENGLISH_TO_KOREAN.put("Pullover", "풀오버");
        ENGLISH_TO_KOREAN.put("Zipper", "지퍼");
        ENGLISH_TO_KOREAN.put("Button", "단추");
        ENGLISH_TO_KOREAN.put("Belted", "벨트");

        // Style (스타일)
        ENGLISH_TO_KOREAN.put("Casual", "캐주얼");
        ENGLISH_TO_KOREAN.put("Formal", "정장");
        ENGLISH_TO_KOREAN.put("Sporty", "스포티");
        ENGLISH_TO_KOREAN.put("Vintage", "빈티지");
        ENGLISH_TO_KOREAN.put("Street", "스트릿");

        // Fit (핏)
        ENGLISH_TO_KOREAN.put("Regular Fit", "레귤러핏");
        ENGLISH_TO_KOREAN.put("Oversized", "오버핏");
        ENGLISH_TO_KOREAN.put("Slim", "슬림핏");

        // Length (길이)
        ENGLISH_TO_KOREAN.put("Standard", "기본");
        ENGLISH_TO_KOREAN.put("Crop", "크롭");
        ENGLISH_TO_KOREAN.put("Mini", "미니");
        ENGLISH_TO_KOREAN.put("Maxi/Long", "맥시/롱");

        // Texture (질감)
        ENGLISH_TO_KOREAN.put("Matte", "무광");
        ENGLISH_TO_KOREAN.put("Shiny", "광택");
        ENGLISH_TO_KOREAN.put("Furry/Soft", "털/부드러움");
        ENGLISH_TO_KOREAN.put("Rough", "거칠음");
        ENGLISH_TO_KOREAN.put("Sheer", "시스루");

        // Detail (디테일)
        ENGLISH_TO_KOREAN.put("None", "없음");
        ENGLISH_TO_KOREAN.put("Pocket", "주머니");
        ENGLISH_TO_KOREAN.put("Ruffle", "러플");
        ENGLISH_TO_KOREAN.put("Distressed", "찢어진");

        // Season (계절)
        ENGLISH_TO_KOREAN.put("SPRING_FALL", "봄/가을");
        ENGLISH_TO_KOREAN.put("SUMMER", "여름");
        ENGLISH_TO_KOREAN.put("WINTER", "겨울");

        // Thickness (두께)
        ENGLISH_TO_KOREAN.put("THIN", "얇음");
        ENGLISH_TO_KOREAN.put("MEDIUM", "보통");
        ENGLISH_TO_KOREAN.put("THICK", "두꺼움");

        // Occasion (상황)
        ENGLISH_TO_KOREAN.put("Daily", "일상");
        ENGLISH_TO_KOREAN.put("Office/Wedding", "정장/결혼식");
        ENGLISH_TO_KOREAN.put("Sports/Gym", "운동");
        ENGLISH_TO_KOREAN.put("Home", "홈웨어");
        ENGLISH_TO_KOREAN.put("Party/Date", "파티/데이트");
    }

    // ============================================
    // [공통 기능]
    // ============================================

    /**
     * 사용자의 문장에서 키워드 추출 (한국어 -> 영어 변환)
     */
    public List<String> extractEnglishKeywords(String userMessage) {
        List<String> foundKeywords = new ArrayList<>();

        System.out.println("🗣️ 사용자 입력: " + userMessage);

        // 단어장을 한 장씩 넘기면서 검사
        for (String koreanWord : DICTIONARY.keySet()) {
            // 사용자의 말 속에 "오버핏"이나 "청바지"가 들어있니?
            if (userMessage.contains(koreanWord)) {
                String englishWord = DICTIONARY.get(koreanWord);

                // 중복 방지 (이미 찾은 단어면 패스)
                if (!foundKeywords.contains(englishWord)) {
                    foundKeywords.add(englishWord);
                    System.out.println("   🔍 변환 성공: [" + koreanWord + "] -> [" + englishWord + "]");
                }
            }
        }

        if (foundKeywords.isEmpty()) {
            System.out.println("   ⚠️ 알는 단어가 없습니다. (기본 검색으로 전환)");
        } else {
            System.out.println("   ✅ 최종 검색 키워드: " + foundKeywords);
        }

        return foundKeywords;
    }

    // ============================================
    // [Public API - 비동기 메서드]
    // ============================================

    /**
     * 옷 분석 및 저장 (비동기) - ClothesController.uploadClothes()에서 호출
     * 즉시 반환하고 백그라운드에서 처리
     * 
     * @param file 옷 이미지 파일
     * @param category 옷 카테고리 (Top, Bottom, Shoes)
     */
    @Async("taskExecutor")
    @Transactional
    public void analyzeAndSaveClothesAsync(MultipartFile file, String category) {
        try {
            log.info("🤖 [비동기] 옷 분석 시작 - category: {}, filename: {}", category, file.getOriginalFilename());
            analyzeAndSaveClothesInternal(file, category);
            log.info("✅ [비동기] 옷 분석 및 저장 완료 - category: {}, filename: {}", category, file.getOriginalFilename());
        } catch (IOException e) {
            log.error("❌ [비동기] 옷 분석 중 오류 발생 - category: {}, filename: {}", category, file.getOriginalFilename(), e);
        }
    }

    /**
     * 옷 분석 및 저장 (비동기) - ClothesController.analyze()에서 호출
     * 상의, 하의, 신발을 한 번에 분석
     * 
     * @param dto 옷 분석 요청 DTO (top, bottom, shoes 포함)
     */
    @Async("taskExecutor")
    public void analyzeClothes(ClothesRequestDto dto) {
        try {
            log.info("🤖 [비동기] 옷 분석 및 DB 매핑 시작...");

            // 상의(Top)
            if (dto.getTop() != null && !dto.getTop().isEmpty()) {
                analyzeAndSaveClothesSync(dto.getTop(), "Top");
            }
            // 하의(Bottom)
            if (dto.getBottom() != null && !dto.getBottom().isEmpty()) {
                analyzeAndSaveClothesSync(dto.getBottom(), "Bottom");
            }
            // 신발(Shoes)
            if (dto.getShoes() != null && !dto.getShoes().isEmpty()) {
                analyzeAndSaveClothesSync(dto.getShoes(), "Shoes");
            }

            log.info("🎉 [비동기] 분석 종료!");

        } catch (Exception e) {
            log.error("❌ 옷 분석 중 오류 발생", e);
        }
    }

    // ============================================
    // [Public API - 동기 메서드]
    // ============================================

    /**
     * 옷 분석 및 저장 (동기) - FittingService의 CompletableFuture.supplyAsync() 내부에서 호출
     * 이미 비동기 컨텍스트 내부이므로 동기 메서드로 처리
     * 
     * @param imageBytes 이미지 바이트 배열 (HTTP 요청 컨텍스트 독립적)
     * @param filename 원본 파일명
     * @param category 옷 카테고리 (Top, Bottom, Shoes)
     * @return 저장된 Clothes 엔티티의 ID
     */
    @Transactional
    public Long analyzeAndSaveClothes(byte[] imageBytes, String filename, String category) throws IOException {
        log.info("🤖 [동기] 옷 분석 시작 - category: {}, filename: {}", category, filename);
        return analyzeAndSaveClothesInternal(imageBytes, filename, category);
    }

    /**
     * 옷 분석 및 저장 (동기) - analyzeClothes() 내부에서 호출
     * 이미 비동기 컨텍스트(@Async) 내부이므로 동기 메서드로 처리
     * 
     * @param file 옷 이미지 파일
     * @param category 옷 카테고리 (Top, Bottom, Shoes)
     * @return 저장된 Clothes 엔티티의 ID
     */
    @Transactional
    public Long analyzeAndSaveClothesSync(MultipartFile file, String category) throws IOException {
        log.info("🤖 [동기] 옷 분석 시작 - category: {}, filename: {}", category, file.getOriginalFilename());
        return analyzeAndSaveClothesInternal(file, category);
    }

    // ============================================
    // [Private 내부 로직]
    // ============================================

    /**
     * 옷 분석 및 저장 내부 로직 - MultipartFile 버전
     * byte[] 버전으로 변환하여 호출
     */
    private Long analyzeAndSaveClothesInternal(MultipartFile file, String category) throws IOException {
        return analyzeAndSaveClothesInternal(file.getBytes(), file.getOriginalFilename(), category);
    }

    /**
     * 옷 분석 및 저장 내부 로직 - byte[] 버전 (실제 분석 로직)
     * Google Vision API를 사용하여 이미지 분석 후 DB에 저장
     */
    private Long analyzeAndSaveClothesInternal(byte[] imageBytes, String filename, String category) throws IOException {

        // [Step 1] 구글 AI에게 물어보기
        List<String> tags = googleVisionService.extractLabels(imageBytes);
        String color = googleVisionService.extractDominantColor(imageBytes);

        // [Step 2] 소재(Material) 분석 (3단계 방어 로직)
        String material = "";
        if (tags.contains("Denim") || tags.contains("Jeans")) material = "Denim";
        else if (tags.contains("Leather")) material = "Leather";
        else if (tags.contains("Fur")) material = "Fur";
        else if (tags.contains("Silk")) material = "Silk";
        else if (tags.contains("Tweed")) material = "Tweed";
        else if (tags.contains("Velvet")) material = "Velvet";
        else if (tags.contains("Wool")) material = "Wool";
        else if (tags.contains("Linen")) material = "Linen";
        else if (tags.contains("Nylon")) material = "Nylon";

        if (material.isEmpty()) { // 2단계: 옷 종류로 추측
            if (tags.contains("Sweater") || tags.contains("Knitwear") || tags.contains("Cardigan")) material = "Knit";
            else if (tags.contains("Hoodie") || tags.contains("Sweatshirt") || tags.contains("T-shirt")) material = "Cotton";
            else if (tags.contains("Coat") || tags.contains("Jacket") || tags.contains("Padding") || tags.contains("Blazer")) material = "Polyester";
        }
        if (material.isEmpty()) material = "Cotton"; // 3단계: 기본값

        // [Step 3] 디자인(Design) 분석
        String neckLine = "Round Neck";
        if (tags.contains("Collar") || tags.contains("Polo shirt")) neckLine = "Collar";
        else if (tags.contains("V-neck")) neckLine = "V-neck";
        else if (tags.contains("Turtleneck")) neckLine = "Turtleneck";
        else if (tags.contains("Hood") || tags.contains("Hoodie")) neckLine = "Hood";

        String sleeveType = "Long Sleeve";
        if (tags.contains("Sleeveless") || tags.contains("Tank top")) sleeveType = "Sleeveless";
        else if (tags.contains("Shorts") || tags.contains("T-shirt")) {
            if (!tags.contains("Long sleeve")) sleeveType = "Short Sleeve";
        }

        String pattern = "Solid";
        if (tags.contains("Stripe") || tags.contains("Striped")) pattern = "Stripe";
        else if (tags.contains("Check") || tags.contains("Plaid") || tags.contains("Tartan")) pattern = "Check";
        else if (tags.contains("Floral")) pattern = "Floral";
        else if (tags.contains("Dot")) pattern = "Dot";
        else if (tags.contains("Logo") || tags.contains("Print")) pattern = "Print/Logo";

        String closure = "Pullover";
        if (tags.contains("Zipper") || tags.contains("Zip")) closure = "Zipper";
        else if (tags.contains("Button") || tags.contains("Shirt") || tags.contains("Cardigan")) closure = "Button";
        else if (tags.contains("Belt") || tags.contains("Trench coat")) closure = "Belted";

        String style = "Casual";
        if (tags.contains("Suit") || tags.contains("Blazer") || tags.contains("Formal")) style = "Formal";
        else if (tags.contains("Sportswear") || tags.contains("Jersey") || tags.contains("Athletic")) style = "Sporty";
        else if (tags.contains("Vintage") || tags.contains("Retro")) style = "Vintage";
        else if (tags.contains("Street fashion")) style = "Street";

        // [Step 4] 구조(Structure) 분석
        String fit = "Regular Fit";
        if (tags.contains("Oversized") || tags.contains("Baggy") || tags.contains("Loose")) fit = "Oversized";
        else if (tags.contains("Slim fit") || tags.contains("Skinny")) fit = "Slim";

        String length = "Standard";
        if (tags.contains("Crop top") || tags.contains("Crop")) length = "Crop";
        else if (tags.contains("Mini skirt")) length = "Mini";
        else if (tags.contains("Maxi") || tags.contains("Long dress")) length = "Maxi/Long";

        String texture = "Matte";
        if (tags.contains("Leather") || tags.contains("Satin") || tags.contains("Silk")) texture = "Shiny";
        else if (tags.contains("Fur") || tags.contains("Wool") || tags.contains("Velvet") || tags.contains("Fleece")) texture = "Furry/Soft";
        else if (tags.contains("Denim") || tags.contains("Canvas")) texture = "Rough";
        else if (tags.contains("Lace") || tags.contains("Sheer")) texture = "Sheer";

        String detail = "None";
        if (tags.contains("Pocket") || tags.contains("Cargo")) detail = "Pocket";
        else if (tags.contains("Ruffle")) detail = "Ruffle";
        else if (tags.contains("Ripped") || tags.contains("Distressed")) detail = "Distressed";

        // [Step 5] 상황(Occasion) & 계절 분석
        String occasion = "Daily";
        if (style.equals("Formal")) occasion = "Office/Wedding";
        else if (style.equals("Sporty")) occasion = "Sports/Gym";
        else if (tags.contains("Pajamas")) occasion = "Home";
        else if (tags.contains("Dress") || tags.contains("Party")) occasion = "Party/Date";

        String season = "SPRING_FALL";
        String thickness = "MEDIUM";
        if (tags.contains("Shorts") || tags.contains("Sleeveless") || tags.contains("Swimwear")) {
            season = "SUMMER"; thickness = "THIN";
        } else if (tags.contains("Coat") || tags.contains("Padding") || material.equals("Wool") || material.equals("Fur")) {
            season = "WINTER"; thickness = "THICK";
        }

        // [Step 6] 영어 -> 한글로 변환 (DB 저장용)
        String materialKr = ENGLISH_TO_KOREAN.getOrDefault(material, material);
        String colorKr = ENGLISH_TO_KOREAN.getOrDefault(color, color);
        String patternKr = ENGLISH_TO_KOREAN.getOrDefault(pattern, pattern);
        String neckLineKr = ENGLISH_TO_KOREAN.getOrDefault(neckLine, neckLine);
        String sleeveTypeKr = ENGLISH_TO_KOREAN.getOrDefault(sleeveType, sleeveType);
        String closureKr = ENGLISH_TO_KOREAN.getOrDefault(closure, closure);
        String styleKr = ENGLISH_TO_KOREAN.getOrDefault(style, style);
        String fitKr = ENGLISH_TO_KOREAN.getOrDefault(fit, fit);
        String lengthKr = ENGLISH_TO_KOREAN.getOrDefault(length, length);
        String textureKr = ENGLISH_TO_KOREAN.getOrDefault(texture, texture);
        String detailKr = ENGLISH_TO_KOREAN.getOrDefault(detail, detail);
        String seasonKr = ENGLISH_TO_KOREAN.getOrDefault(season, season);
        String thicknessKr = ENGLISH_TO_KOREAN.getOrDefault(thickness, thickness);
        String occasionKr = ENGLISH_TO_KOREAN.getOrDefault(occasion, occasion);

        // [Step 7] 이름 자동 생성 (한글)
        String autoName = colorKr + " " + materialKr + " " + (patternKr.equals("단색") ? "" : patternKr + " ") + category;
        if (colorKr.equals("알 수 없음")) autoName = "내 " + category;

        // [Step 8] 이미지를 GCS에 업로드
        String imgUrl;
        try {
            // 고유한 파일명 생성 (UUID 사용)
            String fileExtension = filename.contains(".") 
                ? filename.substring(filename.lastIndexOf(".")) 
                : ".jpg";
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // 카테고리에 따라 적절한 폴더에 업로드
            if ("Top".equalsIgnoreCase(category)) {
                imgUrl = gcsService.uploadTopImage(imageBytes, uniqueFilename, "image/jpeg");
            } else if ("Bottom".equalsIgnoreCase(category)) {
                imgUrl = gcsService.uploadBottomImage(imageBytes, uniqueFilename, "image/jpeg");
            } else {
                // Shoes 등 다른 카테고리는 기본 uploadImage 사용 (virtual-fitting-img 폴더)
                imgUrl = gcsService.uploadImage(imageBytes, uniqueFilename, "image/jpeg");
            }
            log.info("✅ 옷 이미지 GCS 업로드 완료 - category: {}, URL: {}", category, imgUrl);
        } catch (Exception e) {
            log.error("❌ 옷 이미지 GCS 업로드 실패 - category: {}, filename: {}", category, filename, e);
            // 업로드 실패 시 임시 URL 사용
            imgUrl = "http://temp.url/" + filename;
        }

        // [Step 9] DB 저장 (한글로 저장)
        Clothes clothes = Clothes.builder()
                .category(category)
                .name(autoName)
                .imgUrl(imgUrl)
                .color(colorKr)
                .season(seasonKr)
                .material(materialKr)
                .thickness(thicknessKr)
                .neckLine(neckLineKr)
                .sleeveType(sleeveTypeKr)
                .pattern(patternKr)
                .closure(closureKr)
                .style(styleKr)
                .fit(fitKr)
                .length(lengthKr)
                .texture(textureKr)
                .detail(detailKr)
                .occasion(occasionKr)
                .brand(null)
                .price(0)
                .build();

        Clothes saved = clothesRepository.save(clothes);
        log.info("✅ 옷 분석 및 저장 완료 - ID: {}, name: {}", saved.getId(), saved.getName());

        return saved.getId();
    }
}