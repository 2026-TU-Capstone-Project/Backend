package com.example.Capstone_project.service;

import com.example.Capstone_project.common.exception.ResourceNotFoundException;
import com.example.Capstone_project.domain.ExampleEntity;
import com.example.Capstone_project.dto.ExampleRequest;
import com.example.Capstone_project.dto.ExampleResponse;
import com.example.Capstone_project.repository.ExampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleService {
	
	private final ExampleRepository exampleRepository;
	
	public List<ExampleResponse> findAll() {
		return exampleRepository.findAll().stream()
			.map(ExampleResponse::from)
			.collect(Collectors.toList());
	}
	
	public ExampleResponse findById(Long id) {
		ExampleEntity entity = exampleRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Example not found with id: " + id));
		return ExampleResponse.from(entity);
	}
	
	@Transactional
	public ExampleResponse create(ExampleRequest request) {
		ExampleEntity entity = ExampleEntity.builder()
			.name(request.getName())
			.description(request.getDescription())
			.build();
		
		ExampleEntity saved = exampleRepository.save(entity);
		return ExampleResponse.from(saved);
	}
	
	@Transactional
	public ExampleResponse update(Long id, ExampleRequest request) {
		ExampleEntity entity = exampleRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Example not found with id: " + id));
		
		entity.update(request.getName(), request.getDescription());
		return ExampleResponse.from(entity);
	}
	
	@Transactional
	public void delete(Long id) {
		if (!exampleRepository.existsById(id)) {
			throw new ResourceNotFoundException("Example not found with id: " + id);
		}
		exampleRepository.deleteById(id);
	}
}








