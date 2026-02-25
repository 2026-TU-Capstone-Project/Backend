package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.UserProfileResponseDto;
import com.example.Capstone_project.dto.UserProfileUpdateDto;
import com.example.Capstone_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GoogleCloudStorageService gcsService;

    @Transactional(readOnly = true)
    public UserProfileResponseDto getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return UserProfileResponseDto.from(user);
    }

    @Transactional
    public UserProfileResponseDto updateMyProfile(Long userId, UserProfileUpdateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getProfileImageUrl() != null) {
            user.setProfileImageUrl(dto.getProfileImageUrl());
        }
        if (dto.getHeight() != null) {
            user.setHeight(dto.getHeight());
        }
        if (dto.getWeight() != null) {
            user.setWeight(dto.getWeight());
        }

        userRepository.save(user);
        return UserProfileResponseDto.from(user);
    }

    /**
     * 마이페이지 수정하기: 닉네임, 프로필 이미지, 키, 몸무게를 한 번에 갱신.
     * 보내진 값만 수정하며, 프로필 이미지 파일이 있으면 GCS 업로드 후 URL 저장.
     */
    @Transactional
    public UserProfileResponseDto updateMyProfileWithForm(
            Long userId,
            String nickname,
            Float height,
            Float weight,
            byte[] profileImageBytes,
            String profileImageFilename,
            String profileImageContentType
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (height != null) {
            user.setHeight(height);
        }
        if (weight != null) {
            user.setWeight(weight);
        }
        if (profileImageBytes != null && profileImageBytes.length > 0) {
            String extension = (profileImageFilename != null && profileImageFilename.contains("."))
                    ? profileImageFilename.substring(profileImageFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = "user-" + userId + "-" + UUID.randomUUID().toString() + extension;
            String profileImageUrl = gcsService.uploadProfileImage(
                    profileImageBytes,
                    filename,
                    profileImageContentType != null ? profileImageContentType : "image/jpeg"
            );
            user.setProfileImageUrl(profileImageUrl);
        }

        userRepository.save(user);
        return UserProfileResponseDto.from(user);
    }
}
