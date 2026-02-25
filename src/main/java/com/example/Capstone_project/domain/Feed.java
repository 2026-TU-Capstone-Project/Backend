package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "feeds")
@Getter
@Setter
@NoArgsConstructor
public class Feed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "feed_title", nullable = false, length = 200)
    private String feedTitle;

    @Column(name = "feed_content", columnDefinition = "TEXT")
    private String feedContent;

    @Column(name = "style_image_url", length = 500)
    private String styleImageUrl;

    @Column(name = "fitting_task_id")
    private Long fittingTaskId;

    @Column(name = "top_image_url", length = 500)
    private String topImageUrl;

    @Column(name = "top_name", length = 200)
    private String topName;

    @Column(name = "top_clothes_id")
    private Long topClothesId;

    @Column(name = "bottom_image_url", length = 500)
    private String bottomImageUrl;

    @Column(name = "bottom_name", length = 200)
    private String bottomName;

    @Column(name = "bottom_clothes_id")
    private Long bottomClothesId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
