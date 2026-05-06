package com.example.Capstone_project.service;

import com.example.Capstone_project.common.exception.BadRequestException;
import com.example.Capstone_project.common.exception.ForbiddenException;
import com.example.Capstone_project.common.exception.ResourceNotFoundException;
import com.example.Capstone_project.domain.Follow;
import com.example.Capstone_project.domain.FollowStatus;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.FollowRequestItemDto;
import com.example.Capstone_project.dto.FollowUserDto;
import com.example.Capstone_project.dto.UserPublicProfileResponseDto;
import com.example.Capstone_project.repository.FollowRepository;
import com.example.Capstone_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    /** 팔로우 신청 */
    @Transactional
    public void sendFollowRequest(Long followerId, Long targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new BadRequestException("자기 자신을 팔로우할 수 없습니다.");
        }
        User follower = findUser(followerId);
        User following = findUser(targetUserId);

        followRepository.findByFollowerIdAndFollowingId(followerId, targetUserId)
                .ifPresent(f -> {
                    if (f.getStatus() == FollowStatus.PENDING) {
                        throw new BadRequestException("이미 팔로우 신청 중입니다.");
                    }
                    throw new BadRequestException("이미 팔로우 중입니다.");
                });

        followRepository.save(Follow.of(follower, following));
    }

    /** 팔로우 취소 (신청 중) 또는 언팔로우 (팔로우 중) */
    @Transactional
    public void cancelOrUnfollow(Long followerId, Long targetUserId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("팔로우 관계가 없습니다."));
        followRepository.delete(follow);
    }

    /** 팔로우 신청 수락 */
    @Transactional
    public void acceptFollow(Long followId, Long userId) {
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new ResourceNotFoundException("팔로우 신청을 찾을 수 없습니다."));
        if (!follow.getFollowing().getId().equals(userId)) {
            throw new ForbiddenException("본인에게 온 팔로우 신청만 수락할 수 있습니다.");
        }
        if (follow.getStatus() != FollowStatus.PENDING) {
            throw new BadRequestException("이미 처리된 신청입니다.");
        }
        follow.accept();
    }

    /** 팔로우 신청 거절 (Follow 레코드 삭제 → 재신청 허용) */
    @Transactional
    public void rejectFollow(Long followId, Long userId) {
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new ResourceNotFoundException("팔로우 신청을 찾을 수 없습니다."));
        if (!follow.getFollowing().getId().equals(userId)) {
            throw new ForbiddenException("본인에게 온 팔로우 신청만 거절할 수 있습니다.");
        }
        if (follow.getStatus() != FollowStatus.PENDING) {
            throw new BadRequestException("이미 처리된 신청입니다.");
        }
        followRepository.delete(follow);
    }

    /** 받은 팔로우 신청 목록 (PENDING) */
    @Transactional(readOnly = true)
    public List<FollowRequestItemDto> getPendingRequests(Long userId) {
        return followRepository.findPendingRequestsToMe(userId).stream()
                .map(FollowRequestItemDto::from)
                .collect(Collectors.toList());
    }

    /** 내 팔로워 목록 (ACCEPTED) */
    @Transactional(readOnly = true)
    public List<FollowUserDto> getFollowers(Long userId) {
        return followRepository.findFollowers(userId).stream()
                .map(FollowUserDto::fromFollower)
                .collect(Collectors.toList());
    }

    /** 내 팔로잉 목록 (ACCEPTED) */
    @Transactional(readOnly = true)
    public List<FollowUserDto> getFollowings(Long userId) {
        return followRepository.findFollowings(userId).stream()
                .map(FollowUserDto::fromFollowing)
                .collect(Collectors.toList());
    }

    /** 다른 유저 공개 프로필 조회 (팔로워/팔로잉 수 + 나와의 관계 포함) */
    @Transactional(readOnly = true)
    public UserPublicProfileResponseDto getPublicProfile(Long targetUserId, Long requesterId) {
        User target = findUser(targetUserId);
        long followerCount = followRepository.countByFollowingIdAndStatus(targetUserId, FollowStatus.ACCEPTED);
        long followingCount = followRepository.countByFollowerIdAndStatus(targetUserId, FollowStatus.ACCEPTED);

        String followStatus = null;
        if (requesterId != null && !requesterId.equals(targetUserId)) {
            followStatus = followRepository.findByFollowerIdAndFollowingId(requesterId, targetUserId)
                    .map(f -> f.getStatus().name())
                    .orElse(null);
        }

        boolean isMe = requesterId != null && requesterId.equals(targetUserId);
        return UserPublicProfileResponseDto.from(target, followerCount, followingCount, followStatus, isMe);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));
    }
}
