package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.ExampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExampleRepository extends JpaRepository<ExampleEntity, Long> {
}








