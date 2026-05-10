package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.Clothes;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.Capstone_project.domain.User;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClothesRepository extends JpaRepository<Clothes, Long> {
    /** 내 옷장 목록: 직접 등록한 옷만 (가상피팅 입력용 제외) */
    List<Clothes> findByUserAndInClosetTrueOrderByCreatedAtDesc(User user);
    /** 내 옷장 목록 - 카테고리 필터 */
    List<Clothes> findByUserAndInClosetTrueAndCategoryOrderByCreatedAtDesc(User user, String category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Clothes c WHERE c.id = :id")
    Optional<Clothes> findByIdForUpdate(@Param("id") Long id);
}