package launchpath.userservice.co.dto.request;

import jakarta.validation.constraints.NotNull;
import launchpath.userservice.co.enums.PlanName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequestDTO {

    @NotNull(message = "Plan name is required")
    private PlanName planName;

    @NotNull(message = "Duration in months is required")
    private Integer durationMonths;
}