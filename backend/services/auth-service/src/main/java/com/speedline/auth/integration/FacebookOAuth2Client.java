package com.speedline.auth.integration;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.exception.FacebookException;
import com.restfb.types.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FacebookOAuth2Client {

    @Value("${oauth2.facebook.app-id}")
    private String appId;

    @Value("${oauth2.facebook.app-secret}")
    private String appSecret;

    public FacebookUserInfo verifyToken(String accessToken) {
        try {
            FacebookClient facebookClient = new DefaultFacebookClient(
                    accessToken,
                    Version.LATEST
            );

            User user = facebookClient.fetchObject(
                    "me",
                    User.class,
                    Parameter.with("fields", "id,name,email,first_name,last_name,picture")
            );

            if (user == null || user.getEmail() == null) {
                log.error("Unable to get user info from Facebook or email not provided");
                throw new RuntimeException("Unable to get user info from Facebook");
            }

            return FacebookUserInfo.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .pictureUrl(user.getPicture() != null ? user.getPicture().getUrl() : null)
                    .build();

        } catch (FacebookException e) {
            log.error("Error verifying Facebook token", e);
            throw new RuntimeException("Failed to verify Facebook token", e);
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class FacebookUserInfo {
        private String userId;
        private String email;
        private String name;
        private String firstName;
        private String lastName;
        private String pictureUrl;
    }
}
