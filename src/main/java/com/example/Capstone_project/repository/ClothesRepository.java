package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.Capstone_project.domain.User;
import java.util.List;

@Repository
public interface ClothesRepository extends JpaRepository<Clothes, Long> {
    /** 내 옷장 목록: 직접 등록한 옷만 (가상피팅 입력용 제외) */
    List<Clothes> findByUserAndInClosetTrueOrderByCreatedAtDesc(User user);
    /** 내 옷장 목록 - 카테고리 필터 */
    List<Clothes> findByUserAndInClosetTrueAndCategoryOrderByCreatedAtDesc(User user, String category);
}