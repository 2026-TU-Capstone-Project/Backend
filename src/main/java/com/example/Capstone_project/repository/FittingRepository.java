package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.FittingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FittingRepository extends JpaRepository<FittingTask, Long> {

    public interface FittingRepository extends JpaRepository<FittingTask, Long> {

        List<FittingTask> findByUserIdAndIsSavedTrue(Long userId);
    }
    /**
     * pgvector 코사인 유사도 검색 (<=> 연산자)
     * style_embedding은 TEXT에 "[0.1,0.2,...]" 형식으로 저장, 쿼리에서 vector로 캐스팅
     * queryVector: "[0.1,0.2,...]" 형식의 1536차원 벡터 문자열
     * 최대 10개 반환
     */
    @Query(value = """
        SELECT * FROM fitting_tasks
        WHERE style_embedding IS NOT NULL
        ORDER BY style_embedding <=> CAST(:queryVector AS vector)
        LIMIT 10
        """, nativeQuery = true)
    List<FittingTask> findSimilarByStyleEmbedding(@Param("queryVector") String queryVector);

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