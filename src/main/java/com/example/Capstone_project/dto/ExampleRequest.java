package com.example.Capstone_project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExampleRequest {
	@NotBlank(message = "Name is required")
	private String name;
	
	private String description;
}




