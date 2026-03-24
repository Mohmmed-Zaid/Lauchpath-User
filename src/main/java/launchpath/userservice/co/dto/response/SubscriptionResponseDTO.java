package launchpath.userservice.co.dto.response;

import launchpath.userservice.co.enums.PlanName;
import launchpath.userservice.co.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SubscriptionResponseDTO {

    private Long id;
    private PlanName planName;
    private SubscriptionStatus status;

    private Integer totalAtsCredits;
    private Integer atsCreditsUsed;
    private Integer remainingAtsCredits;

    private Integer totalDownloads;
    private Integer downloadsUsed;
    private Integer remainingDownloads;

    private Integer maxResumes;
    private Integer templatesCount;

    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
}