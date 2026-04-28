// controller/AuthController.java
package launchpath.userservice.co.controller;

import jakarta.validation.Valid;
import launchpath.userservice.co.dto.request.*;
import launchpath.userservice.co.dto.response.ApiResponseDTO;
import launchpath.userservice.co.dto.response.AuthResponseDTO;
import launchpath.userservice.co.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/v1/auth/register
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {

        AuthResponseDTO response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(
                        "Registration successful", response
                ));
    }

    // POST /api/v1/auth/login
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {

        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponseDTO.success("Login successful", response)
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {

        authService.sendForgotPasswordOtp(request.getEmail());

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "OTP sent to " + request.getEmail() +
                                ". Check your inbox.", null
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Password reset successfully. Please login.", null
                )
        );
    }

    @PostMapping("/send-verification")
    public ResponseEntity<ApiResponseDTO<Void>> sendVerification(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {

        authService.sendEmailVerificationOtp(request.getEmail());

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Verification OTP sent to " + request.getEmail(), null
                )
        );
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponseDTO<Void>> verifyEmail(
            @Valid @RequestBody VerifyOtpRequestDTO request) {

        authService.verifyEmail(request.getEmail(), request.getOtp());

        return ResponseEntity.ok(
                ApiResponseDTO.success("Email verified successfully!", null)
        );
    }

    @PostMapping("/login-otp/send")
    public ResponseEntity<ApiResponseDTO<Void>> sendLoginOtp(
            @Valid @RequestBody LoginWithOtpRequestDTO request) {

        authService.sendLoginOtp(request.getEmail());

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Login OTP sent to " + request.getEmail(), null
                )
        );
    }

    @PostMapping("/login-otp/verify")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> verifyLoginOtp(
            @Valid @RequestBody VerifyLoginOtpRequestDTO request) {

        AuthResponseDTO response = authService.verifyLoginOtp(request);

        return ResponseEntity.ok(
                ApiResponseDTO.success("Login successful", response)
        );
    }

    // POST /api/v1/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> refresh(
            @Valid @RequestBody RefreshTokenRequestDTO request) {

        AuthResponseDTO response =
                authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(
                ApiResponseDTO.success("Token refreshed", response)
        );
    }
}