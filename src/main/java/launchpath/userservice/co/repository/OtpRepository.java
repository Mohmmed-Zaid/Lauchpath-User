package launchpath.userservice.co.repository;

import launchpath.userservice.co.entities.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpRecord, Long> {

    Optional<OtpRecord> findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String email, OtpRecord.OtpPurpose purpose
    );

    // Delete all OTPs for email+purpose after use
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpRecord o WHERE o.email = :email AND o.purpose = :purpose")
    void deleteByEmailAndPurpose(String email, OtpRecord.OtpPurpose purpose);

    // Count OTPs sent in last 10 minutes — rate limit
    @Query("SELECT COUNT(o) FROM OtpRecord o WHERE o.email = :email " +
            "AND o.purpose = :purpose " +
            "AND o.createdAt > :since")
    long countRecentOtps(String email,
                         OtpRecord.OtpPurpose purpose,
                         java.time.LocalDateTime since);
}
