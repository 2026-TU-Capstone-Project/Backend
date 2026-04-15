package com.example.Capstone_project.dto;

import lombok.Getter;

/**
 * OpenWeatherMap API 응답 기반 날씨 조건 DTO
 * 프론트에서 현재 날씨 데이터를 그대로 넘겨주면 됨
 */
@Getter
public class WeatherConditionDto {

    private final double temp;       // 기온 (섭씨)
    private final double rain;       // 강수량 (mm/h, 없으면 0.0)
    private final double snow;       // 적설량 (mm/h, 없으면 0.0)
    private final double windSpeed;  // 풍속 (m/s, 없으면 0.0)
    private final int    humidity;   // 습도 (%, 없으면 0)

    public WeatherConditionDto(double temp, double rain, double snow, double windSpeed, int humidity) {
        this.temp      = temp;
        this.rain      = rain;
        this.snow      = snow;
        this.windSpeed = windSpeed;
        this.humidity  = humidity;
    }

    public boolean isRaining() { return rain > 0.0; }
    public boolean isSnowing() { return snow > 0.0; }
    public boolean isWindy()   { return windSpeed > 7.0; }  // 7m/s 이상 강풍
    public boolean isHumid()   { return humidity > 80; }    // 습도 80% 이상
}
