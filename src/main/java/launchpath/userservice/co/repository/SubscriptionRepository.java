package launchpath.userservice.co.repository;

import launchpath.userservice.co.entities.Subscription;
import launchpath.userservice.co.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // Most frequent query in entire app — every API call checks this
    // user_id is FK in subscriptions table, Spring resolves user.id automatically
    Optional<Subscription> findByUserId(Long userId);

    // Used for Razorpay webhook — payment comes in with razorpay sub id
    Optional<Subscription> findByRazorpaySubId(String razorpaySubId);

    // Find active subscriptions by status — admin use
    List<Subscription> findByStatus(SubscriptionStatus status);

    // Scheduled job will call this to expire outdated subscriptions
    // JPQL: find all ACTIVE subs where expiresAt is not null AND already passed
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' " +
            "AND s.expiresAt IS NOT NULL AND s.expiresAt < :now")
    List<Subscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    // Deduct credit atomically at DB level — safer than read→modify→save
    // Prevents race condition if two requests come in simultaneously
    // @Modifying + @Transactional on service method = atomic operation
    @Modifying
    @Query("UPDATE Subscription s SET s.atsCreditsUsed = s.atsCreditsUsed + 1 " +
            "WHERE s.user.id = :userId")
    void incrementAtsCreditsUsed(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Subscription s SET s.downloadsUsed = s.downloadsUsed + 1 " +
            "WHERE s.user.id = :userId")
    void incrementDownloadsUsed(@Param("userId") Long userId);
}
