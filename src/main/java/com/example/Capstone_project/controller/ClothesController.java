package com.example.Capstone_project.controller;

import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.repository.ClothesRepository;
import com.example.Capstone_project.service.ClothesAnalysisService;
import com.example.Capstone_project.service.GoogleVisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.Capstone_project.dto.ClothesRequestDto;


import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clothes")
public class ClothesController {

    private final ClothesRepository clothesRepository;
    private final GoogleVisionService googleVisionService;
    private final ClothesAnalysisService clothesAnalysisService;

    // 1. 옷 등록 (AI 초정밀 분석 기능 탑재)
    @PostMapping
    public String uploadClothes(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category
    ) throws IOException {

        // [Step 1] 구글 AI에게 물어보기
        List<String> tags = googleVisionService.extractLabels(file);
        String color = googleVisionService.extractDominantColor(file);

        // [Step 2] 🕵️‍♂️ 소재(Material) 분석 (3단계 방어 로직)
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

        // [Step 3] 🎨 디자인(Design) 분석
        // 3-1. 넥라인
        String neckLine = "Round Neck";
        if (tags.contains("Collar") || tags.contains("Polo shirt")) neckLine = "Collar";
        else if (tags.contains("V-neck")) neckLine = "V-neck";
        else if (tags.contains("Turtleneck")) neckLine = "Turtleneck";
        else if (tags.contains("Hood") || tags.contains("Hoodie")) neckLine = "Hood";

        // 3-2. 소매
        String sleeveType = "Long Sleeve";
        if (tags.contains("Sleeveless") || tags.contains("Tank top")) sleeveType = "Sleeveless";
        else if (tags.contains("Shorts") || tags.contains("T-shirt")) {
            if (!tags.contains("Long sleeve")) sleeveType = "Short Sleeve";
        }

        // 3-3. 패턴
        String pattern = "Solid";
        if (tags.contains("Stripe") || tags.contains("Striped")) pattern = "Stripe";
        else if (tags.contains("Check") || tags.contains("Plaid") || tags.contains("Tartan")) pattern = "Check";
        else if (tags.contains("Floral")) pattern = "Floral";
        else if (tags.contains("Dot")) pattern = "Dot";
        else if (tags.contains("Logo") || tags.contains("Print")) pattern = "Print/Logo";

        // 3-4. 여밈 (Closure)
        String closure = "Pullover"; // 기본값 (그냥 입는 옷)
        if (tags.contains("Zipper") || tags.contains("Zip")) closure = "Zipper";
        else if (tags.contains("Button") || tags.contains("Shirt") || tags.contains("Cardigan")) closure = "Button";
        else if (tags.contains("Belt") || tags.contains("Trench coat")) closure = "Belted";

        // 3-5. 스타일
        String style = "Casual";
        if (tags.contains("Suit") || tags.contains("Blazer") || tags.contains("Formal")) style = "Formal";
        else if (tags.contains("Sportswear") || tags.contains("Jersey") || tags.contains("Athletic")) style = "Sporty";
        else if (tags.contains("Vintage") || tags.contains("Retro")) style = "Vintage";
        else if (tags.contains("Street fashion")) style = "Street";

        // [Step 4] 📐 구조(Structure) 분석
        // 4-1. 핏
        String fit = "Regular Fit";
        if (tags.contains("Oversized") || tags.contains("Baggy") || tags.contains("Loose")) fit = "Oversized";
        else if (tags.contains("Slim fit") || tags.contains("Skinny")) fit = "Slim";

        // 4-2. 기장
        String length = "Standard";
        if (tags.contains("Crop top") || tags.contains("Crop")) length = "Crop";
        else if (tags.contains("Mini skirt")) length = "Mini";
        else if (tags.contains("Maxi") || tags.contains("Long dress")) length = "Maxi/Long";

        // 4-3. 텍스처
        String texture = "Matte"; // 기본값
        if (tags.contains("Leather") || tags.contains("Satin") || tags.contains("Silk")) texture = "Shiny";
        else if (tags.contains("Fur") || tags.contains("Wool") || tags.contains("Velvet") || tags.contains("Fleece")) texture = "Furry/Soft";
        else if (tags.contains("Denim") || tags.contains("Canvas")) texture = "Rough";
        else if (tags.contains("Lace") || tags.contains("Sheer")) texture = "Sheer";

        // 4-4. 디테일
        String detail = "None";
        if (tags.contains("Pocket") || tags.contains("Cargo")) detail = "Pocket";
        else if (tags.contains("Ruffle")) detail = "Ruffle";
        else if (tags.contains("Ripped") || tags.contains("Distressed")) detail = "Distressed";

        // [Step 5] 📅 상황(Occasion) & 계절 분석
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

        // [Step 6] 🏷️ 이름 자동 생성 (예: Black Leather Zipper Jacket)
        String autoName = color + " " + material + " " + (pattern.equals("Solid") ? "" : pattern + " ") + category;
        if (color.equals("Unknown")) autoName = "My " + category;

        // [Step 7] DB 저장
        Clothes clothes = new Clothes();
        clothes.setCategory(category);
        clothes.setName(autoName);
        clothes.setImgUrl("http://temp.url/" + file.getOriginalFilename()); // 실제 S3 구현 전까지 임시 URL

        clothes.setColor(color);
        clothes.setSeason(season);
        clothes.setMaterial(material);
        clothes.setThickness(thickness);

        clothes.setNeckLine(neckLine);
        clothes.setSleeveType(sleeveType);
        clothes.setPattern(pattern);
        clothes.setClosure(closure);
        clothes.setStyle(style);

        clothes.setFit(fit);
        clothes.setLength(length);
        clothes.setTexture(texture);
        clothes.setDetail(detail);

        clothes.setOccasion(occasion);
        clothes.setBrand(null); // 나중에 입력 가능하도록 null
        clothes.setPrice(0);

        clothesRepository.save(clothes);

        return "✅ 저장 완료! \n" +
                "이름: " + autoName + "\n" +
                "특징: " + season + ", " + fit + ", " + style + "\n" +
                "상황: " + occasion + " (AI 추천 완료)";
    }

    // 2. 옷 목록 조회
// ClothesController.java 수정 예시

    @PostMapping("/analysis") // (주소는 원래 쓰던 거 유지)
    public String analyze(@ModelAttribute ClothesRequestDto requestDto) {

        // 서비스 호출 (이제 DTO를 넘겨주니까 에러가 사라집니다)
        clothesAnalysisService.analyzeClothes(requestDto);

        return "분석 요청 완료!";
    }
}