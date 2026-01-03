package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.ExampleEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExampleResponse {
	private Long id;
	private String name;
	private String description;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
	public static ExampleResponse from(ExampleEntity entity) {
		return ExampleResponse.builder()
			.id(entity.getId())
			.name(entity.getName())
			.description(entity.getDescription())
			.createdAt(entity.getCreatedAt())
			.updatedAt(entity.getUpdatedAt())
			.build();
	}
}






