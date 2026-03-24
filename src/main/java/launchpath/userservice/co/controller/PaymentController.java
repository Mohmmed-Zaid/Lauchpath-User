// ============================================================
// PaymentController.java
// ============================================================
package launchpath.userservice.co.controller;

import launchpath.userservice.co.dto.request.CreateOrderRequestDTO;
import launchpath.userservice.co.dto.request.UpgradePlanRequestDTO;
import launchpath.userservice.co.dto.request.WebhookRequestDTO;
import launchpath.userservice.co.dto.response.ApiResponseDTO;
import launchpath.userservice.co.dto.response.SubscriptionResponseDTO;
import launchpath.userservice.co.entities.Subscription;
import launchpath.userservice.co.mapper.UserMapper;
import launchpath.userservice.co.services.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserMapper userMapper;

    // ══════════════════════════════════════════════════════════
    // CREATE RAZORPAY ORDER
    // POST /api/v1/payments/create-order
    // Step 1 of payment flow — frontend gets order details
    // then opens Razorpay checkout UI
    // ══════════════════════════════════════════════════════════

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateOrderRequestDTO request) {

        log.info("Create order - userId: {}, plan: {}", userId, request.getPlanName());

        Map<String, Object> order = paymentService.createOrder(
                userId,
                request.getPlanName(),
                request.getDurationMonths()
        );

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Order created successfully",
                        order
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // VERIFY PAYMENT AND ACTIVATE PLAN
    // POST /api/v1/payments/verify
    // Step 2 — after Razorpay checkout completes on frontend
    // Frontend sends payment IDs here for server-side verification
    // ══════════════════════════════════════════════════════════

    @PostMapping("/verify")
    public ResponseEntity<ApiResponseDTO<SubscriptionResponseDTO>> verifyPayment(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpgradePlanRequestDTO request) {

        log.info("Verify payment - userId: {}, paymentId: {}",
                userId, request.getRazorpayPaymentId());

        Subscription upgraded = paymentService.verifyAndActivate(
                userId,
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                request.getPlanName(),
                request.getDurationMonths()
        );

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Payment verified. Plan activated successfully.",
                        userMapper.toSubscriptionResponseDTO(upgraded)
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // RAZORPAY WEBHOOK
    // POST /api/v1/payments/webhook
    // Razorpay calls this directly — no X-User-Id header
    // Handles: payment success, failure, subscription events
    // ══════════════════════════════════════════════════════════

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponseDTO<Void>> handleWebhook(
            @RequestBody WebhookRequestDTO request) {

        log.info("Webhook received - event: {}", request.getEvent());

        paymentService.handleWebhook(
                request.getEvent(),
                request.getRazorpaySubId(),
                request.getRazorpayPaymentId()
        );

        // Always return 200 to Razorpay — even on error
        // Otherwise Razorpay retries the webhook endlessly
        return ResponseEntity.ok(
                ApiResponseDTO.success("Webhook processed")
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET PAYMENT SUMMARY
    // GET /api/v1/payments/summary
    // Shows current plan, billing dates, Razorpay sub ID
    // ══════════════════════════════════════════════════════════

    @GetMapping("/summary")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getPaymentSummary(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Payment summary for userId: {}", userId);

        Map<String, Object> summary = paymentService.getPaymentSummary(userId);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Payment summary fetched",
                        summary
                )
        );
    }
}