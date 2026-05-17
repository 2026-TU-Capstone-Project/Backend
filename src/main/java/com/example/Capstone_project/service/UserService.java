package com.example.Capstone_project.service;

import com.example.Capstone_project.common.exception.BadRequestException;
import com.example.Capstone_project.common.exception.ResourceNotFoundException;
import com.example.Capstone_project.domain.Follow;
import com.example.Capstone_project.domain.FollowStatus;
import com.example.Capstone_project.domain.Gender;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.UserPublicProfileResponseDto;
import com.example.Capstone_project.dto.UserProfileResponseDto;
import com.example.Capstone_project.dto.UserProfileUpdateDto;
import com.example.Capstone_project.dto.UserSearchResponseDto;
import com.example.Capstone_project.repository.FollowRepository;
import com.example.Capstone_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GoogleCloudStorageService gcsService;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public UserProfileResponseDto getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        long followerCount = followRepository.countByFollowingIdAndStatus(userId, FollowStatus.ACCEPTED);
        long followingCount = followRepository.countByFollowerIdAndStatus(userId, FollowStatus.ACCEPTED);
        return UserProfileResponseDto.from(user, followerCount, followingCount);
    }

    @Transactional
    public UserProfileResponseDto updateMyProfile(Long userId, UserProfileUpdateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            user.setNickname(dto.getNickname().trim());
        }
        if (dto.getProfileImageUrl() != null && !dto.getProfileImageUrl().isBlank()) {
            user.setProfileImageUrl(dto.getProfileImageUrl());
        }
        if (dto.getHeight() != null) {
            user.setHeight(dto.getHeight());
        }
        if (dto.getWeight() != null) {
            user.setWeight(dto.getWeight());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }

        userRepository.save(user);
        long followerCount = followRepository.countByFollowingIdAndStatus(userId, FollowStatus.ACCEPTED);
        long followingCount = followRepository.countByFollowerIdAndStatus(userId, FollowStatus.ACCEPTED);
        return UserProfileResponseDto.from(user, followerCount, followingCount);
    }

    /**
     * 마이페이지 수정하기: 닉네임, 프로필 이미지, 키, 몸무게, 성별을 한 번에 갱신.
     * 보내진 값만 수정하며, 프로필 이미지 파일이 있으면 GCS 업로드 후 URL 저장.
     */
    /** username + nickname 포함해서 한 번에 수정 */
    @Transactional
    public UserProfileResponseDto updateMyProfileWithForm(
            Long userId,
            String username,
            String nickname,
            Float height,
            Float weight,
            String gender,
            byte[] profileImageBytes,
            String profileImageFilename,
            String profileImageContentType
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 빈 문자열/공백만 있으면 "수정하지 않음"으로 간주 (기존 값 유지)
        if (username != null && !username.isBlank()) {
            String trimmedUsername = username.trim();
            if (!trimmedUsername.matches("^[a-zA-Z0-9_]{3,30}$")) {
                throw new BadRequestException("username은 영문, 숫자, 언더스코어(_)만 사용 가능하며 3~30자여야 합니다.");
            }
            if (!trimmedUsername.equals(user.getUsername()) && userRepository.existsByUsername(trimmedUsername)) {
                throw new BadRequestException("이미 사용 중인 username입니다.");
            }
            user.setUsername(trimmedUsername);
        }
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname.trim());
        }
        if (height != null) {
            user.setHeight(height);
        }
        if (weight != null) {
            user.setWeight(weight);
        }
        if (gender != null && !gender.isBlank()) {
            try {
                user.setGender(Gender.valueOf(gender.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // MALE, FEMALE 외 값은 무시
            }
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
        long followerCount = followRepository.countByFollowingIdAndStatus(userId, FollowStatus.ACCEPTED);
        long followingCount = followRepository.countByFollowerIdAndStatus(userId, FollowStatus.ACCEPTED);
        return UserProfileResponseDto.from(user, followerCount, followingCount);
    }

    /** username / 이름으로 유저 검색 (최대 20명) */
    @Transactional(readOnly = true)
    public List<UserSearchResponseDto> searchUsers(String keyword, Long requesterId) {
        List<User> users = userRepository.searchByUsernameOrNickname(keyword, PageRequest.of(0, 20));
        return users.stream()
                .map(u -> {
                    String followStatus = null;
                    if (requesterId != null && !requesterId.equals(u.getId())) {
                        followStatus = followRepository.findByFollowerIdAndFollowingId(requesterId, u.getId())
                                .map(f -> f.getStatus().name())
                                .orElse(null);
                    }
                    return UserSearchResponseDto.from(u, followStatus);
                })
                .collect(Collectors.toList());
    }
}
