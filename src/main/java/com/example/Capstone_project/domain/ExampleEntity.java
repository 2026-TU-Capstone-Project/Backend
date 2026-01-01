package com.example.Capstone_project.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "examples")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExampleEntity extends BaseEntity {
	private String name;
	private String description;
	
	public void update(String name, String description) {
		this.name = name;
		this.description = description;
	}
}


