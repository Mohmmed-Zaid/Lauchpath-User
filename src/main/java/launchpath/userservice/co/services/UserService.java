package launchpath.userservice.co.services;

import launchpath.userservice.co.entities.Plan;
import launchpath.userservice.co.entities.Subscription;
import launchpath.userservice.co.entities.User;
import launchpath.userservice.co.enums.AuthProvider;
import launchpath.userservice.co.enums.PlanName;
import launchpath.userservice.co.enums.SubscriptionStatus;
import launchpath.userservice.co.enums.UserRole;
import launchpath.userservice.co.exception.ResourceAlreadyExistsException;
import launchpath.userservice.co.exception.ResourceNotFoundException;
import launchpath.userservice.co.exception.UnauthorizedAccessException;
import launchpath.userservice.co.repository.PlanRepository;
import launchpath.userservice.co.repository.SubscriptionRepository;
import launchpath.userservice.co.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    // ══════════════════════════════════════════════════════════
    // REGISTRATION — no BCrypt yet, plain password for now
    // We add password encoding when we wire security later
    // ══════════════════════════════════════════════════════════

    @Transactional
    public User registerUser(String email, String password, String fullName) {
        log.info("Registering new user: {}", email);

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed - email exists: {}", email);
            throw new ResourceAlreadyExistsException(
                    "Email already registered: " + email
            );
        }

        validatePasswordStrength(password);

        // Storing plain password for now
        // BCrypt encoding added when we implement security layer
        User user = User.builder()
                .email(email.toLowerCase().trim())
                .password(password)
                .fullName(fullName.trim())
                .provider(AuthProvider.LOCAL)
                .role(UserRole.USER)
                .isVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User saved with id: {}", savedUser.getId());

        assignFreePlan(savedUser);

        return savedUser;
    }

    // ══════════════════════════════════════════════════════════
    // LOGIN — plain password check for now
    // Spring Security + BCrypt added in security phase
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public User loginUser(String email, String password) {
        log.info("Login attempt for: {}", email);

        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with email: " + email
                ));

        // Plain comparison now — BCrypt.matches() added in security phase
        if (!user.getPassword().equals(password)) {
            log.warn("Login failed - wrong password for: {}", email);
            throw new UnauthorizedAccessException("Incorrect password");
        }

        log.info("Login successful for userId: {}", user.getId());
        return user;
    }

    // ══════════════════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id
                ));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email
                ));
    }

    // Admin — paginated user list
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable, Long requestingUserId) {
        User requester = getUserById(requestingUserId);
        if (requester.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedAccessException("Admin access required");
        }
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    @Transactional(readOnly = true)
    public List<Object[]> getUserCountByProvider(Long requestingUserId) {
        User requester = getUserById(requestingUserId);
        if (requester.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedAccessException("Admin access required");
        }
        return userRepository.countByProvider();
    }

    // ══════════════════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════════════════

    @Transactional
    public User updateProfile(Long userId,
                              Long requestingUserId,
                              String fullName,
                              String avatarUrl) {
        log.info("Profile update - userId: {} by: {}", userId, requestingUserId);

        // Only self or admin can update
        if (!userId.equals(requestingUserId)) {
            User requester = getUserById(requestingUserId);
            if (requester.getRole() != UserRole.ADMIN) {
                throw new UnauthorizedAccessException(
                        "You can only update your own profile"
                );
            }
        }

        User user = getUserById(userId);

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarUrl(avatarUrl);
        }

        // Hibernate dirty checking — no explicit save() needed
        // Changes auto-committed when transaction closes
        log.info("Profile updated for userId: {}", userId);
        return user;
    }

    @Transactional
    public void changePassword(Long userId,
                               String currentPassword,
                               String newPassword) {
        log.info("Password change for userId: {}", userId);

        User user = getUserById(userId);

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new UnauthorizedAccessException(
                    "Password change not available for OAuth accounts"
            );
        }

        // Plain comparison now — BCrypt added in security phase
        if (!user.getPassword().equals(currentPassword)) {
            log.warn("Wrong current password for userId: {}", userId);
            throw new UnauthorizedAccessException("Current password is incorrect");
        }

        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException(
                    "New password must be different from current password"
            );
        }

        validatePasswordStrength(newPassword);
        user.setPassword(newPassword);
        log.info("Password changed for userId: {}", userId);
    }

    @Transactional
    public void verifyUser(Long userId) {
        log.info("Verifying user: {}", userId);
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        userRepository.verifyUser(userId);
        log.info("User verified: {}", userId);
    }

    @Transactional
    public User updateUserRole(Long targetUserId,
                               UserRole newRole,
                               Long adminUserId) {
        log.info("Role update - target: {} role: {} by admin: {}",
                targetUserId, newRole, adminUserId);

        User admin = getUserById(adminUserId);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedAccessException("Only admins can change roles");
        }

        User target = getUserById(targetUserId);
        target.setRole(newRole);
        log.info("Role updated for userId: {}", targetUserId);
        return target;
    }

    // ══════════════════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════════════════

    @Transactional
    public void deleteUser(Long userId, Long requestingUserId) {
        log.info("Delete user: {} by: {}", userId, requestingUserId);

        if (!userId.equals(requestingUserId)) {
            User requester = getUserById(requestingUserId);
            if (requester.getRole() != UserRole.ADMIN) {
                throw new UnauthorizedAccessException(
                        "You can only delete your own account"
                );
            }
        }

        User user = getUserById(userId);
        userRepository.delete(user);
        log.info("User deleted: {}", userId);
    }

    // ══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════

    private void assignFreePlan(User user) {
        log.debug("Assigning FREE plan to userId: {}", user.getId());

        Plan freePlan = planRepository.findByName(PlanName.FREE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FREE plan not found — run PlanSeeder first"
                ));

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(freePlan)
                .status(SubscriptionStatus.ACTIVE)
                .atsCreditsUsed(0)
                .downloadsUsed(0)
                .startsAt(LocalDateTime.now())
                .expiresAt(null)
                .build();

        subscriptionRepository.save(subscription);
        log.info("FREE plan assigned to userId: {}", user.getId());
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters"
            );
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter"
            );
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException(
                    "Password must contain at least one number"
            );
        }
        if (!password.matches(".*[!@#$%^&*()].*")) {
            throw new IllegalArgumentException(
                    "Password must contain at least one special character"
            );
        }
    }
}