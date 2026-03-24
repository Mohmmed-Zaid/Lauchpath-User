// ============================================================
// SubscriptionController.java
// ============================================================
package launchpath.userservice.co.controller;

import launchpath.userservice.co.dto.response.ApiResponseDTO;
import launchpath.userservice.co.dto.response.PlanResponseDTO;
import launchpath.userservice.co.dto.response.SubscriptionResponseDTO;
import launchpath.userservice.co.entities.Subscription;
import launchpath.userservice.co.mapper.UserMapper;
import launchpath.userservice.co.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserMapper userMapper;

    // ══════════════════════════════════════════════════════════
    // GET MY SUBSCRIPTION
    // GET /api/v1/subscriptions/my
    // ══════════════════════════════════════════════════════════

    @GetMapping("/my")
    public ResponseEntity<ApiResponseDTO<SubscriptionResponseDTO>> getMySubscription(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Get subscription for userId: {}", userId);

        Subscription sub = subscriptionService.getByUserId(userId);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Subscription fetched successfully",
                        userMapper.toSubscriptionResponseDTO(sub)
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET REMAINING ATS CREDITS
    // GET /api/v1/subscriptions/my/ats-credits
    // Called by resume-service via Feign before ATS analysis
    // ══════════════════════════════════════════════════════════

    @GetMapping("/my/ats-credits")
    public ResponseEntity<ApiResponseDTO<Integer>> getRemainingAtsCredits(
            @RequestHeader("X-User-Id") Long userId) {

        int remaining = subscriptionService.getRemainingAtsCredits(userId);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "ATS credits fetched",
                        remaining
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET REMAINING DOWNLOADS
    // GET /api/v1/subscriptions/my/downloads
    // ══════════════════════════════════════════════════════════

    @GetMapping("/my/downloads")
    public ResponseEntity<ApiResponseDTO<Integer>> getRemainingDownloads(
            @RequestHeader("X-User-Id") Long userId) {

        int remaining = subscriptionService.getRemainingDownloads(userId);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Downloads remaining fetched",
                        remaining
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // CHECK SUBSCRIPTION ACTIVE
    // GET /api/v1/subscriptions/my/active
    // Used by resume-service via Feign
    // ══════════════════════════════════════════════════════════

    @GetMapping("/my/active")
    public ResponseEntity<ApiResponseDTO<Boolean>> isSubscriptionActive(
            @RequestHeader("X-User-Id") Long userId) {

        boolean active = subscriptionService.isSubscriptionActive(userId);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Subscription status checked",
                        active
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // CHECK CAN CREATE MORE RESUMES
    // GET /api/v1/subscriptions/my/can-create?currentCount=3
    // ══════════════════════════════════════════════════════════

    @GetMapping("/my/can-create")
    public ResponseEntity<ApiResponseDTO<Boolean>> canCreateMoreResumes(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam long currentCount) {

        boolean canCreate = subscriptionService
                .canCreateMoreResumes(userId, currentCount);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Resume limit checked",
                        canCreate
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // CONSUME ATS CREDIT
    // POST /api/v1/subscriptions/my/consume-ats
    // Called by resume-service via Feign before every AI call
    // ══════════════════════════════════════════════════════════

    @PostMapping("/my/consume-ats")
    public ResponseEntity<ApiResponseDTO<Void>> consumeAtsCredit(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Consume ATS credit for userId: {}", userId);
        subscriptionService.consumeAtsCredit(userId);

        return ResponseEntity.ok(
                ApiResponseDTO.success("ATS credit consumed successfully")
        );
    }

    // ══════════════════════════════════════════════════════════
    // CONSUME DOWNLOAD CREDIT
    // POST /api/v1/subscriptions/my/consume-download
    // ══════════════════════════════════════════════════════════

    @PostMapping("/my/consume-download")
    public ResponseEntity<ApiResponseDTO<Void>> consumeDownloadCredit(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Consume download credit for userId: {}", userId);
        subscriptionService.consumeDownloadCredit(userId);

        return ResponseEntity.ok(
                ApiResponseDTO.success("Download credit consumed successfully")
        );
    }

    // ══════════════════════════════════════════════════════════
    // REFUND ATS CREDIT
    // POST /api/v1/subscriptions/my/refund-ats
    // Called when AI analysis fails — user not penalized
    // ══════════════════════════════════════════════════════════

    @PostMapping("/my/refund-ats")
    public ResponseEntity<ApiResponseDTO<Void>> refundAtsCredit(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Refund ATS credit for userId: {}", userId);
        subscriptionService.refundAtsCredit(userId);

        return ResponseEntity.ok(
                ApiResponseDTO.success("ATS credit refunded successfully")
        );
    }

    // ══════════════════════════════════════════════════════════
    // REFUND DOWNLOAD CREDIT
    // POST /api/v1/subscriptions/my/refund-download
    // ══════════════════════════════════════════════════════════

    @PostMapping("/my/refund-download")
    public ResponseEntity<ApiResponseDTO<Void>> refundDownloadCredit(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Refund download credit for userId: {}", userId);
        subscriptionService.refundDownloadCredit(userId);

        return ResponseEntity.ok(
                ApiResponseDTO.success("Download credit refunded successfully")
        );
    }

    // ══════════════════════════════════════════════════════════
    // CANCEL SUBSCRIPTION
    // POST /api/v1/subscriptions/my/cancel
    // ══════════════════════════════════════════════════════════

    @PostMapping("/my/cancel")
    public ResponseEntity<ApiResponseDTO<SubscriptionResponseDTO>> cancelSubscription(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Cancel subscription for userId: {}", userId);

        Subscription cancelled = subscriptionService.cancelSubscription(userId);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Subscription cancelled successfully",
                        userMapper.toSubscriptionResponseDTO(cancelled)
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET ALL PLANS
    // GET /api/v1/subscriptions/plans
    // Public — no auth needed, shown on pricing page
    // ══════════════════════════════════════════════════════════

    @GetMapping("/plans")
    public ResponseEntity<ApiResponseDTO<List<PlanResponseDTO>>> getAllPlans() {

        List<PlanResponseDTO> plans = subscriptionService.getAllPlans()
                .stream()
                .map(userMapper::toPlanResponseDTO)
                .toList();

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Plans fetched successfully",
                        plans
                )
        );
    }
}