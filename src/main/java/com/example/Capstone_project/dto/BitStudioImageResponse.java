package com.example.Capstone_project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BitStudioImageResponse {
	private String id;
	private String type;
	private String path;
	private String status;
	private Integer widthPx;
	private Integer heightPx;
	
	@JsonProperty("is_generated")
	private Boolean isGenerated;
	
	@JsonProperty("for_training")
	private Boolean forTraining;
	
	@JsonProperty("created_timestamp")
	private String createdTimestamp;
	
	private List<Version> versions;
	
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Version {
		private String id;
		
		@JsonProperty("version_type")
		private String versionType;
		
		private String path;
	}
}


