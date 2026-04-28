package launchpath.userservice.co.services;

import launchpath.userservice.co.dto.request.LoginRequestDTO;
import launchpath.userservice.co.dto.request.RegisterRequestDTO;
import launchpath.userservice.co.dto.request.ResetPasswordRequestDTO;
import launchpath.userservice.co.dto.request.VerifyLoginOtpRequestDTO;
import launchpath.userservice.co.dto.response.AuthResponseDTO;
import launchpath.userservice.co.entities.OtpRecord;
import launchpath.userservice.co.entities.User;
import launchpath.userservice.co.enums.AuthProvider;
import launchpath.userservice.co.exception.UnauthorizedAccessException;
import launchpath.userservice.co.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    // ── Register ──────────────────────────────────────────

    public AuthResponseDTO register(RegisterRequestDTO request) {
        log.info("Registering: {}", request.getEmail());

        User user = userService.registerUser(
                request.getEmail(),
                request.getPassword(),
                request.getFullName()
        );

        // Send welcome email async
        emailService.sendWelcomeEmail(
                user.getEmail(), user.getFullName()
        );

        // Send verification OTP async
        otpService.generateAndSend(
                user.getEmail(),
                OtpRecord.OtpPurpose.EMAIL_VERIFICATION
        );

        return buildAuthResponse(user);
    }

    // ── Login ─────────────────────────────────────────────

    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Login: {}", request.getEmail());

        User user = userService.getUserByEmail(request.getEmail());

        if (!passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            log.warn("Wrong password for: {}", request.getEmail());
            throw new UnauthorizedAccessException(
                    "Invalid email or password"
            );
        }

        return buildAuthResponse(user);
    }

    // ── Refresh Token ─────────────────────────────────────

    public AuthResponseDTO refresh(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new UnauthorizedAccessException(
                    "Invalid or expired refresh token"
            );
        }
        if (!"REFRESH".equals(jwtUtil.extractTokenType(refreshToken))) {
            throw new UnauthorizedAccessException("Not a refresh token");
        }

        Long userId = jwtUtil.extractUserId(refreshToken);
        User user = userService.getUserById(userId);
        return buildAuthResponse(user);
    }

    // ── Private ───────────────────────────────────────────

    private AuthResponseDTO buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    // ── Send Email Verification OTP ───────────────────────────

    public void sendEmailVerificationOtp(String email) {
        // Check user exists
        userService.getUserByEmail(email);

        otpService.generateAndSend(
                email,
                OtpRecord.OtpPurpose.EMAIL_VERIFICATION
        );
    }



// ── Verify Email ──────────────────────────────────────────

    @Transactional
    public void verifyEmail(String email, String otp) {
        otpService.verifyOtp(
                email, otp,
                OtpRecord.OtpPurpose.EMAIL_VERIFICATION
        );

        // Mark user as verified
        User user = userService.getUserByEmail(email);
        user.setIsVerified(true);
        userService.saveUser(user);

        log.info("Email verified for: {}", email);
    }

// ── Forgot Password — Send OTP ────────────────────────────

    public void sendForgotPasswordOtp(String email) {
        // Check user exists — if not throw 404
        User user = userService.getUserByEmail(email);

        // Only LOCAL users can reset password
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalStateException(
                    "This account uses Google login. " +
                            "Please login with Google."
            );
        }

        otpService.generateAndSend(
                email,
                OtpRecord.OtpPurpose.FORGOT_PASSWORD
        );
    }

// ── Reset Password ────────────────────────────────────────

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        // Verify OTP first
        otpService.verifyOtp(
                request.getEmail(),
                request.getOtp(),
                OtpRecord.OtpPurpose.FORGOT_PASSWORD
        );

        // Update password
        User user = userService.getUserByEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.saveUser(user);

        log.info("Password reset for: {}", request.getEmail());
    }

// ── Login With OTP — Send OTP ─────────────────────────────

    public void sendLoginOtp(String email) {
        userService.getUserByEmail(email); // throws if not found

        otpService.generateAndSend(
                email,
                OtpRecord.OtpPurpose.LOGIN_OTP
        );
    }

// ── Login With OTP — Verify and Login ────────────────────

    public AuthResponseDTO verifyLoginOtp(VerifyLoginOtpRequestDTO request) {
        otpService.verifyOtp(
                request.getEmail(),
                request.getOtp(),
                OtpRecord.OtpPurpose.LOGIN_OTP
        );

        User user = userService.getUserByEmail(request.getEmail());
        return buildAuthResponse(user);
    }
}