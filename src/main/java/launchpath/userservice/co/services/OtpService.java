package launchpath.userservice.co.services;

import launchpath.userservice.co.entities.OtpRecord;
import launchpath.userservice.co.exception.ResourceNotFoundException;
import launchpath.userservice.co.exception.UnauthorizedAccessException;
import launchpath.userservice.co.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    @Value("${otp.expiry.minutes:10}")
    private int otpExpiryMinutes;

    private final SecureRandom random = new SecureRandom();

    // ── Generate and Send OTP ─────────────────────────────

    @Transactional
    public void generateAndSend(String email,
                                OtpRecord.OtpPurpose purpose) {

        // Rate limit: max 3 OTPs per 10 minutes per email+purpose
        long recentCount = otpRepository.countRecentOtps(
                email, purpose,
                LocalDateTime.now().minusMinutes(10)
        );

        if (recentCount >= 3) {
            throw new IllegalStateException(
                    "Too many OTP requests. Please wait 10 minutes."
            );
        }

        // Delete previous unused OTPs for this email+purpose
        otpRepository.deleteByEmailAndPurpose(email, purpose);

        // Generate 6-digit OTP
        String code = String.format("%06d",
                random.nextInt(999999)
        );

        OtpRecord record = OtpRecord.builder()
                .email(email)
                .code(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpRepository.save(record);

        // Send email async — won't block response
        emailService.sendOtpEmail(email, code, purpose.name());

        log.info("OTP generated for email: {}, purpose: {}",
                email, purpose);
    }

    // ── Verify OTP ────────────────────────────────────────

    @Transactional
    public void verifyOtp(String email,
                          String code,
                          OtpRecord.OtpPurpose purpose) {

        OtpRecord record = otpRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        email, purpose
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active OTP found. Please request a new one."
                ));

        if (record.isExpired()) {
            throw new UnauthorizedAccessException(
                    "OTP has expired. Please request a new one."
            );
        }

        if (!record.getCode().equals(code)) {
            throw new UnauthorizedAccessException(
                    "Invalid OTP. Please try again."
            );
        }

        // Mark as used
        record.setUsed(true);
        otpRepository.save(record);

        // Clean up all OTPs for this email+purpose
        otpRepository.deleteByEmailAndPurpose(email, purpose);

        log.info("OTP verified for email: {}, purpose: {}", email, purpose);
    }
}