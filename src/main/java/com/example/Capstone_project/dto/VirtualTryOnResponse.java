package com.example.Capstone_project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualTryOnResponse {
	private String imageId;
	private String status;
	private String imageUrl;
	private Integer creditsUsed;
	private List<String> sourceImageIds;
}


