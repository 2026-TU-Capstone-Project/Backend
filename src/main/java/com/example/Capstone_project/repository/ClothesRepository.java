package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.Capstone_project.domain.User;
import java.util.List;

@Repository
public interface ClothesRepository extends JpaRepository<Clothes, Long> {
    // 나중에 검색 기능 필요하면 여기에 추가 (findByCategory 등)
    List<Clothes> findByUserOrderByCreatedAtDesc(User user);
}