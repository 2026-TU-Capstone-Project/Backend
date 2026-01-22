package com.example.Capstone_project.service;

import com.example.Capstone_project.dto.ClothesRequestDto;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClothesAnalysisService {

    // 1. 한국어 -> 영어 변환 사전 (형님 코드 분석 완료)
    private static final Map<String, String> DICTIONARY = new HashMap<>();

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
        DICTIONARY.put("데님", "Denim");     // 사진 참고: Denim, Jeans
        DICTIONARY.put("청", "Denim");
        DICTIONARY.put("가죽", "Leather");   // 사진 참고: Leather
        DICTIONARY.put("레더", "Leather");
        DICTIONARY.put("퍼", "Fur");         // 사진 참고: Fur
        DICTIONARY.put("털", "Fur");
        DICTIONARY.put("실크", "Silk");       // 사진 참고: Silk
        DICTIONARY.put("트위드", "Tweed");    // 사진 참고: Tweed
        DICTIONARY.put("벨벳", "Velvet");     // 사진 참고: Velvet
        DICTIONARY.put("울", "Wool");         // 사진 참고: Wool
        DICTIONARY.put("린넨", "Linen");      // 사진 참고: Linen
        DICTIONARY.put("나일론", "Nylon");    // 사진 참고: Nylon
        DICTIONARY.put("면", "Cotton");       // 사진 참고: Cotton
        DICTIONARY.put("코튼", "Cotton");

        // === [Pattern: 무늬] ===
        DICTIONARY.put("줄무늬", "Stripe");   // 사진 참고: Stripe
        DICTIONARY.put("스트라이프", "Stripe");
        DICTIONARY.put("체크", "Check");      // 사진 참고: Check, Plaid, Tartan
        DICTIONARY.put("플라워", "Floral");   // 사진 참고: Floral
        DICTIONARY.put("꽃무늬", "Floral");
        DICTIONARY.put("땡땡이", "Dot");      // 사진 참고: Dot
        DICTIONARY.put("도트", "Dot");
        DICTIONARY.put("로고", "Print/Logo"); // 사진 참고: Logo, Print

        // === [Fit: 핏] ===
        DICTIONARY.put("오버핏", "Oversized"); // 사진 참고: Oversized, Baggy, Loose
        DICTIONARY.put("박시", "Oversized");
        DICTIONARY.put("슬림핏", "Slim");      // 사진 참고: Slim fit, Skinny
        DICTIONARY.put("스키니", "Slim");
        DICTIONARY.put("크롭", "Crop");        // 사진 참고: Crop top

        // === [Style: 스타일] ===
        DICTIONARY.put("정장", "Formal");      // 사진 참고: Suit, Blazer, Formal
        DICTIONARY.put("포멀", "Formal");
        DICTIONARY.put("운동복", "Sporty");    // 사진 참고: Sportswear, Jersey, Athletic
        DICTIONARY.put("스포티", "Sporty");
        DICTIONARY.put("빈티지", "Vintage");   // 사진 참고: Vintage, Retro
        DICTIONARY.put("스트릿", "Street");    // 사진 참고: Street fashion

        // === [Detail: 디테일] ===
        DICTIONARY.put("찢청", "Distressed");  // 사진 참고: Ripped, Distressed
        DICTIONARY.put("구제", "Distressed");
        DICTIONARY.put("카고", "Pocket");      // 사진 참고: Pocket, Cargo
        DICTIONARY.put("주머니", "Pocket");
        DICTIONARY.put("목폴라", "Turtleneck");// 사진 참고: Turtleneck
        DICTIONARY.put("터틀넥", "Turtleneck");
        DICTIONARY.put("브이넥", "V-neck");    // 사진 참고: V-neck
    }

    // 2. [핵심 기능] 사용자의 문장에서 키워드 뽑아내기
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

    // 3. 기존 분석 로직 (그대로 유지)
    @Async
    public void analyzeClothes(ClothesRequestDto dto) {
        try {
            System.out.println("🤖 [비동기] 옷 분석 및 DB 매핑 시작...");

            // 상의(Top)
            if (dto.getTop() != null && !dto.getTop().isEmpty()) {
                System.out.println("✅ [처리중] 상의(Top) -> DB 매핑 완료");
            }
            // 하의(Bottom)
            if (dto.getBottom() != null && !dto.getBottom().isEmpty()) {
                System.out.println("✅ [처리중] 하의(Bottom) -> DB 매핑 완료");
            }
            // 신발(Shoes)
            if (dto.getShoes() != null && !dto.getShoes().isEmpty()) {
                System.out.println("✅ [처리중] 신발(Shoes) -> DB 매핑 완료");
            }

            Thread.sleep(3000);
            System.out.println("🎉 [완료] 분석 종료!");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}