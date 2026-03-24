package launchpath.userservice.co.service;

import launchpath.userservice.co.entities.Plan;
import launchpath.userservice.co.entities.Subscription;
import launchpath.userservice.co.enums.PlanName;
import launchpath.userservice.co.enums.SubscriptionStatus;
import launchpath.userservice.co.exception.InsufficientCreditsException;
import launchpath.userservice.co.exception.ResourceNotFoundException;
import launchpath.userservice.co.exception.UnauthorizedAccessException;
import launchpath.userservice.co.repository.PlanRepository;
import launchpath.userservice.co.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    // ══════════════════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Subscription getByUserId(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subscription not found for userId: " + userId
                ));
    }

    @Transactional(readOnly = true)
    public Subscription getByRazorpaySubId(String razorpaySubId) {
        return subscriptionRepository.findByRazorpaySubId(razorpaySubId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subscription not found for razorpaySubId: " + razorpaySubId
                ));
    }

    @Transactional(readOnly = true)
    public int getRemainingAtsCredits(Long userId) {
        Subscription sub = getByUserId(userId);
        int total = sub.getPlan().getAtsCredits();
        if (total == -1) return Integer.MAX_VALUE; // unlimited
        return total - sub.getAtsCreditsUsed();
    }

    @Transactional(readOnly = true)
    public int getRemainingDownloads(Long userId) {
        Subscription sub = getByUserId(userId);
        int total = sub.getPlan().getResumeDownloads();
        if (total == -1) return Integer.MAX_VALUE;
        return total - sub.getDownloadsUsed();
    }

    @Transactional(readOnly = true)
    public boolean isSubscriptionActive(Long userId) {
        Subscription sub = getByUserId(userId);
        if (sub.getExpiresAt() == null) return true; // FREE never expires
        return sub.getStatus() == SubscriptionStatus.ACTIVE
                && sub.getExpiresAt().isAfter(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public boolean canCreateMoreResumes(Long userId, long currentResumeCount) {
        Subscription sub = getByUserId(userId);
        int maxResumes = sub.getPlan().getMaxResumes();
        if (maxResumes == -1) return true; // unlimited
        return currentResumeCount < maxResumes;
    }

    @Transactional(readOnly = true)
    public boolean canAccessTemplate(Long userId, boolean isPremiumTemplate) {
        if (!isPremiumTemplate) return true; // basic templates always accessible
        Subscription sub = getByUserId(userId);
        // Only STARTER, PRIME, AGENCY can access premium templates
        return sub.getPlan().getName() != PlanName.FREE;
    }

    @Transactional(readOnly = true)
    public List<Plan> getAllPlans() {
        return planRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Plan getPlanByName(PlanName planName) {
        return planRepository.findByName(planName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan not found: " + planName
                ));
    }

    // ══════════════════════════════════════════════════════════
    // CREDIT OPERATIONS
    // Called by resume-service via Feign before AI/export ops
    // ══════════════════════════════════════════════════════════

    @Transactional
    public void consumeAtsCredit(Long userId) {
        log.info("Consuming ATS credit for userId: {}", userId);

        if (!isSubscriptionActive(userId)) {
            throw new UnauthorizedAccessException(
                    "Your subscription has expired. Please renew."
            );
        }

        Subscription sub = getByUserId(userId);
        Plan plan = sub.getPlan();

        if (plan.getAtsCredits() != -1) {
            int remaining = plan.getAtsCredits() - sub.getAtsCreditsUsed();
            log.debug("ATS credits - total: {}, used: {}, remaining: {}",
                    plan.getAtsCredits(), sub.getAtsCreditsUsed(), remaining);

            if (remaining <= 0) {
                log.warn("ATS credits exhausted for userId: {}", userId);
                throw new InsufficientCreditsException(
                        "ATS credits exhausted. Upgrade your plan to get more."
                );
            }
        }

        // Atomic DB-level increment — no race condition
        subscriptionRepository.incrementAtsCreditsUsed(userId);
        log.info("ATS credit consumed for userId: {}", userId);
    }

    @Transactional
    public void consumeDownloadCredit(Long userId) {
        log.info("Consuming download credit for userId: {}", userId);

        if (!isSubscriptionActive(userId)) {
            throw new UnauthorizedAccessException(
                    "Your subscription has expired. Please renew."
            );
        }

        Subscription sub = getByUserId(userId);
        Plan plan = sub.getPlan();

        if (plan.getResumeDownloads() != -1) {
            int remaining = plan.getResumeDownloads() - sub.getDownloadsUsed();
            log.debug("Downloads - total: {}, used: {}, remaining: {}",
                    plan.getResumeDownloads(), sub.getDownloadsUsed(), remaining);

            if (remaining <= 0) {
                log.warn("Download limit reached for userId: {}", userId);
                throw new InsufficientCreditsException(
                        "Download limit reached. Upgrade your plan for more downloads."
                );
            }
        }

        subscriptionRepository.incrementDownloadsUsed(userId);
        log.info("Download credit consumed for userId: {}", userId);
    }

    // Refund credit — called when AI fails so user not penalized
    @Transactional
    public void refundAtsCredit(Long userId) {
        log.info("Refunding ATS credit for userId: {}", userId);
        Subscription sub = getByUserId(userId);

        // Don't go below 0
        if (sub.getAtsCreditsUsed() > 0) {
            sub.setAtsCreditsUsed(sub.getAtsCreditsUsed() - 1);
            subscriptionRepository.save(sub);
            log.info("ATS credit refunded for userId: {}", userId);
        }
    }

    @Transactional
    public void refundDownloadCredit(Long userId) {
        log.info("Refunding download credit for userId: {}", userId);
        Subscription sub = getByUserId(userId);

        if (sub.getDownloadsUsed() > 0) {
            sub.setDownloadsUsed(sub.getDownloadsUsed() - 1);
            subscriptionRepository.save(sub);
            log.info("Download credit refunded for userId: {}", userId);
        }
    }

    // ══════════════════════════════════════════════════════════
    // PLAN MANAGEMENT
    // ══════════════════════════════════════════════════════════

    @Transactional
    public Subscription upgradePlan(Long userId,
                                    PlanName newPlanName,
                                    String razorpaySubId,
                                    int durationMonths) {
        log.info("Upgrading plan - userId: {}, plan: {}, months: {}",
                userId, newPlanName, durationMonths);

        Subscription sub = getByUserId(userId);

        Plan newPlan = planRepository.findByName(newPlanName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan not found: " + newPlanName
                ));

        sub.setPlan(newPlan);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setAtsCreditsUsed(0);       // reset on upgrade
        sub.setDownloadsUsed(0);
        sub.setStartsAt(LocalDateTime.now());
        sub.setExpiresAt(LocalDateTime.now().plusMonths(durationMonths));
        sub.setRazorpaySubId(razorpaySubId);

        Subscription saved = subscriptionRepository.save(sub);
        log.info("Plan upgraded for userId: {}", userId);
        return saved;
    }

    @Transactional
    public Subscription cancelSubscription(Long userId) {
        log.info("Cancelling subscription for userId: {}", userId);

        Subscription sub = getByUserId(userId);

        if (sub.getPlan().getName() == PlanName.FREE) {
            throw new IllegalStateException("Cannot cancel a FREE plan");
        }

        Plan freePlan = planRepository.findByName(PlanName.FREE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FREE plan not found"
                ));

        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setPlan(freePlan);
        sub.setAtsCreditsUsed(0);
        sub.setDownloadsUsed(0);
        sub.setExpiresAt(null);
        sub.setRazorpaySubId(null);

        log.info("Subscription cancelled, downgraded to FREE for userId: {}", userId);
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public void activateByRazorpaySubId(String razorpaySubId) {
        log.info("Activating subscription: {}", razorpaySubId);

        Subscription sub = subscriptionRepository
                .findByRazorpaySubId(razorpaySubId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subscription not found for razorpaySubId: " + razorpaySubId
                ));

        sub.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(sub);
        log.info("Subscription activated: {}", razorpaySubId);
    }

    // ══════════════════════════════════════════════════════════
    // SCHEDULED JOBS
    // ══════════════════════════════════════════════════════════

    // Every day midnight — expire outdated subscriptions
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireSubscriptions() {
        log.info("Running expiry job at: {}", LocalDateTime.now());

        List<Subscription> expired = subscriptionRepository
                .findExpiredSubscriptions(LocalDateTime.now());

        if (expired.isEmpty()) {
            log.info("No subscriptions to expire");
            return;
        }

        Plan freePlan = planRepository.findByName(PlanName.FREE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FREE plan not found"
                ));

        expired.forEach(sub -> {
            log.info("Expiring subscription for userId: {}",
                    sub.getUser().getId());
            sub.setStatus(SubscriptionStatus.EXPIRED);
            sub.setPlan(freePlan);
            sub.setAtsCreditsUsed(0);
            sub.setDownloadsUsed(0);
        });

        subscriptionRepository.saveAll(expired);
        log.info("Expired {} subscriptions", expired.size());
    }

    // Every hour — log subscriptions expiring in 3 days
    // TODO: publish to RabbitMQ for email notification in Phase 3
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void warnExpiringSubscriptions() {
        LocalDateTime threeDaysFromNow = LocalDateTime.now().plusDays(3);
        LocalDateTime now = LocalDateTime.now();

        List<Subscription> expiringSoon = subscriptionRepository
                .findExpiredSubscriptions(threeDaysFromNow)
                .stream()
                .filter(sub -> sub.getExpiresAt().isAfter(now))
                .toList();

        if (!expiringSoon.isEmpty()) {
            log.info("{} subscriptions expiring in 3 days — " +
                            "email notification queue goes here (Phase 3)",
                    expiringSoon.size());
        }
    }
}