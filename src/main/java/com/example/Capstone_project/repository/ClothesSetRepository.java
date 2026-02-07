package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.ClothesSet;
import com.example.Capstone_project.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClothesSetRepository extends JpaRepository<ClothesSet, Long> {
    // 특정 유저가 만든 세트 목록만 가져오기
    List<ClothesSet> findByUser(User user);
}