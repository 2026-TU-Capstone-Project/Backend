package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.SocialProvider;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 소셜 서비스에서 유저 정보를 가져옴
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 어느 서비스(kakao, google)인지 확인
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 서비스별로 데이터 추출 방법이 다르므로 가공함
        String email;
        String nickname;
        String profileImageUrl;
        SocialProvider provider;

        if (registrationId.equals("kakao")) {
            Map<String, Object> attributes = oAuth2User.getAttributes();
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            email = (String) kakaoAccount.get("email");
            nickname = (String) profile.get("nickname");
            profileImageUrl = (String) profile.get("profile_image_url");
            provider = SocialProvider.KAKAO;
        } else { // google
            email = oAuth2User.getAttribute("email");
            nickname = oAuth2User.getAttribute("name");
            profileImageUrl = oAuth2User.getAttribute("picture");
            provider = SocialProvider.GOOGLE;
        }

        // DB에 저장하거나 이미 있으면 업데이트
        saveOrUpdate(email, nickname, profileImageUrl, provider);

        return oAuth2User;
    }

    private void saveOrUpdate(String email, String nickname, String profileImageUrl, SocialProvider provider) {
        User user = userRepository.findByEmail(email)
                .map(entity -> {
                    entity.setNickname(nickname);
                    entity.setProfileImageUrl(profileImageUrl);
                    entity.setSocialProvider(provider);
                    return entity;
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setNickname(nickname);
                    newUser.setProfileImageUrl(profileImageUrl);
                    newUser.setSocialProvider(provider);
                    newUser.setPassword("OAUTH2_USER_" + UUID.randomUUID().toString().substring(0, 8));
                    newUser.setUsername(provider.name() + "_" + UUID.randomUUID().toString().substring(0, 8));
                    newUser.setRole("ROLE_USER");
                    return newUser;
                });

        userRepository.save(user);
    }
}