// ============================================================
// UserController.java
// ============================================================
package launchpath.userservice.co.controller;

import launchpath.userservice.co.dto.request.*;
import launchpath.userservice.co.dto.response.*;
import launchpath.userservice.co.entities.User;
import launchpath.userservice.co.mapper.UserMapper;
import launchpath.userservice.co.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    // ══════════════════════════════════════════════════════════
    // REGISTER
    // POST /api/v1/users/register
    // ══════════════════════════════════════════════════════════

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {

        log.info("Register request: {}", request.getEmail());

        User user = userService.registerUser(
                request.getEmail(),
                request.getPassword(),
                request.getFullName()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(
                        "User registered successfully",
                        userMapper.toUserResponseDTO(user)
                ));
    }

    // ══════════════════════════════════════════════════════════
    // LOGIN
    // POST /api/v1/users/login
    // No JWT yet — returns user object
    // JWT added in security phase
    // ══════════════════════════════════════════════════════════

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {

        log.info("Login request: {}", request.getEmail());

        User user = userService.loginUser(
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Login successful",
                        userMapper.toUserResponseDTO(user)
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET USER BY ID
    // GET /api/v1/users/{id}
    // ══════════════════════════════════════════════════════════

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getUserById(
            @PathVariable Long id) {

        log.info("Get user by id: {}", id);

        User user = userService.getUserById(id);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "User fetched successfully",
                        userMapper.toUserResponseDTO(user)
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET USER BY EMAIL
    // GET /api/v1/users/email/{email}
    // Used by resume-service via Feign to validate user exists
    // ══════════════════════════════════════════════════════════

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getUserByEmail(
            @PathVariable String email) {

        log.info("Get user by email: {}", email);

        User user = userService.getUserByEmail(email);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "User fetched successfully",
                        userMapper.toUserResponseDTO(user)
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // CHECK EMAIL EXISTS
    // GET /api/v1/users/exists?email=abc@gmail.com
    // Used by frontend for real-time email validation
    // ══════════════════════════════════════════════════════════

    @GetMapping("/exists")
    public ResponseEntity<ApiResponseDTO<Boolean>> emailExists(
            @RequestParam String email) {

        boolean exists = userService.emailExists(email);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Email check completed",
                        exists
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET ALL USERS — ADMIN ONLY
    // GET /api/v1/users?page=0&size=20&sort=createdAt,desc
    // requestingUserId passed as header — JWT will handle this later
    // ══════════════════════════════════════════════════════════

    @GetMapping
    public ResponseEntity<ApiResponseDTO<Page<UserResponseDTO>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @RequestHeader("X-User-Id") Long requestingUserId) {

        log.info("Get all users requested by userId: {}", requestingUserId);

        Page<UserResponseDTO> users = userService
                .getAllUsers(pageable, requestingUserId)
                .map(userMapper::toUserResponseDTO);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Users fetched successfully",
                        users
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // UPDATE PROFILE
    // PUT /api/v1/users/{id}/profile
    // X-User-Id header = who is making the request
    // JWT will replace this header in security phase
    // ══════════════════════════════════════════════════════════

    @PutMapping("/{id}/profile")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateProfile(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requestingUserId,
            @Valid @RequestBody UpdateProfileRequestDTO request) {

        log.info("Update profile - userId: {} by: {}", id, requestingUserId);

        User updated = userService.updateProfile(
                id,
                requestingUserId,
                request.getFullName(),
                request.getAvatarUrl()
        );

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Profile updated successfully",
                        userMapper.toUserResponseDTO(updated)
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // CHANGE PASSWORD
    // PUT /api/v1/users/{id}/password
    // ══════════════════════════════════════════════════════════

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponseDTO<Void>> changePassword(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requestingUserId,
            @Valid @RequestBody ChangePasswordRequestDTO request) {

        log.info("Change password - userId: {}", id);

        // Must be own account only — no admin override for password
        if (!id.equals(requestingUserId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponseDTO.error(
                            "You can only change your own password"
                    ));
        }

        userService.changePassword(
                id,
                request.getCurrentPassword(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(
                ApiResponseDTO.success("Password changed successfully")
        );
    }

    // ══════════════════════════════════════════════════════════
    // VERIFY EMAIL
    // PATCH /api/v1/users/{id}/verify
    // Called after user clicks email verification link
    // ══════════════════════════════════════════════════════════

    @PatchMapping("/{id}/verify")
    public ResponseEntity<ApiResponseDTO<Void>> verifyUser(
            @PathVariable Long id) {

        log.info("Verify user: {}", id);
        userService.verifyUser(id);

        return ResponseEntity.ok(
                ApiResponseDTO.success("Email verified successfully")
        );
    }

    // ══════════════════════════════════════════════════════════
    // UPDATE ROLE — ADMIN ONLY
    // PATCH /api/v1/users/{id}/role
    // ══════════════════════════════════════════════════════════

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateRole(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminUserId,
            @Valid @RequestBody UpdateRoleRequestDTO request) {

        log.info("Update role - userId: {} by admin: {}", id, adminUserId);

        User updated = userService.updateUserRole(
                id,
                request.getRole(),
                adminUserId
        );

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "User role updated successfully",
                        userMapper.toUserResponseDTO(updated)
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // DELETE USER
    // DELETE /api/v1/users/{id}
    // ══════════════════════════════════════════════════════════

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteUser(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requestingUserId) {

        log.info("Delete user: {} by: {}", id, requestingUserId);
        userService.deleteUser(id, requestingUserId);

        return ResponseEntity.ok(
                ApiResponseDTO.success("User deleted successfully")
        );
    }

    // ══════════════════════════════════════════════════════════
    // ADMIN STATS
    // GET /api/v1/users/stats/provider
    // ══════════════════════════════════════════════════════════

    @GetMapping("/stats/provider")
    public ResponseEntity<ApiResponseDTO<Object>> getUserStatsByProvider(
            @RequestHeader("X-User-Id") Long adminUserId) {

        log.info("Provider stats requested by adminId: {}", adminUserId);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Stats fetched successfully",
                        userService.getUserCountByProvider(adminUserId)
                )
        );
    }
}