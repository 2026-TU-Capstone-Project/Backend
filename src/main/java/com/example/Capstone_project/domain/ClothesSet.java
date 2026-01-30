package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "clothes_sets")
public class ClothesSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String setName; // 예: "결혼식 하객 룩", "데이트 코디"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 세트에 포함된 옷들 (상의, 하의, 신발 등)
    @ManyToMany
    @JoinTable(
            name = "clothes_set_items",
            joinColumns = @JoinColumn(name = "clothes_set_id"),
            inverseJoinColumns = @JoinColumn(name = "clothes_id")
    )
    private List<Clothes> clothes = new ArrayList<>();

    // 이 세트로 만든 피팅 결과물들
    @OneToMany(mappedBy = "clothesSet", cascade = CascadeType.ALL)
    private List<FittingTask> fittingTasks = new ArrayList<>();
}