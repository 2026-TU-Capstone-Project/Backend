package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.ChatMessage;
import com.example.Capstone_project.dto.ChatRequestDto;
import com.example.Capstone_project.dto.ChatResponseDto;
import com.example.Capstone_project.dto.ClothesRecommendationResponse;
import com.example.Capstone_project.dto.StyleRecommendationResponse;
import com.example.Capstone_project.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gemini 기반 AI 챗봇. 에이전트 스타일: 내 옷장 추천, 피드 추천, 웹 검색 툴 + DB 메모리(chat_messages).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String TOOL_NAME_RECOMMEND_FROM_MY_CLOSET = "recommend_from_my_closet";
    private static final String TOOL_NAME_RECOMMEND_FROM_FEED = "recommend_from_feed";
    private static final String TOOL_NAME_SEARCH_WEB_STYLES = "search_web_styles";
    private static final int MAX_TOOL_ROUNDS = 3;
    private static final double MIN_SCORE = 0.7;
    private final WebClient geminiWebClient;
    private final StyleRecommendationService styleRecommendationService;
    private final WebSearchService webSearchService;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.analysis-model:gemini-2.0-flash}")
    private String chatModel;

    private static final String SYSTEM_INSTRUCTION = """
        너는 옷·스타일 추천을 돕는 친절한 챗봇이야. 다음 세 가지 도구만 사용해줘.

        1) recommend_from_my_closet: 내 옷장에서만 추천해달라고 할 때 사용해. ("내 옷장에서 ~", "내가 가진 옷으로 ~", "제 옷장 기준으로 ~" 등)
        2) recommend_from_feed: 피드/커뮤니티에 올라온 코디에서 추천해달라고 할 때 사용해. ("피드에서 ~", "다른 사람 코디에서 ~", "피드 기준으로 ~" 등)
        3) search_web_styles: 인터넷/웹에서 스타일·트렌드를 검색만 해달라고 할 때 사용해. ("요즘 유행하는 스타일", "웹에서 검색해줘", "인터넷에서 찾아줘" 등).
        추천이 아니라 검색일 때만 이 툴을 써.

        recommend_from_my_closet 호출 시 category 파라미터를 반드시 넣어줘.
        - 사용자가 "전체 코디", "스타일 추천", "옷 추천" 같이 전체를 원하면 category="style"
        - "상의만", "티셔츠/셔츠 추천", "윗옷만" 같이 상의만 원하면 category="tops"
        - "하의만", "바지 추천", "아래옷만" 같이 하의만 원하면 category="bottoms"

        일반 대화나 "추천해줘"만 하고 내 옷장/피드/웹 검색 구분이 없으면 recommend_from_my_closet을 우선 사용해줘.

        툴 결과에 추천이 1개 이상 있으면 절대 "못 찾았어요"라고 하지 말고, 최소 1개 이상 구체적으로 추천해줘. 추천이 없으면 "아직 비슷한 스타일이 없어요. 가상 피팅을 먼저 해보시면 더 많은 추천을 받을 수 있어요."처럼 안내해줘.

        search_web_styles 툴 결과를 받았을 때는, 검색 내용을 반영한 한두 문장 요약으로 답해줘.

        응답 형식 규칙 (반드시 지켜줘):
        - **, *, #, > 같은 마크다운 서식 기호는 절대 사용하지 마. 일반 텍스트로만 답해줘.
        - 추천 결과는 도입부/요약/마무리 멘트 없이 바로 목록으로 시작해.
        - 각 아이템은 "번호. 아이템명 — 한 줄 설명" 형식으로만 적어줘.
        - 예시:
          1. 흰 오버핏 티셔츠 — 캐주얼한 데일리 룩에 잘 어울려요.
          2. 네이비 슬랙스 — 깔끔하고 단정한 인상을 줍니다.
        """;

    public ChatResponseDto chat(Long userId, ChatRequestDto request) {
        List<Map<String, Object>> contents = buildContents(userId, request);
        // 사용자 메시지 DB 저장 (히스토리 로드 후 저장해 현재 메시지가 중복으로 로드되지 않게 함)
        chatMessageRepository.save(ChatMessage.builder()
                .userId(userId)
                .role("user")
                .content(request.getMessage())
                .build());
        StyleRecommendationResponse lastRecommendations = null;
        ClothesRecommendationResponse lastRecommendationsTops = null;
        ClothesRecommendationResponse lastRecommendationsBottoms = null;
        int rounds = 0;
        while (rounds < MAX_TOOL_ROUNDS) {
            ObjectNode requestBody = buildGenerateContentRequest(contents);
            String responseStr;
            try {
                responseStr = geminiWebClient.post()
                        .uri(uri -> uri.path("/models/" + chatModel + ":generateContent").queryParam("key", geminiApiKey).build())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(java.time.Duration.ofSeconds(60))
                        .block();
            } catch (WebClientResponseException e) {
                log.error("Gemini chat API error: {}", e.getResponseBodyAsString(), e);
                saveAssistantMessage(userId, "응답을 생성하지 못했어요. 다시 말씀해 주세요.");
                throw new RuntimeException("챗봇 응답 생성 실패: " + e.getMessage());
            }
            JsonNode response;
            try {
                response = objectMapper.readTree(responseStr);
            } catch (JsonProcessingException e) {
                log.error("Gemini response parse error", e);
                saveAssistantMessage(userId, "챗봇 응답 파싱 실패");
                throw new RuntimeException("챗봇 응답 파싱 실패");
            }
            JsonNode candidates = response.path("candidates");
            if (candidates.isEmpty() || !candidates.get(0).path("content").path("parts").isArray()) {
                String msg = "응답을 생성하지 못했어요. 다시 말씀해 주세요.";
                saveAssistantMessage(userId, msg);
                return ChatResponseDto.builder()
                        .message(msg)
                        .recommendations(lastRecommendations)
                        .recommendationsTops(lastRecommendationsTops)
                        .recommendationsBottoms(lastRecommendationsBottoms)
                        .build();
            }
            ArrayNode parts = (ArrayNode) candidates.get(0).path("content").path("parts");
            Optional<JsonNode> functionCall = findFunctionCall(parts);
            if (functionCall.isEmpty()) {
                String text = extractTextFromParts(parts);
                String message = text != null ? text : "응답이 비어있어요.";
                saveAssistantMessage(userId, message);
                return ChatResponseDto.builder()
                        .message(message)
                        .recommendations(lastRecommendations)
                        .recommendationsTops(lastRecommendationsTops)
                        .recommendationsBottoms(lastRecommendationsBottoms)
                        .build();
            }
            JsonNode fc = functionCall.get();
            String name = fc.path("name").asText();
            JsonNode args = fc.path("args");
            String query = args.has("query") && !args.path("query").isNull() ? args.path("query").asText("") : "";
            String gender = args.has("gender") && !args.path("gender").isNull() ? args.path("gender").asText() : null;
            Integer limit = args.has("limit") && !args.path("limit").isNull() ? args.path("limit").asInt() : null;
            String category = args.has("category") && !args.path("category").isNull() ? args.path("category").asText("").trim().toLowerCase() : "style";

            if (TOOL_NAME_RECOMMEND_FROM_MY_CLOSET.equals(name)) {
                try {
                    if ("tops".equals(category)) {
                        ClothesRecommendationResponse rec = styleRecommendationService.recommendTopsByStyleFromMyCloset(query, MIN_SCORE, gender, limit, userId);
                        lastRecommendationsTops = rec;
                        Map<String, Object> responseMap = objectMapper.convertValue(rec, Map.class);
                        appendFunctionResponse(contents, name, responseMap);
                    } else if ("bottoms".equals(category)) {
                        ClothesRecommendationResponse rec = styleRecommendationService.recommendBottomsByStyleFromMyCloset(query, MIN_SCORE, gender, limit, userId);
                        lastRecommendationsBottoms = rec;
                        Map<String, Object> responseMap = objectMapper.convertValue(rec, Map.class);
                        appendFunctionResponse(contents, name, responseMap);
                    } else {
                        var list = styleRecommendationService.recommendByStyleWithFilters(query, MIN_SCORE, gender, limit, userId);
                        StyleRecommendationResponse rec = StyleRecommendationResponse.from(list);
                        lastRecommendations = rec;
                        Map<String, Object> responseMap = objectMapper.convertValue(rec, Map.class);
                        appendFunctionResponse(contents, name, responseMap);
                    }
                } catch (Exception e) {
                    log.warn("recommend_from_my_closet 실행 실패: {}", e.getMessage());
                    appendFunctionResponse(contents, name, Map.of("error", e.getMessage()));
                }
            } else if (TOOL_NAME_RECOMMEND_FROM_FEED.equals(name)) {
                try {
                    var list = styleRecommendationService.recommendFromFeed(query, MIN_SCORE, gender, limit);
                    StyleRecommendationResponse rec = StyleRecommendationResponse.from(list);
                    lastRecommendations = rec;
                    Map<String, Object> responseMap = objectMapper.convertValue(rec, Map.class);
                    appendFunctionResponse(contents, name, responseMap);
                } catch (Exception e) {
                    log.warn("recommend_from_feed 실행 실패: {}", e.getMessage());
                    appendFunctionResponse(contents, name, Map.of("error", e.getMessage()));
                }
            } else if (TOOL_NAME_SEARCH_WEB_STYLES.equals(name)) {
                String searchResult = webSearchService.searchStyleTrends(query);
                appendFunctionResponse(contents, name, Map.of("summary", searchResult));
            } else {
                appendFunctionResponse(contents, name, Map.of("error", "알 수 없는 툴: " + name));
            }
            rounds++;
        }
        String fallbackMsg = "처리 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.";
        saveAssistantMessage(userId, fallbackMsg);
        return ChatResponseDto.builder()
                .message(fallbackMsg)
                .recommendations(lastRecommendations)
                .recommendationsTops(lastRecommendationsTops)
                .recommendationsBottoms(lastRecommendationsBottoms)
                .build();
    }

    private void saveAssistantMessage(Long userId, String content) {
        chatMessageRepository.save(ChatMessage.builder()
                .userId(userId)
                .role("assistant")
                .content(content)
                .build());
    }

    private List<Map<String, Object>> buildContents(Long userId, ChatRequestDto request) {
        List<Map<String, Object>> contents = new ArrayList<>();
        List<ChatRequestDto.ChatMessageDto> history = request.getHistory();
        if (history != null && !history.isEmpty()) {
            for (ChatRequestDto.ChatMessageDto m : history) {
                String role = "user".equalsIgnoreCase(m.getRole()) ? "user" : "model";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", m.getContent() != null ? m.getContent() : ""))
                ));
            }
        } else {
            List<ChatMessage> dbMessages = chatMessageRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
            Collections.reverse(dbMessages);
            for (ChatMessage m : dbMessages) {
                String role = "user".equals(m.getRole()) ? "user" : "model";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", m.getContent() != null ? m.getContent() : ""))
                ));
            }
        }
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", request.getMessage()))
        ));
        return contents;
    }

    private ObjectNode buildGenerateContentRequest(List<Map<String, Object>> contents) {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("contents", objectMapper.valueToTree(contents));
        ObjectNode genConfig = objectMapper.createObjectNode();
        genConfig.put("temperature", 0.7);
        genConfig.put("maxOutputTokens", 768);
        body.set("generationConfig", genConfig);
        body.set("systemInstruction", objectMapper.createObjectNode().set("parts", objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("text", SYSTEM_INSTRUCTION))));
        Map<String, Object> recommendParamsMyCloset = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "query", Map.of("type", "STRING", "description", "검색어 (예: 깔끔하고 단정한 스타일, 캐주얼 데일리)"),
                        "category", Map.of("type", "STRING", "description", "style=전체 코디, tops=상의만(티셔츠/셔츠 등), bottoms=하의만. 사용자 말에 맞게 넣어줘."),
                        "gender", Map.of("type", "STRING", "description", "MALE 또는 FEMALE (선택)"),
                        "limit", Map.of("type", "INTEGER", "description", "최대 개수 1~50 (선택)")
                ),
                "required", List.of("query", "category")
        );
        Map<String, Object> recommendParamsFeed = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "query", Map.of("type", "STRING", "description", "검색어 (예: 깔끔한 스타일)"),
                        "gender", Map.of("type", "STRING", "description", "MALE 또는 FEMALE (선택)"),
                        "limit", Map.of("type", "INTEGER", "description", "최대 개수 1~50 (선택)")
                ),
                "required", List.of("query")
        );
        Map<String, Object> searchParams = Map.of(
                "type", "OBJECT",
                "properties", Map.of("query", Map.of("type", "STRING", "description", "검색어 (예: 요즘 유행 스타일, 2024 FW 트렌드)")),
                "required", List.of("query")
        );
        List<Map<String, Object>> toolDecl = List.of(
                Map.of(
                        "name", TOOL_NAME_RECOMMEND_FROM_MY_CLOSET,
                        "description", "내 옷장 기준 추천. 전체 코디면 category=style, 상의만(티셔츠/셔츠 등)이면 category=tops, 하의만이면 category=bottoms로 호출하세요. '내 옷장에서 ~', '내가 가진 옷으로 ~' 요청일 때 사용.",
                        "parameters", recommendParamsMyCloset
                ),
                Map.of(
                        "name", TOOL_NAME_RECOMMEND_FROM_FEED,
                        "description", "피드(커뮤니티)에 올라온 코디 중에서 스타일을 추천합니다. '피드에서 ~', '다른 사람 코디에서 ~' 요청일 때 사용하세요.",
                        "parameters", recommendParamsFeed
                ),
                Map.of(
                        "name", TOOL_NAME_SEARCH_WEB_STYLES,
                        "description", "인터넷에서 스타일·트렌드를 검색합니다. 추천이 아니라 검색만 할 때 사용하세요. (요즘 유행, 웹에서 찾아줘 등)",
                        "parameters", searchParams
                )
        );
        body.set("tools", objectMapper.createArrayNode().add(objectMapper.createObjectNode().set("functionDeclarations", objectMapper.valueToTree(toolDecl))));
        return body;
    }

    private Optional<JsonNode> findFunctionCall(ArrayNode parts) {
        for (JsonNode p : parts) {
            if (p.has("functionCall")) return Optional.of(p.path("functionCall"));
        }
        return Optional.empty();
    }

    private String extractTextFromParts(ArrayNode parts) {
        for (JsonNode p : parts) {
            if (p.has("text")) return p.path("text").asText("");
        }
        return null;
    }

    private void appendFunctionResponse(List<Map<String, Object>> contents, String name, Map<String, Object> response) {
        // Gemini tool-calling 흐름:
        // (1) 모델이 functionCall 생성 → (2) 클라이언트가 tool 실행 → (3) user role로 functionResponse를 전달
        // 여기서 functionCall을 다시 추가하면 모델이 혼동할 수 있어 functionResponse만 전달합니다.
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("functionResponse", Map.of("name", name, "response", response)))
        ));
    }
}
