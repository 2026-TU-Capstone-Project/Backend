package com.example.Capstone_project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serper API를 이용한 웹 검색. 챗봇 툴 search_web_styles에서 스타일·트렌드 검색 시 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchService {

    private static final String SERPER_SEARCH_URL = "https://google.serper.dev/search";

    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${serper.api-key:}")
    private String serperApiKey;

    /**
     * 검색어로 웹 검색 후 결과 스니펫(제목+요약)을 한 문자열로 포맷해 반환.
     * API 키가 없으면 "검색 API를 사용할 수 없습니다." 반환.
     */
    public String searchStyleTrends(String query) {
        if (query == null || query.isBlank()) {
            return "검색어를 입력해주세요.";
        }
        if (serperApiKey == null || serperApiKey.isBlank()) {
            log.warn("Serper API key not configured. Web search disabled.");
            return "검색 API를 사용할 수 없습니다. 관리자에게 문의해 주세요.";
        }

        try {
            String body = objectMapper.writeValueAsString(Map.of("q", query.trim()));
            String responseStr = webClient.post()
                    .uri(SERPER_SEARCH_URL)
                    .header("X-API-KEY", serperApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .block();

            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode organic = root.path("organic");
            if (organic == null || !organic.isArray()) {
                return "검색 결과가 없습니다.";
            }

            List<String> lines = new ArrayList<>();
            int max = Math.min(organic.size(), 8);
            for (int i = 0; i < max; i++) {
                JsonNode item = organic.get(i);
                String title = item.path("title").asText("");
                String snippet = item.path("snippet").asText("");
                if (!title.isEmpty() || !snippet.isEmpty()) {
                    lines.add((title.isEmpty() ? "" : title + ": ") + snippet);
                }
            }
            if (lines.isEmpty()) {
                return "검색 결과가 없습니다.";
            }
            return String.join("\n", lines);
        } catch (WebClientResponseException e) {
            log.warn("Serper API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "검색 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
        } catch (Exception e) {
            log.warn("Web search failed: {}", e.getMessage());
            return "검색 중 오류가 발생했습니다.";
        }
    }
}
