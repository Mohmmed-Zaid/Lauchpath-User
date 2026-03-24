package launchpath.userservice.co.dto.response;

import launchpath.userservice.co.enums.AuthProvider;
import launchpath.userservice.co.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponseDTO {

    private Long id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private AuthProvider provider;
    private UserRole role;
    private Boolean isVerified;
    private LocalDateTime createdAt;

    // Nested — frontend always needs plan info alongside user
    private SubscriptionResponseDTO subscription;
}