package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.FittingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FittingRepository extends JpaRepository<FittingTask, Long> {

    // soft delete된 task 제외
    @Query("SELECT ft FROM FittingTask ft WHERE ft.id = :id AND ft.deletedAt IS NULL")
    Optional<FittingTask> findActiveById(@Param("id") Long id);

    @Query("SELECT ft FROM FittingTask ft WHERE ft.userId = :userId AND ft.isSaved = true AND ft.deletedAt IS NULL")
    List<FittingTask> findByUserIdAndIsSavedTrue(@Param("userId") Long userId);

    @Query("SELECT COUNT(ft) > 0 FROM FittingTask ft WHERE ft.topId = :topId AND ft.deletedAt IS NULL")
    boolean existsByTopId(@Param("topId") Long topId);

    @Query("SELECT COUNT(ft) > 0 FROM FittingTask ft WHERE ft.bottomId = :bottomId AND ft.deletedAt IS NULL")
    boolean existsByBottomId(@Param("bottomId") Long bottomId);

    @Query("SELECT COUNT(ft) > 0 FROM FittingTask ft WHERE ft.isSaved = true AND ft.topId = :topId AND ft.deletedAt IS NULL")
    boolean existsByIsSavedTrueAndTopId(@Param("topId") Long topId);

    @Query("SELECT COUNT(ft) > 0 FROM FittingTask ft WHERE ft.isSaved = true AND ft.bottomId = :bottomId AND ft.deletedAt IS NULL")
    boolean existsByIsSavedTrueAndBottomId(@Param("bottomId") Long bottomId);

    @Modifying
    @Query("UPDATE FittingTask ft SET ft.topId = null WHERE ft.topId = :clothesId AND ft.deletedAt IS NULL")
    void clearTopIdByClothesId(@Param("clothesId") Long clothesId);

    @Modifying
    @Query("UPDATE FittingTask ft SET ft.bottomId = null WHERE ft.bottomId = :clothesId AND ft.deletedAt IS NULL")
    void clearBottomIdByClothesId(@Param("clothesId") Long clothesId);

    @Query("SELECT ft FROM FittingTask ft LEFT JOIN FETCH ft.top LEFT JOIN FETCH ft.bottom WHERE ft.id = :id AND ft.deletedAt IS NULL")
    Optional<FittingTask> findByIdWithClothes(@Param("id") Long id);

    @Query("SELECT DISTINCT ft FROM FittingTask ft LEFT JOIN FETCH ft.top LEFT JOIN FETCH ft.bottom WHERE ft.userId = :userId AND ft.isSaved = true AND ft.deletedAt IS NULL")
    List<FittingTask> findByUserIdAndIsSavedTrueWithClothes(@Param("userId") Long userId);

    @Query("SELECT DISTINCT ft FROM FittingTask ft LEFT JOIN FETCH ft.top LEFT JOIN FETCH ft.bottom WHERE ft.id IN :ids AND ft.deletedAt IS NULL")
    List<FittingTask> findAllByIdInWithClothes(@Param("ids") List<Long> ids);

    // 스케줄러용: soft delete된 지 1시간 이상 지난 task 조회 (GCS/Clothes 정리 대상)
    @Query("SELECT ft FROM FittingTask ft WHERE ft.deletedAt IS NOT NULL AND ft.deletedAt < :threshold")
    List<FittingTask> findAllSoftDeletedBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * pgvector 유사도 검색 + 거리(distance) 반환, maxDistance 이하만 반환
     */
    @Query(value = """
        SELECT ft.id, (ft.style_embedding <=> CAST(:queryVector AS vector)) AS distance
        FROM fitting_tasks ft
        WHERE ft.style_embedding IS NOT NULL
        AND ft.deleted_at IS NULL
        AND ft.is_saved = true
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

    /**
     * 내 옷장 전용: 동일 쿼리 + user_id로 해당 사용자 피팅만 검색.
     */
    @Query(value = """
        SELECT ft.id, (ft.style_embedding <=> CAST(:queryVector AS vector)) AS distance
        FROM fitting_tasks ft
        WHERE ft.style_embedding IS NOT NULL
        AND ft.deleted_at IS NULL
        AND ft.is_saved = true
        AND ft.user_id = :userId
        AND (:maxDistance IS NULL OR (ft.style_embedding <=> CAST(:queryVector AS vector)) <= :maxDistance)
        AND (:gender IS NULL OR ft.result_gender::text = :gender)
        ORDER BY distance
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarIdsWithDistanceByUser(
        @Param("queryVector") String queryVector,
        @Param("maxDistance") Double maxDistance,
        @Param("gender") String gender,
        @Param("userId") Long userId,
        @Param("limit") int limit
    );

    /**
     * 피드 전용: feeds에 올라온 fitting_task_id만 대상으로 유사도 검색.
     */
    @Query(value = """
        SELECT ft.id, (ft.style_embedding <=> CAST(:queryVector AS vector)) AS distance
        FROM fitting_tasks ft
        INNER JOIN feeds f ON f.fitting_task_id = ft.id AND f.deleted_at IS NULL
        WHERE ft.style_embedding IS NOT NULL
        AND ft.deleted_at IS NULL
        AND (:maxDistance IS NULL OR (ft.style_embedding <=> CAST(:queryVector AS vector)) <= :maxDistance)
        AND (:gender IS NULL OR ft.result_gender::text = :gender)
        ORDER BY distance
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarIdsWithDistanceFromFeed(
        @Param("queryVector") String queryVector,
        @Param("maxDistance") Double maxDistance,
        @Param("gender") String gender,
        @Param("limit") int limit
    );
}
