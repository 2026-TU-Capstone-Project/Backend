package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.Capstone_project.domain.User;
import java.util.List;

@Repository
public interface ClothesRepository extends JpaRepository<Clothes, Long> {
    List<Clothes> findByUserOrderByCreatedAtDesc(User user);
    List<Clothes> findByUserAndCategoryOrderByCreatedAtDesc(User user, String category);
}