package launchpath.userservice.co.services;

import launchpath.userservice.co.entities.Subscription;
import launchpath.userservice.co.enums.PlanName;
import launchpath.userservice.co.exception.ResourceNotFoundException;
import launchpath.userservice.co.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final SubscriptionService subscriptionService;

    // ── Coupon map ─────────────────────────────────────────────
    // Key = coupon code, Value = PlanName it unlocks
    // Phase 3: move this to a DB table (coupon entity)
    private static final Map<String, PlanName> COUPON_MAP = Map.of(
            "STARTER100", PlanName.STARTER,
            "PRIME100",   PlanName.PRIME,
            "AGENCY100",  PlanName.AGENCY
    );

    // Duration granted by coupon (months).
    // Phase 3: store this per-coupon in DB.
    private static final int COUPON_DURATION_MONTHS = 1;

    public Subscription redeemCoupon(Long userId, String couponCode) {
        log.info("Redeeming coupon - userId: {}, code: {}", userId, couponCode);

        String normalised = couponCode.trim().toUpperCase();

        PlanName targetPlan = COUPON_MAP.get(normalised);
        if (targetPlan == null) {
            log.warn("Invalid coupon code: {} for userId: {}", couponCode, userId);
            throw new IllegalArgumentException("Invalid coupon code: " + couponCode);
        }

        // Re-use existing upgradePlan — passes coupon code as the "payment id" reference
        Subscription upgraded = subscriptionService.upgradePlan(
                userId,
                targetPlan,
                "COUPON_" + normalised,   // stored as razorpaySubId — harmless placeholder
                COUPON_DURATION_MONTHS
        );

        log.info("Coupon redeemed - userId: {}, plan: {}", userId, targetPlan);
        return upgraded;
    }
}