package launchpath.userservice.co.entities.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
@Getter
@Setter
@MappedSuperclass
// AuditingEntityListener auto-fills createdAt/updatedAt
// But you MUST add @EnableJpaAuditing on your main class
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
@NoArgsConstructor
public class BaseEntity {

        @CreatedDate
        @Column(nullable = false, updatable = false) // updatable=false → never changes after insert
        private LocalDateTime createdAt;

        @LastModifiedDate
        @Column(nullable = false)
        private LocalDateTime updatedAt;
    }


