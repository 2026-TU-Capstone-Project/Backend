package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.domain.ClothesSet;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.repository.ClothesRepository;
import com.example.Capstone_project.repository.ClothesSetRepository;
import com.example.Capstone_project.repository.FittingRepository;
import com.example.Capstone_project.dto.ClothesSetResponseDto;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClothesSetService {

    private final ClothesSetRepository clothesSetRepository;
    private final ClothesRepository clothesRepository;
    private final FittingRepository fittingRepository;


    /**
     * [추가된 기능] 코디 세트(폴더) 이름 수정
     */
    @Transactional
    public void updateSetName(Long setId, String newName, User user) {
        ClothesSet clothesSet = clothesSetRepository.findById(setId)
                .orElseThrow(() -> new RuntimeException("세트를 찾을 수 없습니다."));

        // 권한 확인
        if (!clothesSet.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }

        // 엔티티의 필드명이 setName이므로 롬복이 만든 setSetName 호출
        clothesSet.setSetName(newName);
    }

    /**
     * [저장] 마음에 드는 착장을 폴더(세트)에 저장
     */
    @Transactional
    public ClothesSet saveFavoriteCoordination(String setName, List<Long> clothesIds, Long fittingTaskId, User user) {
        List<Clothes> clothesList = clothesRepository.findAllById(clothesIds);

        // [수정] FittingRepository.findById (대문자X) -> fittingRepository (소문자 변수)
        FittingTask fittingTask = fittingRepository.findById(fittingTaskId)
                .orElseThrow(() -> new RuntimeException("피팅 결과를 찾을 수 없습니다."));

        ClothesSet clothesSet = ClothesSet.builder()
                .setName(setName)
                .user(user)
                .clothes(clothesList)
                .build();

        ClothesSet savedSet = clothesSetRepository.save(clothesSet);

        fittingTask.setClothesSet(savedSet);
        fittingTask.setSaved(true);

        return savedSet;
    }

    /**
     * [조회] 내 코디 세트(폴더) 전체 조회
     */
    @Transactional(readOnly = true)
    public List<ClothesSetResponseDto> getMySets(User user) {
        List<ClothesSet> clothesSets = clothesSetRepository.findByUser(user);

        return clothesSets.stream()
                .map(set -> {
                    // 해당 폴더에 담긴 피팅 결과 중 가장 최근 것을 대표 이미지로 선정
                    String mainImg = set.getFittingTasks().isEmpty() ? null :
                            set.getFittingTasks().get(set.getFittingTasks().size() - 1).getResultImgUrl();

                    // 보강된 DTO 구조에 맞춰 변환
                    return new ClothesSetResponseDto(
                            set.getId(),
                            set.getSetName(),
                            mainImg, // 대표 이미지 주소 전달
                            set.getFittingTasks().stream()
                                    .map(f -> new ClothesSetResponseDto.FittingDto(f.getId(), f.getResultImgUrl()))
                                    .collect(Collectors.toList()),
                            set.getClothes().stream()
                                    .map(c -> new ClothesSetResponseDto.ClothesDto(c.getId(), c.getName(), c.getCategory(), c.getImgUrl()))
                                    .collect(Collectors.toList())
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * [추가 기능] 폴더 내 특정 착장(착샷) 개별 삭제
     */
    @Transactional
    public void deleteFittingFromSet(Long fittingTaskId, User user) {
        // 1. 삭제할 착장 찾기
        FittingTask task = fittingRepository.findById(fittingTaskId)
                .orElseThrow(() -> new RuntimeException("해당 착장을 찾을 수 없습니다."));

        // 2. 권한 확인
        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }

        // 3. 폴더와의 연결 고리 끊기
        task.setClothesSet(null);

        // 4. 착장 데이터 삭제
        fittingRepository.delete(task);
    }


    /**
     * [삭제] 코디 세트(폴더) 삭제
     */
    @Transactional
    public void deleteSet(Long setId, User user) {
        ClothesSet clothesSet = clothesSetRepository.findById(setId)
                .orElseThrow(() -> new RuntimeException("세트를 찾을 수 없습니다."));

        // 권한 확인
        if (!clothesSet.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }

        if (clothesSet.getFittingTasks() != null) {
            for (FittingTask task : clothesSet.getFittingTasks()) {
                task.setClothesSet(null);
                task.setSaved(false);
            }
        }

        clothesSetRepository.delete(clothesSet);
    }
}