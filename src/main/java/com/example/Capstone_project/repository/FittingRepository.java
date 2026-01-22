package com.example.Capstone_project.repository; // ✅ 형님 구조 맞춤

import com.example.Capstone_project.domain.FittingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FittingRepository extends JpaRepository<FittingTask, Long> {
}