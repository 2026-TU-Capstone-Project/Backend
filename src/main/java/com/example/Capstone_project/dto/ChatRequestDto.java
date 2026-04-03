package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챗봇 메시지 요청 (대화 이력 + 새 메시지 + 날씨 정보)")
public class ChatRequestDto {

    @Schema(description = "이전 대화 목록 (선택). 비우면 새 대화로 처리.")
    private List<ChatMessageDto> history = List.of();

    @NotBlank(message = "메시지를 입력해주세요.")
    @Schema(description = "사용자가 보낼 메시지", required = true, example = "결혼식에 입을 옷 추천해줘")
    private String message;

    // =========================================================================
    // 날씨 정보 (OpenWeatherMap 데이터, 선택값 - 없으면 기본값 사용)
    // 프론트에서 현재 날씨 데이터를 넘겨주면 추천에 반영됨
    // =========================================================================

    @Schema(description = "현재 기온 (섭씨). 없으면 20.0 기본값 사용", example = "23.5")
    private Double temp;

    @Schema(description = "강수량 mm/h. 비 안 오면 0.0", example = "0.0")
    private Double rain = 0.0;

    @Schema(description = "적설량 mm/h. 눈 안 오면 0.0", example = "0.0")
    private Double snow = 0.0;

    @Schema(description = "풍속 m/s. 없으면 0.0", example = "3.5")
    private Double windSpeed = 0.0;

    @Schema(description = "습도 %. 없으면 0", example = "60")
    private Integer humidity = 0;

    // 날씨 데이터가 없을 때 기본값 처리
    public double getTempOrDefault()      { return temp      != null ? temp      : 20.0; }
    public double getRainOrDefault()      { return rain      != null ? rain      : 0.0;  }
    public double getSnowOrDefault()      { return snow      != null ? snow      : 0.0;  }
    public double getWindSpeedOrDefault() { return windSpeed != null ? windSpeed : 0.0;  }
    public int    getHumidityOrDefault()  { return humidity  != null ? humidity  : 0;    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "대화 한 턴")
    public static class ChatMessageDto {
        @Schema(description = "역할: user 또는 assistant", example = "user")
        private String role;

        @Schema(description = "메시지 내용")
        private String content;
    }
}
