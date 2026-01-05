package com.example.Capstone_project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BitStudioVirtualTryOnResponse {
	private String id;
	private String status;
	private String task;
	
	@JsonProperty("estimated_completion")
	private String estimatedCompletion;
	
	@JsonProperty("credits_used")
	private Integer creditsUsed;
	
	@JsonProperty("source_image_ids")
	private List<String> sourceImageIds;
}


