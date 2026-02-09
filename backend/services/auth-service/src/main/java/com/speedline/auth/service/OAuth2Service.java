package com.speedline.auth.service;

import com.speedline.auth.domain.AuthProvider;
import com.speedline.auth.integration.FacebookOAuth2Client;
import com.speedline.auth.integration.GoogleOAuth2Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service {

    private final GoogleOAuth2Client googleClient;
    private final FacebookOAuth2Client facebookClient;

    public OAuth2UserInfo verifyAndGetUserInfo(String accessToken, AuthProvider provider) {
        log.info("Verifying {} token", provider);
        
        switch (provider) {
            case GOOGLE:
                GoogleOAuth2Client.GoogleUserInfo googleInfo = googleClient.verifyToken(accessToken);
                return OAuth2UserInfo.builder()
                        .providerId(googleInfo.getUserId())
                        .email(googleInfo.getEmail())
                        .firstName(googleInfo.getGivenName())
                        .lastName(googleInfo.getFamilyName())
                        .profilePicture(googleInfo.getPictureUrl())
                        .emailVerified(googleInfo.getEmailVerified())
                        .provider(AuthProvider.GOOGLE)
                        .build();
                        
            case FACEBOOK:
                FacebookOAuth2Client.FacebookUserInfo facebookInfo = facebookClient.verifyToken(accessToken);
                return OAuth2UserInfo.builder()
                        .providerId(facebookInfo.getUserId())
                        .email(facebookInfo.getEmail())
                        .firstName(facebookInfo.getFirstName())
                        .lastName(facebookInfo.getLastName())
                        .profilePicture(facebookInfo.getPictureUrl())
                        .emailVerified(true) // Facebook requires verified email
                        .provider(AuthProvider.FACEBOOK)
                        .build();
                        
            default:
                throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class OAuth2UserInfo {
        private String providerId;
        private String email;
        private String firstName;
        private String lastName;
        private String profilePicture;
        private Boolean emailVerified;
        private AuthProvider provider;
    }
}
