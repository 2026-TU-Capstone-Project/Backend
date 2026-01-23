package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.repository.ClothesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClothesService {

    private final ClothesRepository clothesRepository;

    // 1. 옷 저장 기능
    @Transactional
    public void saveClothes(Clothes clothes) {
        clothesRepository.save(clothes);
    }

    // 2. 옷 목록 조회 기능
    @Transactional(readOnly = true)
    public List<Clothes> findClothes() {
        return clothesRepository.findAll();
    }
}