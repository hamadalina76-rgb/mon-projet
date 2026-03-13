package com.speedline.auth.service;

import com.speedline.auth.domain.Otp;
import com.speedline.auth.repository.OtpRepository;
import com.speedline.auth.util.EmailTemplateLoader;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing OTP authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final JavaMailSender mailSender;
    private final EmailTemplateLoader templateLoader;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    @Value("${otp.expiration.minutes:15}")
    private int otpExpirationMinutes;
    
    @Value("${frontend.url:http://localhost:4300}")
    private String frontendUrl;
    
    @Value("${mail.dev-mode:true}")
    private boolean mailDevMode;
    
    private static final int MAX_ATTEMPTS = 3;
    private static final SecureRandom random = new SecureRandom();
    private static final String OTP_EMAIL_TEMPLATE_PATH = "templates/otp-email.html";
    
    private String emailTemplate;
    
    @PostConstruct
    public void init() {
        loadEmailTemplate();
    }
    
    private void loadEmailTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource(OTP_EMAIL_TEMPLATE_PATH);
            emailTemplate = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            log.info("OTP email template loaded successfully");
        } catch (IOException e) {
            log.error("Failed to load OTP email template: {}", e.getMessage());
            throw new RuntimeException("Failed to load email template", e);
        }
    }

    /**
     * Generate and send OTP to user's email
     */
    @Transactional
    public void generateAndSendOtp(String email, String firstName) {
        log.info("Generating OTP for email: {}", email);
        
        // Delete any existing OTPs for this email
        otpRepository.deleteByEmail(email);
        
        // Generate 6-digit OTP
        String otpCode = generateOtpCode();
        
        // Create OTP entity
        Otp otp = Otp.builder()
                .email(email)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .build();
        
        otpRepository.save(otp);
        log.info("OTP generated for user {}: {}", email, otpCode);
        
        // Send OTP via email (or log in dev mode if SMTP disabled)
        if (mailDevMode) {
            log.warn("MAIL_DEV_MODE=true : OTP non envoyé par email. Utilisez ce code pour {} : {}", email, otpCode);
            return;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("spring.mail.username non configuré : OTP non envoyé. Utilisez ce code pour {} : {}", email, otpCode);
            return;
        }
        try {
            sendOtpEmail(email, firstName, otpCode);
            log.info("OTP sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send email to: {}. Error: {} - Vérifiez SMTP (port 465 ou 587, pare-feu) ou activez MAIL_DEV_MODE=true", email, e.getMessage());
            log.warn("OTP pour {} (saisir ce code pour continuer) : {}", email, otpCode);
            // Ne pas faire échouer le login : l'utilisateur peut récupérer le code dans les logs.
        }
    }

    /**
     * Verify OTP code
     */
    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        log.info("Verifying OTP for email: {}", email);
        
        return otpRepository.findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc(email)
                .filter(otp -> isValidOtpCode(otp, otpCode, email))
                .filter(otp -> isNotExpired(otp, email))
                .filter(otp -> hasRemainingAttempts(otp, email))
                .map(otp -> markOtpAsUsed(otp, email))
                .orElseGet(() -> {
                    log.warn("No valid OTP found for user {}", email);
                    return false;
                });
    }
    
    private boolean isValidOtpCode(Otp otp, String otpCode, String email) {
        return Optional.of(otp)
                .filter(o -> o.getOtpCode().equals(otpCode))
                .map(o -> true)
                .orElseGet(() -> {
                    otp.setAttemptsCount(otp.getAttemptsCount() + 1);
                    otpRepository.save(otp);
                    log.warn("Invalid OTP for user {}", email);
                    return false;
                });
    }
    
    private boolean isNotExpired(Otp otp, String email) {
        return Optional.of(otp)
                .filter(o -> LocalDateTime.now().isBefore(o.getExpiresAt()))
                .map(o -> true)
                .orElseGet(() -> {
                    log.warn("OTP expired for user {}", email);
                    return false;
                });
    }
    
    private boolean hasRemainingAttempts(Otp otp, String email) {
        return Optional.of(otp)
                .filter(o -> o.getAttemptsCount() < MAX_ATTEMPTS)
                .map(o -> true)
                .orElseGet(() -> {
                    log.warn("Max OTP attempts exceeded for user {}", email);
                    return false;
                });
    }
    
    private boolean markOtpAsUsed(Otp otp, String email) {
        otp.setIsUsed(true);
        otpRepository.save(otp);
        log.info("OTP verified successfully for email: {}", email);
        return true;
    }

    /**
     * Clean up expired OTPs (can be scheduled)
     */
    @Transactional
    public void cleanupExpiredOtps() {
        log.info("Cleaning up expired OTPs");
        otpRepository.deleteExpiredOtps(LocalDateTime.now());
    }

    /**
     * Generate random 6-digit OTP code
     */
    private String generateOtpCode() {
        int otp = 100_000 + random.nextInt(900_000); // Generates number between 100000 and 999999
        return String.valueOf(otp);
    }

    /**
     * Send OTP via email
     */
    private void sendOtpEmail(String email, String firstName, String otpCode) throws MessagingException {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new MessagingException("Sender email (spring.mail.username) is not configured");
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(email);
        helper.setSubject("🔐 Your SpeedLine Verification Code / Votre code de vérification SpeedLine");

        String emailContent = emailTemplate
                .replace("{{firstName}}", firstName)
                .replace("{{otpCode}}", otpCode)
                .replace("{{expirationMinutes}}", String.valueOf(otpExpirationMinutes));

        helper.setText(emailContent, true);
        
        try {
            mailSender.send(message);
            log.info("Email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send email to: {}. Error: {}", email, e.getMessage());
            throw e;
        }
    }

    /**
     * Send welcome email with credentials to new admin
     */
    public void sendAdminWelcomeEmail(String email, String fullName, String temporaryPassword) {
        log.info("Sending welcome email to new admin: {}", email);
        
        if (mailDevMode) {
            log.warn("MAIL_DEV_MODE=true : Email admin non envoyé. Credentials pour {} : password={}", email, temporaryPassword);
            return;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("spring.mail.username non configuré : email admin non envoyé. Credentials pour {} : password={}", email, temporaryPassword);
            return;
        }
        try {
            // Charger et formater le template
            Map<String, String> variables = new HashMap<>();
            variables.put("fullName", fullName);
            variables.put("email", email);
            variables.put("temporaryPassword", temporaryPassword);
            variables.put("loginUrl", frontendUrl + "/auth/login");
            
            String emailContent = templateLoader.loadAndFormat("admin-welcome-email.html", variables);
            
            // Créer et envoyer l'email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Bienvenue sur SpeedLine - Vos identifiants administrateur");
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            
            log.info("Welcome email sent successfully to: {}", email);
        } catch (IOException e) {
            log.error("Failed to load email template. Error: {}", e.getMessage());
            throw new OtpEmailException("Failed to load email template", e);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}. Error: {}", email, e.getMessage());
            throw new OtpEmailException("Failed to send welcome email", e);
        }
    }
    
    /**
     * Custom exception for OTP email sending failures
     */
    public static class OtpEmailException extends RuntimeException {
        public OtpEmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
