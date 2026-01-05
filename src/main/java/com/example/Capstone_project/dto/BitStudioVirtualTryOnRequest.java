package com.example.Capstone_project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BitStudioVirtualTryOnRequest {
	@JsonProperty("person_image_id")
	private String personImageId;
	
	@JsonProperty("person_image_url")
	private String personImageUrl;
	
	@JsonProperty("outfit_image_id")
	private String outfitImageId;
	
	@JsonProperty("outfit_image_url")
	private String outfitImageUrl;
	
	@JsonProperty("outfit_asset_id")
	private String outfitAssetId;
	
	private String prompt;
	private String resolution; // standard or high
	private Integer numImages; // 1-4
	private Integer seed;
}


