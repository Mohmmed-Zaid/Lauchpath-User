package launchpath.userservice.co.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedeemCouponRequest {
    private String couponCode;
}