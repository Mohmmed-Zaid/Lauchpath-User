package launchpath.userservice.co.services;

import launchpath.userservice.co.entities.Subscription;
import launchpath.userservice.co.enums.PlanName;
import launchpath.userservice.co.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import launchpath.userservice.co.service.SubscriptionService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SubscriptionService subscriptionService;

    // Razorpay credentials — loaded from application.properties
    // We add actual Razorpay SDK calls when we integrate in Phase 3
    @Value("${razorpay.key.id:test_key}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:test_secret}")
    private String razorpayKeySecret;

    // ══════════════════════════════════════════════════════════
    // ORDER CREATION
    // Called when user clicks "Upgrade Plan"
    // Returns order details → frontend sends to Razorpay SDK
    // ══════════════════════════════════════════════════════════

    public Map<String, Object> createOrder(Long userId,
                                           PlanName planName,
                                           int durationMonths) {
        log.info("Creating payment order - userId: {}, plan: {}, months: {}",
                userId, planName, durationMonths);

        // Get plan price from DB
        BigDecimal planPrice = subscriptionService
                .getPlanByName(planName)
                .getPriceInr();

        // Calculate total amount
        // Annual = 20% discount
        BigDecimal totalAmount = planPrice.multiply(
                BigDecimal.valueOf(durationMonths)
        );
        if (durationMonths == 12) {
            totalAmount = totalAmount.multiply(new BigDecimal("0.80"));
        }

        // Amount in paise (Razorpay requires paise — 1 INR = 100 paise)
        long amountInPaise = totalAmount
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        // TODO Phase 3: Replace with actual Razorpay SDK call
        // RazorpayClient client = new RazorpayClient(keyId, keySecret);
        // JSONObject orderRequest = new JSONObject();
        // orderRequest.put("amount", amountInPaise);
        // orderRequest.put("currency", "INR");
        // Order order = client.orders.create(orderRequest);

        // Stub response for now — replace with real Razorpay order
        Map<String, Object> orderResponse = new HashMap<>();
        orderResponse.put("orderId", "order_stub_" + userId + "_" + System.currentTimeMillis());
        orderResponse.put("amount", amountInPaise);
        orderResponse.put("currency", "INR");
        orderResponse.put("planName", planName.name());
        orderResponse.put("durationMonths", durationMonths);
        orderResponse.put("razorpayKeyId", razorpayKeyId);

        log.info("Payment order created for userId: {}, amount: {} paise",
                userId, amountInPaise);
        return orderResponse;
    }

    // ══════════════════════════════════════════════════════════
    // PAYMENT VERIFICATION
    // Called after frontend Razorpay SDK confirms payment
    // ══════════════════════════════════════════════════════════

    @Transactional
    public Subscription verifyAndActivate(Long userId,
                                          String razorpayOrderId,
                                          String razorpayPaymentId,
                                          String razorpaySignature,
                                          PlanName planName,
                                          int durationMonths) {
        log.info("Verifying payment - userId: {}, paymentId: {}",
                userId, razorpayPaymentId);

        // TODO Phase 3: Verify Razorpay signature
        // String generatedSignature = HmacSHA256(orderId + "|" + paymentId, secret)
        // if (!generatedSignature.equals(razorpaySignature)) throw exception

        // Stub verification — always passes for now
        boolean isValid = verifySignatureStub(
                razorpayOrderId, razorpayPaymentId, razorpaySignature
        );

        if (!isValid) {
            log.error("Payment signature invalid - userId: {}, paymentId: {}",
                    userId, razorpayPaymentId);
            throw new IllegalStateException(
                    "Payment verification failed. Contact support."
            );
        }

        // Activate subscription
        Subscription upgraded = subscriptionService.upgradePlan(
                userId, planName, razorpayPaymentId, durationMonths
        );

        log.info("Payment verified and plan activated - userId: {}, plan: {}",
                userId, planName);
        return upgraded;
    }

    // ══════════════════════════════════════════════════════════
    // WEBHOOK HANDLER
    // Razorpay calls this URL on payment events
    // More reliable than frontend callback (handles browser close etc)
    // ══════════════════════════════════════════════════════════

    @Transactional
    public void handleWebhook(String event,
                              String razorpaySubId,
                              String razorpayPaymentId) {
        log.info("Razorpay webhook - event: {}, subId: {}", event, razorpaySubId);

        switch (event) {
            case "subscription.activated" -> {
                subscriptionService.activateByRazorpaySubId(razorpaySubId);
                log.info("Subscription activated via webhook: {}", razorpaySubId);
            }
            case "subscription.cancelled" -> {
                Subscription sub = subscriptionService
                        .getByRazorpaySubId(razorpaySubId);
                subscriptionService.cancelSubscription(sub.getUser().getId());
                log.info("Subscription cancelled via webhook: {}", razorpaySubId);
            }
            case "payment.failed" -> {
                log.warn("Payment failed - subId: {}, paymentId: {}",
                        razorpaySubId, razorpayPaymentId);
                // TODO Phase 3: publish to notification queue — email user
            }
            default -> log.warn("Unhandled webhook event: {}", event);
        }
    }

    // ══════════════════════════════════════════════════════════
    // INVOICE / HISTORY
    // ══════════════════════════════════════════════════════════

    public Map<String, Object> getPaymentSummary(Long userId) {
        Subscription sub = subscriptionService.getByUserId(userId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("currentPlan", sub.getPlan().getName());
        summary.put("status", sub.getStatus());
        summary.put("startsAt", sub.getStartsAt());
        summary.put("expiresAt", sub.getExpiresAt());
        summary.put("razorpaySubId", sub.getRazorpaySubId());
        summary.put("planPrice", sub.getPlan().getPriceInr());

        log.debug("Payment summary fetched for userId: {}", userId);
        return summary;
    }

    // ══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════

    private boolean verifySignatureStub(String orderId,
                                        String paymentId,
                                        String signature) {
        // TODO Phase 3: Real HMAC-SHA256 verification
        // String data = orderId + "|" + paymentId;
        // Mac mac = Mac.getInstance("HmacSHA256");
        // mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        // String generated = Hex.encodeHex(mac.doFinal(data.getBytes()));
        // return generated.equals(signature);
        return true; // stub for now
    }
}