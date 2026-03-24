package launchpath.userservice.co.entities;

import jakarta.persistence.*;
import launchpath.userservice.co.entities.base.BaseEntity;
import launchpath.userservice.co.enums.AuthProvider;
import launchpath.userservice.co.enums.UserRole;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
    @Table(
            name = "users",
            uniqueConstraints = {
                    @UniqueConstraint(columnNames = "email") // DB-level uniqueness, not just app-level
            }
    )
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public class User extends BaseEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
        private Long id;

        @Column(nullable = false, length = 255)
        private String email;

        // nullable = true → OAuth users have no password
        @Column(length = 255)
        private String password;

        @Column(name = "full_name", length = 255)
        private String fullName;

        @Column(name = "avatar_url", length = 512)
        private String avatarUrl;

        @Enumerated(EnumType.STRING) // Store "GOOGLE" not "1" in DB
        @Column(nullable = false, length = 20)
        private AuthProvider provider = AuthProvider.LOCAL;

        // OAuth provider's unique ID for this user (Google sub, GitHub id)
        @Column(name = "provider_id", length = 255)
        private String providerId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private UserRole role = UserRole.USER;

        @Column(name = "is_verified", nullable = false)
        private Boolean isVerified = false;

        // One user has one active subscription
        // mappedBy = field name in Subscription that owns the FK
        // CascadeType.ALL → if user deleted, subscription deleted too
        // fetch = LAZY → don't load subscription unless explicitly called
        //   (EAGER would load subscription on EVERY user query — bad for performance)
        @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private Subscription subscription;

        // One user can have many file records
        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private java.util.List<FileRecord> fileRecords;
    }