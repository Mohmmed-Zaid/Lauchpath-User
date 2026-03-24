package launchpath.userservice.co.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookRequestDTO {

    private String event;
    private String razorpaySubId;
    private String razorpayPaymentId;
}