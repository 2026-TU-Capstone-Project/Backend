package com.example.Capstone_project.repository; // 패키지명 확인

import com.example.Capstone_project.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username); //아아디로 유저 찾기
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}