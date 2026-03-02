package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.ClothesUploadTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesUploadTaskRepository extends JpaRepository<ClothesUploadTask, Long> {
}
