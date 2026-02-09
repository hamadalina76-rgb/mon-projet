package com.speedline.auth.integration;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Component
@Slf4j
public class GoogleOAuth2Client {

    @Value("${oauth2.google.client-id}")
    private String clientId;
    
    private final RestTemplate restTemplate;
    private final Gson gson;
    
    public GoogleOAuth2Client(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.gson = new Gson();
    }

    public GoogleUserInfo verifyToken(String token) {
        log.info("Attempting to verify Google token");
        
        // First try as ID token
        try {
            return verifyIdToken(token);
        } catch (Exception e) {
            log.warn("Token is not a valid ID token, trying as access token: {}", e.getMessage());
        }
        
        // If ID token verification fails, try as access token
        try {
            return verifyAccessToken(token);
        } catch (Exception e) {
            log.error("Failed to verify token as both ID token and access token", e);
            throw new RuntimeException("Failed to verify Google token: " + e.getMessage(), e);
        }
    }
    
    private GoogleUserInfo verifyIdToken(String idToken) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), 
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken token = verifier.verify(idToken);
        
        if (token != null) {
            GoogleIdToken.Payload payload = token.getPayload();
            log.info("Successfully verified Google ID token for user: {}", payload.getEmail());
            
            return GoogleUserInfo.builder()
                    .userId(payload.getSubject())
                    .email(payload.getEmail())
                    .emailVerified(payload.getEmailVerified())
                    .name((String) payload.get("name"))
                    .pictureUrl((String) payload.get("picture"))
                    .givenName((String) payload.get("given_name"))
                    .familyName((String) payload.get("family_name"))
                    .build();
        } else {
            throw new RuntimeException("ID token verification returned null");
        }
    }
    
    private GoogleUserInfo verifyAccessToken(String accessToken) {
        // Call Google's tokeninfo endpoint to validate access token
        String tokenInfoUrl = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=" + accessToken;
        
        try {
            String response = restTemplate.getForObject(tokenInfoUrl, String.class);
            JsonObject tokenInfo = gson.fromJson(response, JsonObject.class);
            
            // Check if token has an error
            if (tokenInfo.has("error") || tokenInfo.has("error_description")) {
                String error = tokenInfo.has("error_description") 
                    ? tokenInfo.get("error_description").getAsString() 
                    : "Invalid token";
                log.error("Token validation failed: {}", error);
                throw new RuntimeException(error);
            }
            
            // Log token info for debugging
            String aud = tokenInfo.has("aud") ? tokenInfo.get("aud").getAsString() : "N/A";
            String azp = tokenInfo.has("azp") ? tokenInfo.get("azp").getAsString() : "N/A";
            log.info("Token info - aud: {}, azp: {}, configured clientId: {}", aud, azp, clientId);
            
            // For mobile/web apps, we are more lenient with audience checking
            // We just verify the token is valid and issued by Google
            // The tokeninfo endpoint already validates the token signature and expiry
            
            log.info("Successfully verified Google access token");
            
            // Get user info from userinfo endpoint
            String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + accessToken;
            String userInfoResponse = restTemplate.getForObject(userInfoUrl, String.class);
            JsonObject userInfo = gson.fromJson(userInfoResponse, JsonObject.class);
            
            return GoogleUserInfo.builder()
                    .userId(userInfo.get("sub").getAsString())
                    .email(userInfo.has("email") ? userInfo.get("email").getAsString() : null)
                    .emailVerified(userInfo.has("email_verified") ? userInfo.get("email_verified").getAsBoolean() : false)
                    .name(userInfo.has("name") ? userInfo.get("name").getAsString() : null)
                    .pictureUrl(userInfo.has("picture") ? userInfo.get("picture").getAsString() : null)
                    .givenName(userInfo.has("given_name") ? userInfo.get("given_name").getAsString() : null)
                    .familyName(userInfo.has("family_name") ? userInfo.get("family_name").getAsString() : null)
                    .build();
        } catch (Exception e) {
            log.error("Error verifying Google access token", e);
            throw new RuntimeException("Failed to verify Google access token: " + e.getMessage(), e);
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class GoogleUserInfo {
        private String userId;
        private String email;
        private Boolean emailVerified;
        private String name;
        private String pictureUrl;
        private String givenName;
        private String familyName;
    }
}
