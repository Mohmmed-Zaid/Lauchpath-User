package launchpath.userservice.co.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import launchpath.userservice.co.enums.PlanName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpgradePlanRequestDTO {

    @NotNull(message = "Plan name is required")
    private PlanName planName;

    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;

    @NotNull(message = "Duration in months is required")
    private Integer durationMonths;
}