package launchpath.userservice.co.dto.response;

import launchpath.userservice.co.enums.PlanName;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentOrderResponseDTO {

    private String orderId;
    private Long amount;           // in paise
    private String currency;
    private PlanName planName;
    private Integer durationMonths;
    private String razorpayKeyId;  // frontend needs this to init Razorpay SDK
}