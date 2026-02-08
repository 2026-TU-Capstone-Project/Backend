package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.FittingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FittingRepository extends JpaRepository<FittingTask, Long> {

    List<FittingTask> findByUserIdAndIsSavedTrue(Long userId);

    @Query("SELECT DISTINCT ft FROM FittingTask ft LEFT JOIN FETCH ft.top LEFT JOIN FETCH ft.bottom WHERE ft.userId = :userId AND ft.isSaved = true")
    List<FittingTask> findByUserIdAndIsSavedTrueWithClothes(@Param("userId") Long userId);

    /**
     * pgvector 유사도 검색 + 거리(distance) 반환, maxDistance 이하만 반환
     * distance: 0=동일, 낮을수록 유사. 유사도 score = 1 - distance
     * maxDistance: null이면 필터 없음
     * gender: null이면 필터 없음. 'MALE'/'FEMALE'이면 유저 프로필 성별과 fitting_task.result_gender가 같은 것만 추천
     */
    @Query(value = """
        SELECT ft.id, (ft.style_embedding <=> CAST(:queryVector AS vector)) AS distance
        FROM fitting_tasks ft
        WHERE ft.style_embedding IS NOT NULL
        AND (:maxDistance IS NULL OR (ft.style_embedding <=> CAST(:queryVector AS vector)) <= :maxDistance)
        AND (:gender IS NULL OR ft.result_gender::text = :gender)
        ORDER BY distance
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarIdsWithDistance(
        @Param("queryVector") String queryVector,
        @Param("maxDistance") Double maxDistance,
        @Param("gender") String gender,
        @Param("limit") int limit
    );
}